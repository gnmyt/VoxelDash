import {useEffect, useRef, useState} from "react";
import {Navigate} from "react-router-dom";
import {t} from "i18next";
import {
    ArrowSquareOutIcon, CopyIcon, GlobeSimpleIcon, LinkIcon, PlugsIcon, SpinnerGapIcon, TrashIcon,
} from "@phosphor-icons/react";
import {useMasterAuth} from "@/contexts/MasterAuthContext.tsx";
import {masterDelete, masterJson, masterRequest} from "@/lib/RequestUtil.ts";
import {MasterLayout} from "@/states/Servers/MasterLayout.tsx";
import {Button} from "@/components/ui/button.tsx";
import {Skeleton} from "@/components/ui/skeleton.tsx";
import {
    AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription,
    AlertDialogFooter, AlertDialogHeader, AlertDialogTitle,
} from "@/components/ui/alert-dialog.tsx";
import {toast} from "@/hooks/use-toast.ts";

export interface PlayitTunnel {
    tunnelId: string;
    serverId: string | null;
    serverName: string | null;
    localPort: number;
    name: string | null;
    assignedDomain: string | null;
    disabled: boolean;
    proto: string;
}

interface PlayitStatus {
    linked: boolean;
    enabled: boolean;
    agentRunning: boolean;
    agentState: "stopped" | "connecting" | "connected" | "error";
    agentConnected: boolean;
    agentRestarts: number;
    agentId: string | null;
    claimPending: boolean;
}

const AGENT_BADGE: Record<string, { labelKey: string; dot: string; text: string; pulse?: string }> = {
    connected: {labelKey: "forwardings.agent.connected", dot: "bg-green-500", text: "text-green-600 dark:text-green-400"},
    connecting: {labelKey: "forwardings.agent.connecting", dot: "bg-yellow-500", text: "text-yellow-600 dark:text-yellow-400", pulse: "45 93% 47%"},
    error: {labelKey: "forwardings.agent.error", dot: "bg-red-500", text: "text-red-600 dark:text-red-400", pulse: "0 84% 60%"},
    stopped: {labelKey: "forwardings.agent.stopped", dot: "bg-muted-foreground", text: "text-muted-foreground"},
};

const AgentBadge = ({status}: { status: PlayitStatus }) => {
    const meta = AGENT_BADGE[status.agentState] || AGENT_BADGE.stopped;
    return (
        <div className="flex items-center gap-1.5 rounded-full border border-border/60 bg-muted/30 px-2.5 py-1 text-xs font-medium"
             title={status.agentRestarts ? t("forwardings.restarts", {count: status.agentRestarts}) : undefined}>
            <span className={`size-2 rounded-full ${meta.dot} ${meta.pulse ? "vd-pulse" : ""}`}
                  style={meta.pulse ? {"--vd-pulse-color": `hsl(${meta.pulse} / 0.55)`} as React.CSSProperties : undefined}/>
            <span className={meta.text}>{t(meta.labelKey)}</span>
        </div>
    );
};

const copy = (text: string) => {
    navigator.clipboard.writeText(text).then(
        () => toast({description: t("forwardings.copied")}),
        () => toast({description: t("forwardings.copy_failed"), variant: "destructive"})
    );
};

