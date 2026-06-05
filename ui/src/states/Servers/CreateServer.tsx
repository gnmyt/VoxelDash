import {useEffect, useState} from "react";
import {useNavigate} from "react-router-dom";
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
    WarningCircleIcon, ArrowSquareOutIcon,
} from "@phosphor-icons/react";

interface SoftwareEntry {
    key: string;
    name: string;
    kind: string;
    accent: string;
    tagline: string;
}

const MEMORY_MIN = 1024;
const MEMORY_MAX = 16384;
const MEMORY_STEP = 512;
const MEMORY_MARKS = [
    {mb: 2048, label: "Low"},
    {mb: 4096, label: "Medium"},
    {mb: 8192, label: "High"},
];
const formatGb = (mb: number) => `${Number.isInteger(mb / 1024) ? mb / 1024 : (mb / 1024).toFixed(1)} GB`;

const CreateServerDialog = ({open, onOpenChange}: { open: boolean; onOpenChange: (open: boolean) => void }) => {
    const {createServer, selectServer} = useServerSelection();
    const navigate = useNavigate();

    const [step, setStep] = useState<"software" | "details" | "provisioning">("software");
    const [catalog, setCatalog] = useState<SoftwareEntry[]>([]);
    const [software, setSoftware] = useState<string | null>(null);
    const [versions, setVersions] = useState<string[]>([]);
    const [versionsLoading, setVersionsLoading] = useState(false);
    const [name, setName] = useState("");
    const [mcVersion, setMcVersion] = useState("");
    const [memoryMb, setMemoryMb] = useState(2048);
    const [creating, setCreating] = useState(false);
    const [serverId, setServerId] = useState<string | null>(null);

    useEffect(() => {
        if (!open || catalog.length) return;
        masterJson("software").then((d) => setCatalog(d.software || [])).catch(() => {});
    }, [open]);

    useEffect(() => {
        if (open) return;
        const t = setTimeout(() => {
            setStep("software");
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
            toast({description: "Could not load versions", variant: "destructive"});
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
            <DialogContent className="max-w-xl">
                <DialogHeader>
                    <DialogTitle className="font-display text-xl">New server</DialogTitle>
                </DialogHeader>

                <Steps step={step}/>

                {step === "software" && (
                    <div className="grid gap-3 sm:grid-cols-2">
                        {catalog.map((entry) => (
                            <button key={entry.key} onClick={() => chooseSoftware(entry.key)}
                                    className={`group relative flex items-start gap-4 overflow-hidden rounded-2xl border p-4 text-left transition-all
                                        "border-border/70 bg-card/40 hover:border-border hover:bg-card/80 cursor-pointer"}`}>
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
                                    <div className="flex items-center gap-2">
                                        <h3 className="font-display text-base font-semibold">{entry.name}</h3>
                                    </div>
                                    <p className="text-xs text-muted-foreground">{entry.tagline}</p>
                                </div>
                            </button>
                        ))}
                    </div>
                )}

                {step === "details" && (
                    <div className="space-y-5">
                        <div className="space-y-2">
                            <Label htmlFor="name">Server name</Label>
                            <Input id="name" autoFocus value={name} placeholder="Survival, Lobby, Creative…"
                                   onChange={(e) => setName(e.target.value)}/>
                        </div>

                        <div className="space-y-2">
                            <Label>Minecraft version</Label>
                            <Select value={mcVersion} onValueChange={setMcVersion} disabled={versionsLoading}>
                                <SelectTrigger>
                                    <SelectValue placeholder={versionsLoading ? "Loading versions…" : "Choose a version"}/>
                                </SelectTrigger>
                                <SelectContent className="max-h-72">
                                    {versions.map((v) => <SelectItem key={v} value={v}>{v}</SelectItem>)}
                                </SelectContent>
                            </Select>
                        </div>

                        <div className="space-y-2">
                            <div className="flex items-center justify-between">
                                <Label>Memory</Label>
                                <span className="rounded-md bg-primary/10 px-2 py-0.5 font-mono text-sm font-semibold text-primary">
                                    {formatGb(memoryMb)}
                                </span>
                            </div>
                            <Slider value={[memoryMb]} min={MEMORY_MIN} max={MEMORY_MAX} step={MEMORY_STEP}
                                    onValueChange={([v]) => setMemoryMb(v)} aria-label="Memory"/>
                            <div className="relative h-7">
                                {MEMORY_MARKS.map((mark) => {
                                    const pct = ((mark.mb - MEMORY_MIN) / (MEMORY_MAX - MEMORY_MIN)) * 100;
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
                                <ArrowLeftIcon className="mr-1.5 size-4"/> Back
                            </Button>
                            <Button disabled={!name || !mcVersion || creating} onClick={submit}>
                                {creating ? <SpinnerGapIcon className="size-4 animate-spin"/> : (
                                    <>Create &amp; start <ArrowRightIcon className="ml-1.5 size-4"/></>
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
    const labels = {software: "Software", details: "Details", provisioning: "Provision"};
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
                        {failed ? "Provisioning failed" : online ? "Server is online" : "Setting things up…"}
                    </h3>
                    <p className={`text-sm ${meta.text}`}>{meta.label}</p>
                </div>
            </div>

            <div className="h-64 overflow-auto rounded-lg bg-zinc-950 p-3 font-mono text-xs leading-relaxed text-zinc-300">
                {log.length === 0 ? <span className="text-zinc-500">Starting…</span> :
                    log.map((line, i) => <div key={i} className="whitespace-pre-wrap break-all">{line}</div>)}
            </div>

            <div className="flex justify-between">
                <Button variant="ghost" onClick={onClose}>Close</Button>
                <Button disabled={!online} onClick={onOpen}>
                    <ArrowSquareOutIcon className="mr-1.5 size-4"/> Open dashboard
                </Button>
            </div>
        </div>
    );
};

export default CreateServerDialog;
