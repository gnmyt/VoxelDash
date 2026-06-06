import {useCallback, useEffect, useState} from "react";
import {Navigate} from "react-router-dom";
import {t} from "i18next";
import {
    ArrowsClockwiseIcon, CheckCircleIcon, CubeIcon, DownloadSimpleIcon, SpinnerGapIcon,
} from "@phosphor-icons/react";
import {useMasterAuth} from "@/contexts/MasterAuthContext.tsx";
import {masterJson, masterRequest} from "@/lib/RequestUtil.ts";
import {softwareMeta} from "@/lib/servers.ts";
import {MasterLayout} from "@/states/Servers/MasterLayout.tsx";
import {Button} from "@/components/ui/button.tsx";
import {Switch} from "@/components/ui/switch.tsx";
import {Skeleton} from "@/components/ui/skeleton.tsx";
import {toast} from "@/hooks/use-toast.ts";

type Channel = "release" | "beta";

interface SelfUpdate {
    supported: boolean;
    reason: string | null;
}

interface NodeStatus {
    current: string;
    latest: string | null;
    updatable: boolean;
    selfUpdate: SelfUpdate;
}

interface UpdateStatus {
    channel: Channel;
    autoUpdate: boolean;
    node: NodeStatus;
}

interface UpdatableServer {
    id: string;
    name: string;
    software: string;
    artifact: string | null;
    current: string;
    latest: string | null;
    updatable: boolean;
}

const VersionDelta = ({current, latest}: { current: string; latest: string | null }) => (
    <span className="flex items-center gap-1.5 font-mono text-xs">
        <span className="text-muted-foreground">{current}</span>
        {latest && latest !== current && (
            <>
                <span className="text-muted-foreground">→</span>
                <span className="font-medium text-primary">{latest}</span>
            </>
        )}
    </span>
);

const ChannelToggle = ({value, onChange, disabled}: {
    value: Channel; onChange: (c: Channel) => void; disabled: boolean;
}) => {
    const options: { key: Channel; label: string; hint: string }[] = [
        {key: "release", label: t("updates.channel.release"), hint: t("updates.channel.release_hint")},
        {key: "beta", label: t("updates.channel.beta"), hint: t("updates.channel.beta_hint")},
    ];
    return (
        <div className="inline-flex rounded-xl border border-border/60 bg-muted/30 p-1">
            {options.map((o) => (
                <button key={o.key} disabled={disabled} onClick={() => onChange(o.key)} title={o.hint}
                        className={`rounded-lg px-4 py-1.5 text-sm font-medium transition-colors disabled:opacity-50 ${
                            value === o.key ? "bg-background text-foreground shadow-sm" : "text-muted-foreground hover:text-foreground"
                        }`}>
                    {o.label}
                </button>
            ))}
        </div>
    );
};

const Row = ({icon, title, subtitle, current, latest, updatable, busy, disabled, disabledReason, onUpdate, index}: {
    icon: React.ReactNode;
    title: string;
    subtitle: string;
    current: string;
    latest: string | null;
    updatable: boolean;
    busy: boolean;
    disabled?: boolean;
    disabledReason?: string | null;
    onUpdate: () => void;
    index: number;
}) => (
    <div className="vd-rise flex items-center gap-4 rounded-2xl border border-border/60 bg-card/40 px-5 py-4"
         style={{animationDelay: `${index * 45}ms`}}>
        <div className="flex size-11 shrink-0 items-center justify-center rounded-xl border border-border/60 bg-background">
            {icon}
        </div>
        <div className="min-w-0 flex-1">
            <h3 className="truncate font-display text-base font-semibold leading-tight">{title}</h3>
            <p className="truncate text-sm text-muted-foreground">{subtitle}</p>
        </div>
        <div className="hidden sm:block"><VersionDelta current={current} latest={latest}/></div>
        {updatable ? (
            <Button size="sm" disabled={busy || disabled} onClick={onUpdate}
                    title={disabled ? disabledReason || undefined : undefined}>
                {busy
                    ? <SpinnerGapIcon className="size-4 animate-spin"/>
                    : <><DownloadSimpleIcon weight="bold" className="mr-1.5 size-4"/> {t("updates.update")}</>}
            </Button>
        ) : (
            <span className="flex items-center gap-1.5 text-xs font-medium text-emerald-500">
                <CheckCircleIcon weight="fill" className="size-4"/> {t("updates.up_to_date")}
            </span>
        )}
    </div>
);

