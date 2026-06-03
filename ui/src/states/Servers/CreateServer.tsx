import {useEffect, useState} from "react";
import {useNavigate} from "react-router-dom";
import {useServerSelection} from "@/contexts/ServerSelectionContext.tsx";
import {masterJson} from "@/lib/RequestUtil.ts";
import {softwareMeta, statusMeta} from "@/lib/servers.ts";
import {Button} from "@/components/ui/button.tsx";
import {Input} from "@/components/ui/input.tsx";
import {Label} from "@/components/ui/label.tsx";
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from "@/components/ui/select.tsx";
import {Dialog, DialogContent, DialogHeader, DialogTitle} from "@/components/ui/dialog.tsx";
import {toast} from "@/hooks/use-toast.ts";
import {
    ArrowLeftIcon, ArrowRightIcon, CheckIcon, CheckCircleIcon, SpinnerGapIcon, LockSimpleIcon,
    WarningCircleIcon, ArrowSquareOutIcon,
} from "@phosphor-icons/react";

interface SoftwareEntry {
    key: string;
    name: string;
    kind: string;
    available: boolean;
    accent: string;
    tagline: string;
}

const MEMORY_PRESETS = [2048, 4096, 8192];

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
        if (!next && (creating || step === "provisioning")) return;
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
                            <button key={entry.key} disabled={!entry.available} onClick={() => chooseSoftware(entry.key)}
                                    className={`group relative flex items-start gap-4 overflow-hidden rounded-2xl border p-4 text-left transition-all
                                        ${entry.available
                                            ? "border-border/70 bg-card/40 hover:border-border hover:bg-card/80 cursor-pointer"
                                            : "border-border/40 bg-card/20 opacity-60 cursor-not-allowed"}`}>
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
                                        {!entry.available && (
                                            <span className="flex items-center gap-1 rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium text-muted-foreground">
                                                <LockSimpleIcon className="size-3"/> Soon
                                            </span>
                                        )}
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
                            <Label>Memory</Label>
                            <div className="flex gap-2">
                                {MEMORY_PRESETS.map((mb) => (
                                    <button key={mb} onClick={() => setMemoryMb(mb)}
                                            className={`flex-1 rounded-xl border px-3 py-2.5 text-sm font-medium transition-colors
                                                ${memoryMb === mb ? "border-primary bg-primary/10 text-primary" : "border-border/70 hover:border-border"}`}>
                                        {mb / 1024} GB
                                    </button>
                                ))}
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
