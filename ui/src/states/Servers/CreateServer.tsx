import {useEffect, useState} from "react";
import {useNavigate} from "react-router-dom";
import {t} from "i18next";
import {useServerSelection} from "@/contexts/ServerSelectionContext.tsx";
import {masterJson} from "@/lib/RequestUtil.ts";
import {softwareMeta, statusMeta} from "@/lib/servers.ts";
import {Button} from "@/components/ui/button.tsx";
import {Input} from "@/components/ui/input.tsx";
import {Label} from "@/components/ui/label.tsx";
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from "@/components/ui/select.tsx";
import {Slider} from "@/components/ui/slider.tsx";
import {Dialog, DialogContent, DialogHeader, DialogTitle} from "@/components/ui/dialog.tsx";
import {toast} from "@/hooks/use-toast.ts";
import {
    ArrowLeftIcon, ArrowRightIcon, CheckIcon, CheckCircleIcon, SpinnerGapIcon,
    WarningCircleIcon, ArrowSquareOutIcon, PuzzlePieceIcon, StackIcon, CubeIcon, ShareNetworkIcon,
    type Icon,
} from "@phosphor-icons/react";

interface SoftwareEntry {
    key: string;
    name: string;
    kind: string;
    category: string;
    accent: string;
    tagline: string;
}

const CATEGORIES: { key: string; icon: Icon; accent: string }[] = [
    {key: "plugins", icon: PuzzlePieceIcon, accent: "#4f8ff7"},
    {key: "modded", icon: StackIcon, accent: "#e8732a"},
    {key: "vanilla", icon: CubeIcon, accent: "#5cb85c"},
    {key: "proxy", icon: ShareNetworkIcon, accent: "#e0a82e"},
];

const MEMORY_MIN = 1024;
const MEMORY_STEP = 512;
const MEMORY_FALLBACK_MAX = 16384;
const formatGb = (mb: number) => `${Number.isInteger(mb / 1024) ? mb / 1024 : (mb / 1024).toFixed(1)} GB`;

const snapToStep = (mb: number) => Math.max(MEMORY_MIN, Math.round(mb / MEMORY_STEP) * MEMORY_STEP);

const memoryMarks = (max: number) => [
    {mb: snapToStep(MEMORY_MIN + (max - MEMORY_MIN) * 0.25), label: t("create_server.memory_low")},
    {mb: snapToStep(MEMORY_MIN + (max - MEMORY_MIN) * 0.5), label: t("create_server.memory_medium")},
    {mb: snapToStep(MEMORY_MIN + (max - MEMORY_MIN) * 0.75), label: t("create_server.memory_high")},
];

