import {useEffect, useRef, useState} from "react";
import {EditorContent, useEditor} from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import {Color, TextStyle} from "@tiptap/extension-text-style";
import {
    ChatCircleTextIcon,
    EraserIcon,
    FloppyDiskIcon,
    ImageIcon,
    SparkleIcon,
    TextBolderIcon,
    TextItalicIcon,
    TextStrikethroughIcon,
    TextUnderlineIcon,
    TrashIcon,
    WarningCircleIcon,
} from "@phosphor-icons/react";
import {t} from "i18next";
import {deleteRequest, jsonRequest, postRequest, putRequest} from "@/lib/RequestUtil.ts";
import {toast} from "@/hooks/use-toast.ts";
import {Button} from "@/components/ui/button.tsx";
import {Alert, AlertDescription, AlertTitle} from "@/components/ui/alert.tsx";
import {ScrollArea} from "@/components/ui/scroll-area.tsx";
import {MotdCapabilities, StyleKey} from "@/types/motd";
import {countDocLines, docToMotd, MAX_MOTD_LINES, motdToDoc, NAMED_COLORS, normalizeColor} from "@/lib/MotdUtil.ts";
import {Obfuscated} from "@/states/Root/pages/Motd/components/ObfuscatedMark.ts";

const EDITOR_STYLES = `
.motd-surface .ProseMirror {
    outline: none;
    min-height: 2.6em;
    line-height: 1.55;
    font-family: ui-monospace, "Cascadia Code", "Segoe UI Mono", monospace;
    color: #ffffff;
    text-shadow: 2px 2px 0 rgba(0,0,0,0.55);
    white-space: pre-wrap;
    word-break: break-word;
    caret-color: #ffffff;
}
.motd-surface .ProseMirror p { margin: 0; }
.motd-surface .ProseMirror-focused { outline: none; }
.motd-obfuscated { position: relative; }
.motd-obf-real { color: transparent; }
.motd-obf-scramble {
    position: absolute;
    left: 0;
    top: 0;
    pointer-events: none;
    user-select: none;
    color: inherit;
    white-space: pre;
}
`;

