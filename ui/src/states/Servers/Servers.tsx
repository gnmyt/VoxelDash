import {useCallback, useEffect, useState} from "react";
import {Navigate, useNavigate} from "react-router-dom";
import {t} from "i18next";
import {useMasterAuth} from "@/contexts/MasterAuthContext.tsx";
import {ManagedServer, useServerSelection} from "@/contexts/ServerSelectionContext.tsx";
import {masterDelete, masterJson, masterRequest} from "@/lib/RequestUtil.ts";
import {softwareMeta, statusMeta} from "@/lib/servers.ts";
import {MasterLayout} from "@/states/Servers/MasterLayout.tsx";
import {PlayitTunnel} from "@/states/Servers/Forwardings/Forwardings.tsx";
import {Button} from "@/components/ui/button.tsx";
import {Skeleton} from "@/components/ui/skeleton.tsx";
import {
    Dialog, DialogContent, DialogHeader, DialogTitle,
} from "@/components/ui/dialog.tsx";
import {
    DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuSeparator, DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu.tsx";
import {
    AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription,
    AlertDialogFooter, AlertDialogHeader, AlertDialogTitle,
} from "@/components/ui/alert-dialog.tsx";
import {toast} from "@/hooks/use-toast.ts";
import CreateServerDialog from "@/states/Servers/CreateServer.tsx";
import {
    PlusIcon, PlayIcon, StopIcon, DotsThreeIcon, TrashIcon, TerminalWindowIcon,
    HardDrivesIcon, SpinnerGapIcon, GlobeSimpleIcon, CopyIcon, LinkBreakIcon,
} from "@phosphor-icons/react";

const SoftwareMark = ({software}: { software: string }) => {
    const meta = softwareMeta(software);
    if (meta.logo) {
        return (
            <div className="flex size-14 shrink-0 items-center justify-center rounded-2xl border border-border/60 bg-background">
                <img src={meta.logo} alt={meta.name} className="size-8 object-contain"
                     style={{imageRendering: meta.name === "Fabric" ? "pixelated" : "auto"}}/>
            </div>
        );
    }
    return (
        <div className="flex size-14 shrink-0 items-center justify-center rounded-2xl text-base font-bold text-white"
             style={{backgroundColor: meta.accent}}>
            {meta.short}
        </div>
    );
};

const StatusDot = ({status}: { status: string }) => {
    const meta = statusMeta(status);
    const animated = ["online", "starting", "installing"].includes(status);
    return (
        <span className={`size-2.5 shrink-0 rounded-full ${meta.dot} ${animated ? "vd-pulse" : ""}`}
              title={meta.label}
              style={{"--vd-pulse-color": `hsl(${meta.pulse} / 0.55)`} as React.CSSProperties}/>
    );
};

const Stat = ({label, value}: { label: string; value: string }) => (
    <div className="flex items-baseline gap-1.5">
        <span className="text-[11px] uppercase tracking-wide text-muted-foreground">{label}</span>
        <span className="font-medium tabular-nums text-foreground">{value}</span>
    </div>
);

const copy = (text: string) => navigator.clipboard.writeText(text).then(
    () => toast({description: t("servers.copied")}),
    () => toast({description: t("servers.copy_failed"), variant: "destructive"})
);

const ServerRow = ({server, index, onLog, tunnel, playitLinked, canForward, onForward, onRemoveForward}: {
    server: ManagedServer;
    index: number;
    onLog: (s: ManagedServer) => void;
    tunnel?: PlayitTunnel;
    playitLinked: boolean;
    canForward: boolean;
    onForward: (id: string) => Promise<void>;
    onRemoveForward: (tunnelId: string) => Promise<void>;
}) => {
    const meta = softwareMeta(server.software);
    const {selectServer, startServer, stopServer, deleteServer} = useServerSelection();
    const navigate = useNavigate();
    const [busy, setBusy] = useState(false);
    const [confirmDelete, setConfirmDelete] = useState(false);

    const isOnline = server.status === "online";
    const isBusy = server.status === "installing" || server.status === "starting";
    const canStart = server.status === "offline" || server.status === "install_failed";
    const canOpen = isOnline || server.status === "offline";

    const run = async (fn: () => Promise<void>) => {
        setBusy(true);
        try {
            await fn();
        } catch (err) {
            toast({description: (err as Error).message, variant: "destructive"});
        } finally {
            setBusy(false);
        }
    };

    const open = () => {
        if (!canOpen) return;
        selectServer(server.id);
        navigate(isOnline ? "/" : "/files/");
    };

    return (
        <div role={canOpen ? "button" : undefined} tabIndex={canOpen ? 0 : undefined}
             onClick={open}
             onKeyDown={(e) => { if (canOpen && (e.key === "Enter" || e.key === " ")) { e.preventDefault(); open(); } }}
             className={`vd-rise group flex items-center gap-5 rounded-2xl border border-border/60 bg-card/40 px-5 py-5 transition-colors hover:border-border hover:bg-card/80 ${canOpen ? "cursor-pointer" : ""}`}
             style={{animationDelay: `${index * 45}ms`}}>
            <SoftwareMark software={server.software}/>

            <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2.5">
                    <h3 className="truncate font-display text-lg font-semibold leading-tight">{server.name}</h3>
                    <StatusDot status={server.status}/>
                </div>
                <p className="truncate text-sm text-muted-foreground">
                    {meta.name}{server.mcVersion ? ` · ${server.mcVersion}` : ""}
                </p>
                {tunnel?.assignedDomain && (
                    <button onClick={(e) => { e.stopPropagation(); copy(tunnel.assignedDomain!); }}
                            className="group/addr mt-1 flex items-center gap-1.5 text-xs text-primary hover:underline">
                        <GlobeSimpleIcon className="size-3.5"/>
                        <span className="truncate font-mono">{tunnel.assignedDomain}</span>
                        <CopyIcon className="size-3 opacity-0 transition-opacity group-hover/addr:opacity-100"/>
                    </button>
                )}
            </div>

            <div className="hidden items-center gap-3 rounded-xl border border-border/50 bg-muted/30 px-4 py-2 text-xs lg:flex">
                <Stat label={t("servers.stat.port")} value={server.gamePort ? String(server.gamePort) : "-"}/>
                <span className="h-3.5 w-px bg-border"/>
                <Stat label={t("servers.stat.ram")} value={server.memoryMb ? `${server.memoryMb / 1024} GB` : "-"}/>
                <span className="h-3.5 w-px bg-border"/>
                <Stat label={t("servers.stat.build")} value={server.build || "-"}/>
            </div>

            <div className="flex shrink-0 flex-wrap items-center gap-1.5" onClick={(e) => e.stopPropagation()}>
                {canStart && (
                    <Button size="sm" disabled={busy} onClick={() => run(() => startServer(server.id))}>
                        {busy ? <SpinnerGapIcon className="size-4 animate-spin"/> : <><PlayIcon weight="fill" className="mr-1.5 size-4"/> {t("servers.start")}</>}
                    </Button>
                )}
                {(isOnline || server.status === "starting") && (
                    <Button size="icon" variant="outline" className="size-9" disabled={busy} onClick={() => run(() => stopServer(server.id))}>
                        <StopIcon weight="fill" className="size-4"/>
                    </Button>
                )}
                {server.status === "installing" && (
                    <Button size="sm" variant="secondary" onClick={() => onLog(server)}>
                        <SpinnerGapIcon className="mr-1.5 size-4 animate-spin"/> {t("servers.installing")}
                    </Button>
                )}

                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button size="icon" variant="ghost" className="size-9 shrink-0">
                            <DotsThreeIcon weight="bold" className="size-5"/>
                        </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                        <DropdownMenuItem onClick={() => onLog(server)}>
                            <TerminalWindowIcon className="mr-2 size-4"/> {t("servers.view_log")}
                        </DropdownMenuItem>
                        {canForward && (tunnel ? (
                            <DropdownMenuItem disabled={busy} onClick={() => run(() => onRemoveForward(tunnel.tunnelId))}>
                                <LinkBreakIcon className="mr-2 size-4"/> {t("servers.remove_forwarding")}
                            </DropdownMenuItem>
                        ) : playitLinked ? (
                            <DropdownMenuItem disabled={busy || !server.gamePort}
                                              onClick={() => run(() => onForward(server.id))}>
                                <GlobeSimpleIcon className="mr-2 size-4"/> {t("servers.forward_with_playit")}
                            </DropdownMenuItem>
                        ) : (
                            <DropdownMenuItem onClick={() => navigate("/forwardings")}>
                                <GlobeSimpleIcon className="mr-2 size-4"/> {t("servers.set_up_forwarding")}
                            </DropdownMenuItem>
                        ))}
                        <DropdownMenuSeparator/>
                        <DropdownMenuItem className="text-destructive focus:text-destructive"
                                          disabled={isBusy}
                                          onSelect={(e) => {
                                              e.preventDefault();
                                              setConfirmDelete(true);
                                          }}>
                            <TrashIcon className="mr-2 size-4"/> {t("action.delete")}
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            </div>

            <AlertDialog open={confirmDelete} onOpenChange={setConfirmDelete}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle className="font-display">{t("servers.delete.title", {name: server.name})}</AlertDialogTitle>
                        <AlertDialogDescription>
                            {t("servers.delete.description")}
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>{t("action.cancel")}</AlertDialogCancel>
                        <AlertDialogAction
                            className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                            onClick={() => run(() => deleteServer(server.id))}>
                            {t("action.delete")}
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </div>
    );
};

const LogDialog = ({server, onClose}: { server: ManagedServer | null; onClose: () => void }) => {
    const [log, setLog] = useState<string[]>([]);

    useEffect(() => {
        if (!server) return;
        let active = true;
        const poll = async () => {
            try {
                const data = await masterJson(`servers/${server.id}`);
                if (active) setLog(data.log || []);
            } catch {}
        };
        poll();
        const interval = setInterval(poll, 1500);
        return () => {
            active = false;
            clearInterval(interval);
        };
    }, [server?.id]);

    return (
        <Dialog open={!!server} onOpenChange={(o) => !o && onClose()}>
            <DialogContent className="w-full max-w-2xl">
                <DialogHeader>
                    <DialogTitle className="font-display">{t("servers.log_title", {name: server?.name})}</DialogTitle>
                </DialogHeader>
                <div className="h-80 overflow-auto rounded-lg bg-zinc-950 p-3 font-mono text-xs leading-relaxed text-zinc-300">
                    {log.length === 0 ? <span className="text-zinc-500">{t("servers.waiting_for_output")}</span> :
                        log.map((line, i) => <div key={i} className="whitespace-pre-wrap break-all">{line}</div>)}
                </div>
            </DialogContent>
        </Dialog>
    );
};

const Servers = () => {
    const {authenticated, loading: authLoading, can} = useMasterAuth();
    const {servers, loading} = useServerSelection();
    const [logServer, setLogServer] = useState<ManagedServer | null>(null);
    const [createOpen, setCreateOpen] = useState(false);

    const canForward = can("Forwardings", 2);
    const [playitLinked, setPlayitLinked] = useState(false);
    const [tunnelsByServer, setTunnelsByServer] = useState<Record<string, PlayitTunnel>>({});

    const loadPlayit = useCallback(async () => {
        if (!canForward) return;
        try {
            const status = await masterJson("playit/status");
            setPlayitLinked(!!status.linked);
            if (status.linked) {
                const data = await masterJson("playit/tunnels");
                const map: Record<string, PlayitTunnel> = {};
                for (const t of (data.tunnels || []) as PlayitTunnel[]) if (t.serverId) map[t.serverId] = t;
                setTunnelsByServer(map);
            }
        } catch { /* ignore */ }
    }, [canForward]);

    useEffect(() => {
        if (!authenticated || !canForward) return;
        loadPlayit();
        const interval = setInterval(loadPlayit, 5000);
        return () => clearInterval(interval);
    }, [authenticated, canForward, loadPlayit]);

    const onForward = async (serverId: string) => {
        const res = await masterRequest("playit/tunnels", "POST", {serverId});
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || t("servers.forward_create_failed"));
        if (data.tunnel) setTunnelsByServer((prev) => ({...prev, [serverId]: data.tunnel}));
        toast({description: t("servers.forward_created")});
        loadPlayit();
    };

    const onRemoveForward = async (tunnelId: string) => {
        await masterDelete(`playit/tunnels/${tunnelId}`);
        setTunnelsByServer((prev) => {
            const next = {...prev};
            for (const k of Object.keys(next)) if (next[k].tunnelId === tunnelId) delete next[k];
            return next;
        });
        toast({description: t("servers.forward_removed")});
    };

    if (!authLoading && !authenticated) return <Navigate to="/login" replace/>;

    return (
        <MasterLayout active="servers" title={t("servers.title")}
                      subtitle={t("servers.count", {count: servers.length})}
                      actions={can("Servers", 2) ? (
                          <Button onClick={() => setCreateOpen(true)}>
                              <PlusIcon weight="bold" className="mr-1.5 size-4"/> {t("servers.new_server")}
                          </Button>
                      ) : undefined}>
            {loading && servers.length === 0 ? (
                <div className="space-y-2.5">
                    {[0, 1, 2, 3].map((i) => <Skeleton key={i} className="h-[98px] rounded-2xl"/>)}
                </div>
            ) : servers.length === 0 ? (
                <EmptyState onCreate={() => setCreateOpen(true)} canCreate={can("Servers", 2)}/>
            ) : (
                <div className="space-y-2.5">
                    {servers.map((server, i) => (
                        <ServerRow key={server.id} server={server} index={i} onLog={setLogServer}
                                   tunnel={tunnelsByServer[server.id]} playitLinked={playitLinked}
                                   canForward={canForward} onForward={onForward} onRemoveForward={onRemoveForward}/>
                    ))}
                </div>
            )}

            <LogDialog server={logServer} onClose={() => setLogServer(null)}/>
            <CreateServerDialog open={createOpen} onOpenChange={setCreateOpen}/>
        </MasterLayout>
    );
};

const EmptyState = ({onCreate, canCreate}: { onCreate: () => void; canCreate: boolean }) => (
    <div className="vd-rise flex flex-col items-center justify-center rounded-3xl border border-dashed border-border/70 bg-card/30 px-6 py-20 text-center">
        <div className="mb-5 flex size-16 items-center justify-center rounded-2xl bg-muted">
            <HardDrivesIcon className="size-8 text-muted-foreground"/>
        </div>
        <h3 className="font-display text-xl font-bold">{t("servers.empty.title")}</h3>
        <p className="mb-6 mt-1 max-w-sm text-sm text-muted-foreground">
            {canCreate
                ? t("servers.empty.can_create")
                : t("servers.empty.no_access")}
        </p>
        {canCreate && (
            <Button size="lg" onClick={onCreate}>
                <PlusIcon weight="bold" className="mr-1.5 size-4"/> {t("servers.create_server")}
            </Button>
        )}
    </div>
);

export default Servers;