const CreateServerDialog = ({open, onOpenChange}: { open: boolean; onOpenChange: (open: boolean) => void }) => {
    const {createServer, selectServer} = useServerSelection();
    const navigate = useNavigate();

    const [step, setStep] = useState<"software" | "details" | "provisioning">("software");
    const [catalog, setCatalog] = useState<SoftwareEntry[]>([]);
    const [category, setCategory] = useState<string | null>(null);
    const [software, setSoftware] = useState<string | null>(null);
    const [versions, setVersions] = useState<string[]>([]);
    const [versionsLoading, setVersionsLoading] = useState(false);
    const [name, setName] = useState("");
    const [mcVersion, setMcVersion] = useState("");
    const [memoryMb, setMemoryMb] = useState(2048);
    const [memoryMax, setMemoryMax] = useState(MEMORY_FALLBACK_MAX);
    const [creating, setCreating] = useState(false);
    const [serverId, setServerId] = useState<string | null>(null);

    useEffect(() => {
        if (!open || catalog.length) return;
        masterJson("software").then((d) => setCatalog(d.software || [])).catch(() => {});
        masterJson("system").then((d) => {
            if (d.totalMemoryMb) setMemoryMax(Math.max(MEMORY_MIN + MEMORY_STEP * 2, snapToStep(d.totalMemoryMb)));
        }).catch(() => {});
    }, [open]);

    useEffect(() => {
        if (open) return;
        const t = setTimeout(() => {
            setStep("software");
            setCategory(null);
            setSoftware(null);
            setVersions([]);
            setName("");
            setMcVersion("");
            setMemoryMb(2048);
            setServerId(null);
        }, 200);
        return () => clearTimeout(t);
    }, [open]);

    const chooseSoftware = async (key: string) => {
        setSoftware(key);
        setStep("details");
        setVersionsLoading(true);
        setVersions([]);
        try {
            const data = await masterJson(`software/${key}/versions`);
            const stable = (data.versions || []).filter((v: string) => !v.includes("-"));
            setVersions(stable);
            setMcVersion(stable[0] || "");
        } catch {
            toast({description: t("create_server.load_versions_failed"), variant: "destructive"});
        } finally {
            setVersionsLoading(false);
        }
    };

    const submit = async () => {
        if (!software || !name || !mcVersion) return;
        setCreating(true);
        try {
            const server = await createServer({name, software, mcVersion, memoryMb});
            setServerId(server.id);
            setStep("provisioning");
        } catch (err) {
            toast({description: (err as Error).message, variant: "destructive"});
        } finally {
            setCreating(false);
        }
    };

    const handleOpenChange = (next: boolean) => {
        if (!next && creating) return;
        onOpenChange(next);
    };

    return (
        <Dialog open={open} onOpenChange={handleOpenChange}>
            <DialogContent className="w-full max-w-xl">
                <DialogHeader>
                    <DialogTitle className="font-display text-xl">{t("create_server.title")}</DialogTitle>
                </DialogHeader>

                <Steps step={step}/>

                {step === "software" && category === null && (
                    <div className="grid gap-3 sm:grid-cols-2">
                        {CATEGORIES.filter((cat) => catalog.some((e) => e.category === cat.key)).map((cat) => {
                            const Icon = cat.icon;
                            const loaders = catalog.filter((e) => e.category === cat.key);
                            const pick = () => loaders.length === 1 ? chooseSoftware(loaders[0].key) : setCategory(cat.key);
                            return (
                                <button key={cat.key} onClick={pick}
                                        className="group flex min-h-[10.5rem] cursor-pointer flex-col gap-3 rounded-2xl border border-border/70 bg-card/40 p-5 text-left transition-all hover:border-border hover:bg-card/80">
                                    <div className="flex items-center gap-3">
                                        <div className="flex size-10 shrink-0 items-center justify-center rounded-xl transition-transform group-hover:scale-105"
                                             style={{backgroundColor: `${cat.accent}1a`, color: cat.accent}}>
                                            <Icon weight="duotone" className="size-[22px]"/>
                                        </div>
                                        <h3 className="font-display text-base font-semibold">{t(`create_server.category_${cat.key}`)}</h3>
                                    </div>
                                    <p className="text-sm leading-relaxed text-muted-foreground">{t(`create_server.category_${cat.key}_desc`)}</p>
                                    <div className="mt-auto flex items-center gap-2 pt-1">
                                        <div className="flex -space-x-1.5">
                                            {loaders.slice(0, 5).map((e) => (
                                                <div key={e.key} title={e.name}
                                                     className="flex size-7 items-center justify-center rounded-lg border border-border/60 bg-background ring-2 ring-card/40">
                                                    {softwareMeta(e.key).logo ? (
                                                        <img src={softwareMeta(e.key).logo} alt={e.name} className="size-4 object-contain"
                                                             style={{imageRendering: e.key === "fabric" ? "pixelated" : "auto"}}/>
                                                    ) : (
                                                        <span className="text-[9px] font-bold">{e.name.slice(0, 2).toUpperCase()}</span>
                                                    )}
                                                </div>
                                            ))}
                                        </div>
                                        <span className="text-xs text-muted-foreground">
                                            {t("create_server.option_count", {count: loaders.length})}
                                        </span>
                                    </div>
                                </button>
                            );
                        })}
                    </div>
                )}

                {step === "software" && category !== null && (
                    <div className="space-y-3">
                        <button onClick={() => setCategory(null)}
                                className="flex cursor-pointer items-center gap-1.5 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground">
                            <ArrowLeftIcon className="size-4"/> {t(`create_server.category_${category}`)}
                        </button>
                        <div className="grid gap-3 sm:grid-cols-2">
                            {catalog.filter((e) => e.category === category).map((entry) => (
                                <button key={entry.key} onClick={() => chooseSoftware(entry.key)}
                                        className="group relative flex cursor-pointer items-start gap-4 overflow-hidden rounded-2xl border border-border/70 bg-card/40 p-4 text-left transition-all hover:border-border hover:bg-card/80">
                                    {softwareMeta(entry.key).logo ? (
                                        <div className="flex size-11 shrink-0 items-center justify-center rounded-xl border border-border/60 bg-background">
                                            <img src={softwareMeta(entry.key).logo} alt={entry.name} className="size-7 object-contain"
                                                 style={{imageRendering: entry.key === "fabric" ? "pixelated" : "auto"}}/>
                                        </div>
                                    ) : (
                                        <div className="flex size-11 shrink-0 items-center justify-center rounded-xl text-base font-bold text-white"
                                             style={{backgroundColor: entry.accent}}>
                                            {entry.name.slice(0, 2).toUpperCase()}
                                        </div>
                                    )}
                                    <div className="min-w-0 flex-1">
                                        <h3 className="font-display text-base font-semibold">{entry.name}</h3>
                                        <p className="text-xs text-muted-foreground">{entry.tagline}</p>
                                    </div>
                                </button>
                            ))}
                        </div>
                    </div>
                )}

                {step === "details" && (
                    <div className="space-y-5">
                        <div className="space-y-2">
                            <Label htmlFor="name">{t("create_server.server_name")}</Label>
                            <Input id="name" autoFocus value={name} placeholder={t("create_server.server_name_placeholder")}
                                   onChange={(e) => setName(e.target.value)}/>
                        </div>

                        <div className="space-y-2">
                            <Label>{t("create_server.minecraft_version")}</Label>
                            <Select value={mcVersion} onValueChange={setMcVersion} disabled={versionsLoading}>
                                <SelectTrigger>
                                    <SelectValue placeholder={versionsLoading ? t("create_server.loading_versions") : t("create_server.choose_version")}/>
                                </SelectTrigger>
                                <SelectContent className="max-h-72">
                                    {versions.map((v) => <SelectItem key={v} value={v}>{v}</SelectItem>)}
                                </SelectContent>
                            </Select>
                        </div>

                        <div className="space-y-2">
                            <div className="flex items-center justify-between">
                                <Label>{t("create_server.memory")}</Label>
                                <span className="rounded-md bg-primary/10 px-2 py-0.5 font-mono text-sm font-semibold text-primary">
                                    {formatGb(memoryMb)}
                                </span>
                            </div>
                            <Slider value={[memoryMb]} min={MEMORY_MIN} max={memoryMax} step={MEMORY_STEP}
                                    onValueChange={([v]) => setMemoryMb(v)} aria-label={t("create_server.memory")}/>
                            <div className="relative h-7">
                                {memoryMarks(memoryMax).map((mark) => {
                                    const pct = ((mark.mb - MEMORY_MIN) / (memoryMax - MEMORY_MIN)) * 100;
                                    const active = Math.abs(memoryMb - mark.mb) < MEMORY_STEP;
                                    return (
                                        <div key={mark.mb} style={{left: `${pct}%`}}
                                             className="absolute flex -translate-x-1/2 flex-col items-center gap-1">
                                            <span className={`h-1.5 w-px ${active ? "bg-primary" : "bg-border"}`}/>
                                            <span className={`whitespace-nowrap text-[10px] leading-none transition-colors
                                                ${active ? "font-medium text-primary" : "text-muted-foreground"}`}>
                                                {mark.label} · {formatGb(mark.mb)}
                                            </span>
                                        </div>
                                    );
                                })}
                            </div>
                        </div>

                        <div className="flex justify-between pt-1">
                            <Button variant="ghost" onClick={() => setStep("software")}>
                                <ArrowLeftIcon className="mr-1.5 size-4"/> {t("create_server.back")}
                            </Button>
                            <Button disabled={!name || !mcVersion || creating} onClick={submit}>
                                {creating ? <SpinnerGapIcon className="size-4 animate-spin"/> : (
                                    <>{t("create_server.create_and_start")} <ArrowRightIcon className="ml-1.5 size-4"/></>
                                )}
                            </Button>
                        </div>
                    </div>
                )}

                {step === "provisioning" && serverId && (
                    <Provisioning serverId={serverId} onOpen={() => {
                        selectServer(serverId);
                        navigate("/");
                    }} onClose={() => onOpenChange(false)}/>
                )}
            </DialogContent>
        </Dialog>
    );
};