const Motd = () => {
    const [icon, setIcon] = useState<string | null>(null);
    const [capabilities, setCapabilities] = useState<MotdCapabilities | null>(null);
    const [saving, setSaving] = useState(false);
    const [, setTick] = useState(0);

    const loaded = useRef(false);

    const editor = useEditor({
        extensions: [
            StarterKit.configure({heading: false, codeBlock: false, horizontalRule: false}),
            TextStyle,
            Color,
            Obfuscated,
        ],
        content: "",
        editorProps: {
            attributes: {class: "min-w-0 flex-1"},
            handleKeyDown: (_view, event) => {
                if (event.key === "Enter") {
                    event.preventDefault();
                    if (countDocLines(editorRef.current?.getJSON()) < MAX_MOTD_LINES) {
                        editorRef.current?.chain().focus().setHardBreak().run();
                    }
                    return true;
                }
                return false;
            },
            handlePaste: (_view, event) => {
                const text = event.clipboardData?.getData("text/plain");
                if (text) {
                    editorRef.current?.chain().focus().insertContent(text.replace(/\r?\n/g, " ")).run();
                    return true;
                }
                return false;
            },
        },
        onSelectionUpdate: () => setTick((value) => value + 1),
        onTransaction: () => setTick((value) => value + 1),
    });

    const editorRef = useRef<typeof editor>(null);
    (editorRef as { current: typeof editor }).current = editor;

    useEffect(() => {
        if (!editor || loaded.current) return;
        loaded.current = true;
        jsonRequest("motd").then((data) => {
            if (!data) return;
            editor.commands.setContent(motdToDoc(data.motd));
            setIcon(data.icon ?? null);
            setCapabilities(data.capabilities ?? null);
        });
    }, [editor]);

    const activeColor = normalizeColor(editor?.getAttributes("textStyle").color);

    const applyColor = (hex: string | null) => {
        if (!editor) return;
        const chain = editor.chain().focus();
        if (hex) chain.setColor(hex).run();
        else chain.unsetColor().run();
    };

    const toggleStyle = (key: StyleKey) => {
        if (!editor) return;
        const map: Record<StyleKey, string> = {
            bold: "bold",
            italic: "italic",
            underlined: "underline",
            strikethrough: "strike",
            obfuscated: "obfuscated",
        };
        editor.chain().focus().toggleMark(map[key]).run();
    };

    const isActive = (markName: string) => editor?.isActive(markName) ?? false;

    const clearFormatting = () => editor?.chain().focus().unsetAllMarks().run();

    const onPickIcon = async (file: File) => {
        const dataUrl = await new Promise<string>((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () => resolve(reader.result as string);
            reader.onerror = reject;
            reader.readAsDataURL(file);
        });
        const png = await new Promise<string>((resolve, reject) => {
            const image = new Image();
            image.onload = () => {
                const canvas = document.createElement("canvas");
                canvas.width = 64;
                canvas.height = 64;
                const ctx = canvas.getContext("2d");
                if (!ctx) return reject(new Error("no canvas context"));
                ctx.imageSmoothingEnabled = false;
                ctx.drawImage(image, 0, 0, 64, 64);
                resolve(canvas.toDataURL("image/png"));
            };
            image.onerror = reject;
            image.src = dataUrl;
        });
        const result = await postRequest("motd/icon", {image: png});
        if (result?.error) {
            toast({title: t("motd.icon.error_title"), description: result.error, variant: "destructive"});
            return;
        }
        setIcon(png);
        toast({title: t("motd.icon.updated_title"), description: t("motd.icon.updated_description")});
    };

    const removeIcon = async () => {
        await deleteRequest("motd/icon");
        setIcon(null);
        toast({title: t("motd.icon.removed_title"), description: t("motd.icon.removed_description")});
    };

    const save = async () => {
        if (!editor) return;
        setSaving(true);
        try {
            const result = await putRequest("motd", {motd: docToMotd(editor.getJSON())});
            if (result?.error) {
                toast({title: t("motd.save_error_title"), description: result.error, variant: "destructive"});
                return;
            }
            toast({title: t("motd.saved_title"), description: t("motd.saved_description")});
        } finally {
            setSaving(false);
        }
    };

    if (!capabilities) return null;

    const styleButtons: { key: StyleKey; mark: string; icon: typeof TextBolderIcon; label: string }[] = [
        {key: "bold", mark: "bold", icon: TextBolderIcon, label: t("motd.style.bold")},
        {key: "italic", mark: "italic", icon: TextItalicIcon, label: t("motd.style.italic")},
        {key: "underlined", mark: "underline", icon: TextUnderlineIcon, label: t("motd.style.underline")},
        {key: "strikethrough", mark: "strike", icon: TextStrikethroughIcon, label: t("motd.style.strikethrough")},
        {key: "obfuscated", mark: "obfuscated", icon: SparkleIcon, label: t("motd.style.obfuscated")},
    ];

    const iconSrc = icon ? (icon.startsWith("data:") ? icon : `data:image/png;base64,${icon}`) : null;

    return (
        <>
            <style>{EDITOR_STYLES}</style>
            <div className="flex flex-col p-4 md:p-6 pt-0 gap-6" style={{height: "calc(var(--app-vh) - 5.5rem)"}}>
                <div className="flex flex-wrap items-center justify-between gap-3 p-4 rounded-xl border bg-card shrink-0">
                    <div className="flex items-center gap-4">
                        <div className="h-12 w-12 rounded-xl bg-primary/10 flex items-center justify-center">
                            <ChatCircleTextIcon className="h-6 w-6 text-primary" weight="fill"/>
                        </div>
                        <div>
                            <h1 className="text-lg font-semibold">{t("motd.title")}</h1>
                            <p className="text-sm text-muted-foreground">{t("motd.subtitle")}</p>
                        </div>
                    </div>
                    <Button className="gap-2 rounded-xl" onClick={save} disabled={saving}>
                        <FloppyDiskIcon className="h-4 w-4"/>
                        {t("motd.save")}
                    </Button>
                </div>

                <div className="flex-1 min-h-0">
                    <ScrollArea className="h-full">
                        <div className="flex flex-col gap-6">
                            {capabilities.requiresRestart && (
                                <Alert className="rounded-xl">
                                    <WarningCircleIcon className="h-5 w-5"/>
                                    <AlertTitle
                                        className="text-base font-semibold">{t("motd.restart_title")}</AlertTitle>
                                    <AlertDescription
                                        className="text-sm">{t("motd.restart_description")}</AlertDescription>
                                </Alert>
                            )}

                            <div className="rounded-xl border bg-card overflow-hidden">
                                <div className="p-3 border-b space-y-2.5">
                                    <div className="flex flex-wrap items-center gap-1.5">
                                        {NAMED_COLORS.map((color) => {
                                            const active = activeColor === color.hex.toUpperCase();
                                            return (
                                                <button
                                                    key={color.name}
                                                    title={color.name.replace(/_/g, " ")}
                                                    onMouseDown={(e) => e.preventDefault()}
                                                    onClick={() => applyColor(color.hex)}
                                                    className={`h-7 w-7 rounded-md border transition-transform hover:scale-110 ${
                                                        active ? "ring-2 ring-primary ring-offset-2 ring-offset-card scale-110" : "border-black/20"
                                                    }`}
                                                    style={{backgroundColor: color.hex}}
                                                />
                                            );
                                        })}
                                        <button
                                            title={t("motd.default_color")}
                                            onMouseDown={(e) => e.preventDefault()}
                                            onClick={() => applyColor(null)}
                                            className={`h-7 px-2 rounded-md border text-xs hover:bg-accent ${
                                                !activeColor ? "ring-2 ring-primary" : ""
                                            }`}
                                        >
                                            {t("motd.default_color")}
                                        </button>
                                        {capabilities.hex && (
                                            <label
                                                className="h-7 px-2 rounded-md border text-xs flex items-center gap-1.5 cursor-pointer hover:bg-accent"
                                                onMouseDown={(e) => e.preventDefault()}
                                                title={t("motd.hex")}
                                            >
                                        <span
                                            className="h-4 w-4 rounded-sm border border-black/20"
                                            style={{backgroundColor: activeColor ?? "#ffffff"}}
                                        />
                                                {t("motd.hex")}
                                                <input
                                                    type="color"
                                                    className="sr-only"
                                                    value={activeColor ?? "#ffffff"}
                                                    onChange={(e) => applyColor(e.target.value.toUpperCase())}
                                                />
                                            </label>
                                        )}
                                        <span className="mx-1 h-6 w-px bg-border"/>
                                        {styleButtons.map(({key, mark, icon: Icon, label}) => (
                                            <Button
                                                key={key}
                                                variant={isActive(mark) ? "default" : "outline"}
                                                size="icon"
                                                title={label}
                                                className="h-7 w-7 rounded-md"
                                                onMouseDown={(e) => e.preventDefault()}
                                                onClick={() => toggleStyle(key)}
                                            >
                                                <Icon className="h-4 w-4"/>
                                            </Button>
                                        ))}
                                        <Button
                                            variant="ghost"
                                            size="icon"
                                            title={t("motd.clear")}
                                            className="h-7 w-7 rounded-md ml-auto"
                                            onMouseDown={(e) => e.preventDefault()}
                                            onClick={clearFormatting}
                                        >
                                            <EraserIcon className="h-4 w-4"/>
                                        </Button>
                                    </div>
                                </div>

                                <div
                                    className="motd-surface p-4"
                                    style={{background: "linear-gradient(180deg, #2a2a32 0%, #232329 100%)"}}
                                >
                                    <div className="flex gap-3">
                                        <div
                                            className="h-16 w-16 shrink-0 rounded-md overflow-hidden flex items-center justify-center select-none pointer-events-none"
                                            style={{
                                                imageRendering: "pixelated",
                                                background: "repeating-conic-gradient(#3b3b44 0% 25%, #34343c 0% 50%) 50% / 16px 16px",
                                            }}
                                        >
                                            {iconSrc ? (
                                                <img src={iconSrc} alt={t("motd.icon.title")} className="h-16 w-16"
                                                     style={{imageRendering: "pixelated"}}/>
                                            ) : (
                                                <ImageIcon className="h-6 w-6 text-white/40"/>
                                            )}
                                        </div>
                                        <div className="min-w-0 flex flex-col justify-center flex-1">
                                            <div
                                                className="text-white/90 font-semibold mb-1 select-none pointer-events-none"
                                                style={{
                                                    fontFamily: "ui-monospace, monospace",
                                                    textShadow: "2px 2px 0 rgba(0,0,0,0.55)"
                                                }}
                                            >
                                                A Minecraft Server
                                            </div>
                                            <EditorContent editor={editor}/>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <p className="text-xs text-muted-foreground">
                                {t("motd.editor_hint")} {t("motd.selection_hint")}
                                {!capabilities.hex && " " + t("motd.no_hex_hint")}
                            </p>

                            {capabilities.favicon && (
                                <div className="bg-card border rounded-xl p-4 flex flex-wrap items-center gap-4">
                                    <div
                                        className="h-16 w-16 rounded-md overflow-hidden flex items-center justify-center bg-muted shrink-0"
                                        style={{imageRendering: "pixelated"}}
                                    >
                                        {iconSrc ? (
                                            <img src={iconSrc} alt={t("motd.icon.title")} className="h-16 w-16"
                                                 style={{imageRendering: "pixelated"}}/>
                                        ) : (
                                            <ImageIcon className="h-7 w-7 text-muted-foreground"/>
                                        )}
                                    </div>
                                    <div className="flex-1 min-w-0">
                                        <p className="font-medium">{t("motd.icon.title")}</p>
                                        <p className="text-sm text-muted-foreground">{t("motd.icon.description")}</p>
                                    </div>
                                    <label className="cursor-pointer">
                                        <input
                                            type="file"
                                            accept="image/png,image/jpeg,image/webp"
                                            className="hidden"
                                            onChange={(e) => {
                                                const file = e.target.files?.[0];
                                                if (file) onPickIcon(file).catch(() =>
                                                    toast({
                                                        title: t("motd.icon.error_title"),
                                                        description: t("motd.icon.error_description"),
                                                        variant: "destructive",
                                                    })
                                                );
                                                e.target.value = "";
                                            }}
                                        />
                                        <span
                                            className="inline-flex items-center gap-2 h-9 px-4 rounded-xl border text-sm hover:bg-accent">
                                    <ImageIcon className="h-4 w-4"/>
                                            {t("motd.icon.upload")}
                                </span>
                                    </label>
                                    {icon && (
                                        <Button variant="ghost" size="icon" className="rounded-xl" onClick={removeIcon}>
                                            <TrashIcon className="h-4 w-4"/>
                                        </Button>
                                    )}
                                </div>
                            )}
                        </div>
                    </ScrollArea>
                </div>
            </div>
        </>
    );
};

export default Motd;