const LinkPanel = ({onLinked}: { onLinked: () => void }) => {
    const [claimUrl, setClaimUrl] = useState<string | null>(null);
    const [busy, setBusy] = useState(false);
    const pollRef = useRef<ReturnType<typeof setInterval>>();

    useEffect(() => () => clearInterval(pollRef.current), []);

    const startClaim = async () => {
        setBusy(true);
        try {
            const res = await masterRequest("playit/claim", "POST");
            const data = await res.json();
            if (!res.ok) throw new Error(data.error || t("forwardings.link_start_failed"));
            setClaimUrl(data.url);
            window.open(data.url, "_blank", "noopener");
            pollRef.current = setInterval(async () => {
                try {
                    const poll = await masterJson("playit/claim");
                    if (poll.linked) {
                        clearInterval(pollRef.current);
                        toast({description: t("forwardings.linked")});
                        onLinked();
                    }
                } catch { /* keep polling */ }
            }, 2000);
        } catch (err) {
            toast({description: (err as Error).message, variant: "destructive"});
            setBusy(false);
        }
    };

    return (
        <div className="vd-rise flex flex-col items-center justify-center rounded-3xl border border-dashed border-border/70 bg-card/30 px-6 py-16 text-center">
            <div className="mb-5 flex size-16 items-center justify-center rounded-2xl bg-muted">
                <GlobeSimpleIcon className="size-8 text-muted-foreground"/>
            </div>
            <h3 className="font-display text-xl font-bold">{t("forwardings.connect_title")}</h3>
            <p className="mb-6 mt-1 max-w-md text-sm text-muted-foreground">
                {t("forwardings.connect_description")}
            </p>
            {claimUrl ? (
                <div className="flex flex-col items-center gap-3">
                    <div className="flex items-center gap-2 text-sm text-muted-foreground">
                        <SpinnerGapIcon className="size-4 animate-spin"/> {t("forwardings.waiting_approval")}
                    </div>
                    <a href={claimUrl} target="_blank" rel="noopener noreferrer"
                       className="inline-flex items-center gap-1.5 text-sm font-medium text-primary hover:underline">
                        {t("forwardings.open_approval")} <ArrowSquareOutIcon className="size-4"/>
                    </a>
                </div>
            ) : (
                <Button size="lg" disabled={busy} onClick={startClaim}>
                    {busy ? <SpinnerGapIcon className="mr-1.5 size-4 animate-spin"/> :
                        <LinkIcon weight="bold" className="mr-1.5 size-4"/>}
                    {t("forwardings.link_account")}
                </Button>
            )}
        </div>
    );
};

const TunnelRow = ({tunnel, index, onRemove}: { tunnel: PlayitTunnel; index: number; onRemove: (t: PlayitTunnel) => void }) => (
    <div className="vd-rise flex items-center gap-4 rounded-2xl border border-border/60 bg-card/40 px-5 py-4"
         style={{animationDelay: `${index * 45}ms`}}>
        <div className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-muted">
            <GlobeSimpleIcon className="size-5 text-muted-foreground"/>
        </div>
        <div className="min-w-0 flex-1">
            <div className="flex items-center gap-2">
                <h3 className="truncate font-display font-semibold leading-tight">
                    {tunnel.serverName || tunnel.name || t("forwardings.tunnel")}
                </h3>
                {tunnel.disabled && <span className="rounded bg-yellow-500/15 px-1.5 py-0.5 text-[11px] text-yellow-600 dark:text-yellow-400">{t("forwardings.disabled")}</span>}
            </div>
            {tunnel.assignedDomain ? (
                <button onClick={() => copy(tunnel.assignedDomain!)}
                        className="group flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground">
                    <span className="truncate font-mono">{tunnel.assignedDomain}</span>
                    <CopyIcon className="size-3.5 opacity-0 transition-opacity group-hover:opacity-100"/>
                </button>
            ) : (
                <span className="flex items-center gap-1.5 text-sm text-muted-foreground">
                    <SpinnerGapIcon className="size-3.5 animate-spin"/> {t("forwardings.assigning_address")}
                </span>
            )}
        </div>
        <div className="hidden items-center gap-3 rounded-xl border border-border/50 bg-muted/30 px-4 py-2 text-xs sm:flex">
            <span className="text-muted-foreground">→ 127.0.0.1:</span>
            <span className="font-medium tabular-nums">{tunnel.localPort}</span>
        </div>
        <Button size="icon" variant="ghost" className="size-9 shrink-0 text-destructive hover:text-destructive"
                onClick={() => onRemove(tunnel)}>
            <TrashIcon className="size-4"/>
        </Button>
    </div>
);