const Steps = ({step}: { step: string }) => {
    const order = ["software", "details", "provisioning"];
    const labels = {software: t("create_server.step_software"), details: t("create_server.step_details"), provisioning: t("create_server.step_provision")};
    const currentIndex = order.indexOf(step);
    return (
        <div className="flex items-center gap-3">
            {order.map((s, i) => (
                <div key={s} className="flex items-center gap-3">
                    <div className={`flex items-center gap-2 text-sm font-medium ${i <= currentIndex ? "text-foreground" : "text-muted-foreground"}`}>
                        <span className={`flex size-6 items-center justify-center rounded-full text-xs font-bold
                            ${i < currentIndex ? "bg-primary text-primary-foreground" : i === currentIndex ? "border-2 border-primary text-primary" : "border border-border"}`}>
                            {i < currentIndex ? <CheckIcon weight="bold" className="size-3.5"/> : i + 1}
                        </span>
                        <span className="hidden sm:inline">{labels[s as keyof typeof labels]}</span>
                    </div>
                    {i < order.length - 1 && <div className="h-px w-8 bg-border"/>}
                </div>
            ))}
        </div>
    );
};

const Provisioning = ({serverId, onOpen, onClose}: { serverId: string; onOpen: () => void; onClose: () => void }) => {
    const [log, setLog] = useState<string[]>([]);
    const [status, setStatus] = useState<string>("installing");

    useEffect(() => {
        let active = true;
        const poll = async () => {
            try {
                const data = await masterJson(`servers/${serverId}`);
                if (!active) return;
                setLog(data.log || []);
                setStatus(data.server?.status || "installing");
            } catch {}
        };
        poll();
        const interval = setInterval(poll, 1500);
        return () => {
            active = false;
            clearInterval(interval);
        };
    }, [serverId]);

    const meta = statusMeta(status);
    const failed = status === "install_failed";
    const online = status === "online";

    return (
        <div className="space-y-4">
            <div className="flex items-center gap-3">
                {failed ? <WarningCircleIcon weight="fill" className="size-6 text-destructive"/>
                    : online ? <CheckCircleIcon weight="fill" className="size-6 text-emerald-500"/>
                        : <SpinnerGapIcon className="size-6 animate-spin text-primary"/>}
                <div>
                    <h3 className="font-display text-lg font-semibold">
                        {failed ? t("create_server.provision_failed") : online ? t("create_server.server_online") : t("create_server.setting_up")}
                    </h3>
                    <p className={`text-sm ${meta.text}`}>{meta.label}</p>
                </div>
            </div>

            <div className="h-64 overflow-auto rounded-lg bg-zinc-950 p-3 font-mono text-xs leading-relaxed text-zinc-300">
                {log.length === 0 ? <span className="text-zinc-500">{t("create_server.starting")}</span> :
                    log.map((line, i) => <div key={i} className="whitespace-pre-wrap break-all">{line}</div>)}
            </div>

            <div className="flex justify-between">
                <Button variant="ghost" onClick={onClose}>{t("create_server.close")}</Button>
                <Button disabled={!online} onClick={onOpen}>
                    <ArrowSquareOutIcon className="mr-1.5 size-4"/> {t("create_server.open_dashboard")}
                </Button>
            </div>
        </div>
    );
};

export default CreateServerDialog;
