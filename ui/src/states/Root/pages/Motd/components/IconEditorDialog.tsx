import {useCallback, useRef, useState} from "react";
import {EraserIcon, PaintBucketIcon, PencilSimpleIcon, TrashIcon} from "@phosphor-icons/react";
import {t} from "i18next";
import {Button} from "@/components/ui/button.tsx";
import {Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle} from "@/components/ui/dialog.tsx";
import {NAMED_COLORS} from "@/lib/MotdUtil.ts";

const GRID = 64;
const BRUSH_SIZES = [1, 2, 3, 5];

type Tool = "pen" | "eraser" | "fill";

const TOOLS: { key: Tool; icon: typeof PencilSimpleIcon; label: string }[] = [
    {key: "pen", icon: PencilSimpleIcon, label: "pen"},
    {key: "eraser", icon: EraserIcon, label: "eraser"},
    {key: "fill", icon: PaintBucketIcon, label: "fill"},
];

const hexToRgb = (hex: string) => {
    const h = hex.replace("#", "");
    return {r: parseInt(h.slice(0, 2), 16), g: parseInt(h.slice(2, 4), 16), b: parseInt(h.slice(4, 6), 16)};
};

interface IconEditorDialogProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    currentIcon: string | null;
    onSave: (png: string) => Promise<void>;
}

const toDataUrl = (icon: string | null) =>
    icon ? (icon.startsWith("data:") ? icon : `data:image/png;base64,${icon}`) : null;