const Forwardings = () => {
    const {authenticated, loading: authLoading, can} = useMasterAuth();
    const [status, setStatus] = useState<PlayitStatus | null>(null);
    const [tunnels, setTunnels] = useState<PlayitTunnel[]>([]);
    const [loading, setLoading] = useState(true);
    const [removeTarget, setRemoveTarget] = useState<PlayitTunnel | null>(null);

    const load = async () => {
        try {
            const st = await masterJson("playit/status");
            setStatus(st);
            if (st.linked) {
                const data = await masterJson("playit/tunnels");
                setTunnels(data.tunnels || []);
            }
        } catch { /* ignore */ } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (!authenticated) return;
        load();
        const interval = setInterval(load, 4000);
        return () => clearInterval(interval);
    }, [authenticated]);

    const disconnect = async () => {
        await masterRequest("playit/disconnect", "POST");
        setTunnels([]);
        await load();
        toast({description: t("forwardings.disconnected")});
    };

    const remove = async () => {
        if (!removeTarget) return;
        const target = removeTarget;
        setRemoveTarget(null);
        try {
            await masterDelete(`playit/tunnels/${target.tunnelId}`);
            setTunnels((prev) => prev.filter((t) => t.tunnelId !== target.tunnelId));
        } catch {
            toast({description: t("forwardings.remove_failed"), variant: "destructive"});
        }
    };

    if (!authLoading && !authenticated) return <Navigate to="/login" replace/>;
    if (!authLoading && authenticated && !can("Forwardings", 1)) return <Navigate to="/servers" replace/>;

    const headerActions = status?.linked ? (
        <>
            <AgentBadge status={status}/>
            <Button variant="outline" size="sm" onClick={disconnect}>
                <PlugsIcon className="mr-1.5 size-4"/> {t("forwardings.disconnect")}
            </Button>
        </>
    ) : undefined;

    return (
        <MasterLayout active="forwardings" title={t("forwardings.title")}
                      subtitle={status?.linked ? t("forwardings.active_count", {count: tunnels.length}) : undefined}
                      actions={headerActions}>
            {loading ? (
                <div className="space-y-2.5">{[0, 1, 2].map((i) => <Skeleton key={i} className="h-[76px] rounded-2xl"/>)}</div>
            ) : !status?.linked ? (
                <LinkPanel onLinked={load}/>
            ) : tunnels.length === 0 ? (
                <div className="vd-rise flex flex-col items-center justify-center rounded-3xl border border-dashed border-border/70 bg-card/30 px-6 py-20 text-center">
                    <div className="mb-5 flex size-16 items-center justify-center rounded-2xl bg-muted">
                        <GlobeSimpleIcon className="size-8 text-muted-foreground"/>
                    </div>
                    <h3 className="font-display text-xl font-bold">{t("forwardings.empty.title")}</h3>
                    <p className="mt-1 max-w-sm text-sm text-muted-foreground">
                        {t("forwardings.empty.description")}
                    </p>
                </div>
            ) : (
                <div className="space-y-2.5">
                    {tunnels.map((tunnel, i) => (
                        <TunnelRow key={tunnel.tunnelId} tunnel={tunnel} index={i} onRemove={setRemoveTarget}/>
                    ))}
                </div>
            )}

            <AlertDialog open={!!removeTarget} onOpenChange={(o) => !o && setRemoveTarget(null)}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle className="font-display">{t("forwardings.remove.title")}</AlertDialogTitle>
                        <AlertDialogDescription>
                            {t("forwardings.remove.description_prefix")}{" "}
                            <span className="font-mono">{removeTarget?.assignedDomain || t("forwardings.this_tunnel")}</span>.
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>{t("action.cancel")}</AlertDialogCancel>
                        <AlertDialogAction className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                                           onClick={remove}>{t("action.remove")}</AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </MasterLayout>
    );
};

export default Forwardings;