const Updates = () => {
    const {authenticated, loading: authLoading, user} = useMasterAuth();
    const [status, setStatus] = useState<UpdateStatus | null>(null);
    const [servers, setServers] = useState<UpdatableServer[]>([]);
    const [loading, setLoading] = useState(true);
    const [savingSettings, setSavingSettings] = useState(false);
    const [busyId, setBusyId] = useState<string | null>(null);
    const [restarting, setRestarting] = useState(false);

    const load = useCallback(async () => {
        try {
            const [s, srv] = await Promise.all([
                masterJson("updates/status"),
                masterJson("updates/servers"),
            ]);
            if (!s.error) setStatus(s);
            if (!srv.error) setServers(srv.servers || []);
        } catch { /* ignore */ } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        if (!authenticated) return;
        load();
        const interval = setInterval(load, 15000);
        return () => clearInterval(interval);
    }, [authenticated, load]);

    const saveSetting = async (body: { channel?: Channel; autoUpdate?: boolean }) => {
        setSavingSettings(true);
        try {
            const res = await masterRequest("updates/settings", "POST", body);
            const data = await res.json();
            if (!res.ok) throw new Error(data.error || t("updates.save_failed"));
            await load();
        } catch (err) {
            toast({description: (err as Error).message, variant: "destructive"});
        } finally {
            setSavingSettings(false);
        }
    };

    const updateOneServer = async (server: UpdatableServer) => {
        setBusyId(server.id);
        try {
            const res = await masterRequest(`updates/servers/${server.id}`, "POST");
            const data = await res.json();
            if (!res.ok) throw new Error(data.error || t("updates.update_failed"));
            toast({description: t("updates.server_updated", {name: server.name, version: data.version || status?.node.latest})});
            await load();
        } catch (err) {
            toast({description: (err as Error).message, variant: "destructive"});
        } finally {
            setBusyId(null);
        }
    };

    const updateNode = async () => {
        setBusyId("node");
        try {
            const res = await masterRequest("updates/node", "POST");
            const data = await res.json();
            if (!res.ok) throw new Error(data.error || t("updates.update_failed"));
            toast({description: t("updates.node_updating", {version: data.version})});
            setRestarting(true);
            const poll = setInterval(async () => {
                try {
                    await masterJson("updates/status");
                    clearInterval(poll);
                    window.location.reload();
                } catch {
                    /* keep polling until the backend is back */
                }
            }, 3000);
        } catch (err) {
            toast({description: (err as Error).message, variant: "destructive"});
            setBusyId(null);
        }
    };

    if (!authLoading && !authenticated) return <Navigate to="/login" replace/>;
    if (!authLoading && authenticated && user && !user.isAdmin) return <Navigate to="/servers" replace/>;

    const node = status?.node;
    const nodeSubtitle = node?.selfUpdate.supported
        ? t("updates.node_subtitle")
        : node?.selfUpdate.reason || t("updates.node_subtitle");
    const updatableServers = servers.filter((s) => s.updatable);
    const nothingToUpdate = !!node && !node.updatable && updatableServers.length === 0;

    return (
        <MasterLayout active="updates" title={t("updates.title")}
                      subtitle={status ? (status.channel === "beta" ? t("updates.beta_channel") : t("updates.release_channel")) : undefined}>
            {loading && !status ? (
                <div className="space-y-4">
                    <Skeleton className="h-40 rounded-2xl"/>
                    <Skeleton className="h-24 rounded-2xl"/>
                </div>
            ) : !status ? (
                <div className="rounded-2xl border border-border/60 bg-card/40 px-5 py-10 text-center text-sm text-muted-foreground">
                    {t("updates.load_failed")}
                </div>
            ) : (
                <div className="space-y-6">
                    <div className="rounded-2xl border border-border/60 bg-card/40 p-5">
                        <div className="flex flex-wrap items-center justify-between gap-4">
                            <div>
                                <h2 className="font-display text-base font-semibold">{t("updates.release_channel")}</h2>
                                <p className="text-sm text-muted-foreground">
                                    {t("updates.release_channel_description")}
                                </p>
                            </div>
                            <ChannelToggle value={status.channel} disabled={savingSettings}
                                           onChange={(c) => saveSetting({channel: c})}/>
                        </div>
                        <div className="mt-5 flex items-center justify-between gap-4 border-t border-border/50 pt-5">
                            <div>
                                <h2 className="font-display text-base font-semibold">{t("updates.auto_updates")}</h2>
                                <p className="text-sm text-muted-foreground">
                                    {t("updates.auto_updates_description")}
                                </p>
                            </div>
                            <Switch checked={status.autoUpdate} disabled={savingSettings}
                                    onCheckedChange={(v) => saveSetting({autoUpdate: v})}/>
                        </div>
                    </div>

                    {status.autoUpdate ? (
                        <div className="flex items-center gap-3 rounded-2xl border border-border/60 bg-muted/20 px-5 py-4 text-sm text-muted-foreground">
                            <ArrowsClockwiseIcon className="size-5 shrink-0 text-primary"/>
                            {t("updates.auto_on", {channel: status.channel === "beta" ? t("updates.channel.beta") : t("updates.channel.release")})}
                        </div>
                    ) : nothingToUpdate ? (
                        <div className="vd-rise flex flex-col items-center justify-center rounded-3xl border border-dashed border-border/70 bg-card/30 px-6 py-16 text-center">
                            <div className="mb-4 flex size-14 items-center justify-center rounded-2xl bg-muted">
                                <CheckCircleIcon weight="fill" className="size-7 text-emerald-500"/>
                            </div>
                            <h3 className="font-display text-lg font-bold">{t("updates.everything_up_to_date")}</h3>
                            <p className="mt-1 max-w-sm text-sm text-muted-foreground">
                                {t("updates.latest_build", {channel: status.channel === "beta" ? t("updates.channel.beta_lower") : t("updates.channel.release_lower")})}
                            </p>
                        </div>
                    ) : (
                        <div className="space-y-2.5">
                            {node && (
                                <Row index={0}
                                     icon={<CubeIcon weight="duotone" className="size-6 text-primary"/>}
                                     title="VoxelDash One"
                                     subtitle={nodeSubtitle}
                                     current={node.current}
                                     latest={node.latest}
                                     updatable={node.updatable}
                                     busy={busyId === "node" || restarting}
                                     disabled={!node.selfUpdate.supported}
                                     disabledReason={node.selfUpdate.reason}
                                     onUpdate={updateNode}/>
                            )}
                            {updatableServers.map((server, i) => {
                                const meta = softwareMeta(server.software);
                                return (
                                    <Row key={server.id} index={i + 1}
                                         icon={meta.logo
                                             ? <img src={meta.logo} alt={meta.name} className="size-6 object-contain"
                                                    style={{imageRendering: meta.name === "Fabric" ? "pixelated" : "auto"}}/>
                                             : <span className="text-sm font-bold" style={{color: meta.accent}}>{meta.short}</span>}
                                         title={server.name}
                                         subtitle={`${meta.name} · VoxelDash ${meta.kind.toLowerCase()}`}
                                         current={server.current}
                                         latest={server.latest}
                                         updatable={server.updatable}
                                         busy={busyId === server.id}
                                         onUpdate={() => updateOneServer(server)}/>
                                );
                            })}
                        </div>
                    )}
                </div>
            )}
        </MasterLayout>
    );
};

export default Updates;