const IconEditorDialog = ({open, onOpenChange, currentIcon, onSave}: IconEditorDialogProps) => {
    const canvasRef = useRef<HTMLCanvasElement>(null);
    const drawing = useRef(false);
    const last = useRef<{ x: number; y: number } | null>(null);
    const [tool, setTool] = useState<Tool>("pen");
    const [color, setColor] = useState("#FF5555");
    const [size, setSize] = useState(1);
    const [saving, setSaving] = useState(false);

    const setCanvas = useCallback((canvas: HTMLCanvasElement | null) => {
        canvasRef.current = canvas;
        if (!canvas) return;
        const ctx = canvas.getContext("2d");
        if (!ctx) return;
        ctx.clearRect(0, 0, GRID, GRID);
        const src = toDataUrl(currentIcon);
        if (!src) return;
        const image = new Image();
        image.onload = () => {
            ctx.imageSmoothingEnabled = false;
            ctx.clearRect(0, 0, GRID, GRID);
            ctx.drawImage(image, 0, 0, GRID, GRID);
        };
        image.src = src;
    }, [currentIcon]);

    const pixelAt = (e: React.PointerEvent) => {
        const canvas = canvasRef.current;
        if (!canvas) return null;
        const rect = canvas.getBoundingClientRect();
        const x = Math.floor(((e.clientX - rect.left) / rect.width) * GRID);
        const y = Math.floor(((e.clientY - rect.top) / rect.height) * GRID);
        if (x < 0 || y < 0 || x >= GRID || y >= GRID) return null;
        return {x, y};
    };

    const paint = (x: number, y: number) => {
        const ctx = canvasRef.current?.getContext("2d");
        if (!ctx) return;

        const half = Math.floor((size - 1) / 2);
        const sx = Math.max(0, x - half);
        const sy = Math.max(0, y - half);
        const ex = Math.min(GRID, x - half + size);
        const ey = Math.min(GRID, y - half + size);
        const w = ex - sx;
        const h = ey - sy;
        if (w <= 0 || h <= 0) return;
        if (tool === "eraser") {
            ctx.clearRect(sx, sy, w, h);
        } else {
            ctx.fillStyle = color;
            ctx.fillRect(sx, sy, w, h);
        }
    };

    const fill = (x: number, y: number) => {
        const ctx = canvasRef.current?.getContext("2d");
        if (!ctx) return;
        const img = ctx.getImageData(0, 0, GRID, GRID);
        const d = img.data;
        const at = (px: number, py: number) => (py * GRID + px) * 4;
        const s = at(x, y);
        const [tr, tg, tb, ta] = [d[s], d[s + 1], d[s + 2], d[s + 3]];
        const {r, g, b} = hexToRgb(color);
        const TOL = 48;
        const matches = (i: number) => {
            const a = d[i + 3];
            if (Math.abs(a - ta) > TOL) return false;
            if (a < TOL && ta < TOL) return true;
            return Math.abs(d[i] - tr) <= TOL && Math.abs(d[i + 1] - tg) <= TOL && Math.abs(d[i + 2] - tb) <= TOL;
        };
        if (matches(s) && tr === r && tg === g && tb === b && ta === 255) return;
        const seen = new Uint8Array(GRID * GRID);
        const stack: [number, number][] = [[x, y]];
        while (stack.length) {
            const [cx, cy] = stack.pop()!;
            if (cx < 0 || cy < 0 || cx >= GRID || cy >= GRID) continue;
            const flat = cy * GRID + cx;
            if (seen[flat]) continue;
            const i = flat * 4;
            if (!matches(i)) continue;
            seen[flat] = 1;
            d[i] = r;
            d[i + 1] = g;
            d[i + 2] = b;
            d[i + 3] = 255;
            stack.push([cx + 1, cy], [cx - 1, cy], [cx, cy + 1], [cx, cy - 1]);
        }
        ctx.putImageData(img, 0, 0);
    };

    const paintLine = (a: { x: number; y: number }, b: { x: number; y: number }) => {
        let {x: x0, y: y0} = a;
        const {x: x1, y: y1} = b;
        const dx = Math.abs(x1 - x0);
        const dy = Math.abs(y1 - y0);
        const sx = x0 < x1 ? 1 : -1;
        const sy = y0 < y1 ? 1 : -1;
        let err = dx - dy;
        for (; ;) {
            paint(x0, y0);
            if (x0 === x1 && y0 === y1) break;
            const e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    };

    const onPointerDown = (e: React.PointerEvent) => {
        const p = pixelAt(e);
        if (!p) return;
        if (tool === "fill") {
            fill(p.x, p.y);
            return;
        }
        drawing.current = true;
        last.current = p;
        canvasRef.current?.setPointerCapture(e.pointerId);
        paint(p.x, p.y);
    };

    const onPointerMove = (e: React.PointerEvent) => {
        if (!drawing.current) return;
        const p = pixelAt(e);
        if (!p) return;
        paintLine(last.current ?? p, p);
        last.current = p;
    };

    const stop = () => {
        drawing.current = false;
        last.current = null;
    };

    const clearAll = () => {
        const ctx = canvasRef.current?.getContext("2d");
        ctx?.clearRect(0, 0, GRID, GRID);
    };

    const handleSave = async () => {
        const canvas = canvasRef.current;
        if (!canvas) return;
        setSaving(true);
        try {
            await onSave(canvas.toDataURL("image/png"));
            onOpenChange(false);
        } finally {
            setSaving(false);
        }
    };

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="sm:max-w-md rounded-xl">
                <DialogHeader>
                    <DialogTitle>{t("motd.icon.editor.title")}</DialogTitle>
                    <DialogDescription>{t("motd.icon.editor.description")}</DialogDescription>
                </DialogHeader>

                <div className="flex flex-col gap-4">
                    <div
                        className="rounded-xl border overflow-hidden touch-none"
                        style={{
                            imageRendering: "pixelated",
                            background: "repeating-conic-gradient(#3b3b44 0% 25%, #2a2a32 0% 50%) 50% / 24px 24px",
                        }}
                    >
                        <canvas
                            ref={setCanvas}
                            width={GRID}
                            height={GRID}
                            className="w-full aspect-square cursor-crosshair touch-none block"
                            style={{imageRendering: "pixelated"}}
                            onPointerDown={onPointerDown}
                            onPointerMove={onPointerMove}
                            onPointerUp={stop}
                            onPointerLeave={stop}
                            onPointerCancel={stop}
                        />
                    </div>

                    <div className="flex items-center gap-1.5">
                        {TOOLS.map(({key, icon: Icon, label}) => (
                            <Button
                                key={key}
                                variant={tool === key ? "default" : "outline"}
                                size="icon"
                                title={t(`motd.icon.editor.${label}`)}
                                className="h-9 w-9 rounded-xl"
                                onClick={() => setTool(key)}
                            >
                                <Icon className="h-4 w-4"/>
                            </Button>
                        ))}

                        <span className="mx-1 h-6 w-px bg-border"/>

                        {BRUSH_SIZES.map((s) => (
                            <Button
                                key={s}
                                variant={size === s ? "default" : "outline"}
                                size="icon"
                                title={`${s}px`}
                                disabled={tool === "fill"}
                                className="h-9 w-9 rounded-xl"
                                onClick={() => setSize(s)}
                            >
                                <span className="rounded-sm bg-current" style={{width: `${2 + s * 2}px`, height: `${2 + s * 2}px`}}/>
                            </Button>
                        ))}

                        <Button variant="ghost" size="icon" title={t("motd.icon.editor.clear")} className="h-9 w-9 rounded-xl ml-auto" onClick={clearAll}>
                            <TrashIcon className="h-4 w-4"/>
                        </Button>
                    </div>

                    <div className="flex flex-wrap items-center gap-1.5">
                        {NAMED_COLORS.map((c) => {
                            const active = tool !== "eraser" && color.toUpperCase() === c.hex.toUpperCase();
                            return (
                                <button
                                    key={c.name}
                                    title={c.name.replace(/_/g, " ")}
                                    onClick={() => {
                                        setColor(c.hex);
                                        if (tool === "eraser") setTool("pen");
                                    }}
                                    className={`h-7 w-7 rounded-md border transition-transform hover:scale-110 ${
                                        active ? "ring-2 ring-primary ring-offset-2 ring-offset-background scale-110" : "border-black/20"
                                    }`}
                                    style={{backgroundColor: c.hex}}
                                />
                            );
                        })}
                        <label
                            className="h-7 px-2 rounded-md border text-xs flex items-center gap-1.5 cursor-pointer hover:bg-accent"
                            title={t("motd.icon.editor.custom")}
                        >
                            <span className="h-4 w-4 rounded-sm border border-black/20" style={{backgroundColor: color}}/>
                            {t("motd.icon.editor.custom")}
                            <input
                                type="color"
                                className="sr-only"
                                value={color}
                                onChange={(e) => {
                                    setColor(e.target.value);
                                    if (tool === "eraser") setTool("pen");
                                }}
                            />
                        </label>
                    </div>
                </div>

                <DialogFooter>
                    <Button variant="outline" className="rounded-xl" onClick={() => onOpenChange(false)} disabled={saving}>
                        {t("action.cancel")}
                    </Button>
                    <Button className="rounded-xl" onClick={handleSave} disabled={saving}>
                        {t("action.save")}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default IconEditorDialog;
