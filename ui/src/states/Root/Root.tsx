import {Sidebar} from "@/components/Sidebar.tsx"
import {
    Breadcrumb,
    BreadcrumbItem,
    BreadcrumbLink,
    BreadcrumbList,
    BreadcrumbPage,
    BreadcrumbSeparator,
} from "@/components/ui/breadcrumb"
import {Separator} from "@/components/ui/separator"
import {
    SidebarInset,
    SidebarProvider,
    SidebarTrigger,
} from "@/components/ui/sidebar"
import {ServerInfoContext, ServerInfoProvider} from "@/contexts/ServerInfoContext.tsx";
import {SocketProvider} from "@/contexts/SocketContext.tsx";
import {ResourcesProvider} from "@/contexts/ResourcesContext.tsx";
import {useContext} from "react";
import {Navigate, Outlet, useLocation, useNavigate} from "react-router-dom";
import {getLocationByPath} from "@/states/Root/routes.tsx";
import {useMasterAuth} from "@/contexts/MasterAuthContext.tsx";
import {useServerSelection} from "@/contexts/ServerSelectionContext.tsx";
import {isMasterMode} from "@/lib/RequestUtil.ts";
import {SpinnerGapIcon, PlugsIcon, ArrowLeftIcon, ArrowsClockwiseIcon, PlayIcon} from "@phosphor-icons/react";
import {Button} from "@/components/ui/button.tsx";
import {useState} from "react";
import {toast} from "@/hooks/use-toast.ts";
import {AuroraBackground} from "@/components/AuroraBackground.tsx";
import {AnimatePresence, motion} from "motion/react";

const Loader = ({label}: { label: string }) => (
    <div className="flex min-h-screen flex-col items-center justify-center gap-3 bg-background text-muted-foreground">
        <SpinnerGapIcon className="size-8 animate-spin text-primary"/>
        <p className="text-sm">{label}</p>
    </div>
);

const ServerStatePanel = () => {
    const {tokenValid, checkToken, serverInfo} = useContext(ServerInfoContext)!;
    const {activeServer} = useServerSelection();
    const navigate = useNavigate();

    if (tokenValid === null && !serverInfo.serverSoftware) {
        return (
            <div className="flex flex-1 flex-col items-center justify-center gap-3 text-muted-foreground">
                <SpinnerGapIcon className="size-8 animate-spin text-primary"/>
                <p className="text-sm">Connecting to {activeServer?.name || "server"}…</p>
            </div>
        );
    }

    return (
        <div className="flex flex-1 flex-col items-center justify-center gap-5 text-center">
            <div className="flex size-16 items-center justify-center rounded-2xl bg-muted">
                <PlugsIcon className="size-8 text-muted-foreground"/>
            </div>
            <div className="space-y-1">
                <h2 className="text-xl font-semibold">{activeServer?.name || "Server"} is offline</h2>
                <p className="max-w-sm text-sm text-muted-foreground">
                    Start it from the server list, then open it again.
                </p>
            </div>
            <div className="flex gap-2">
                <Button variant="outline" onClick={() => navigate("/servers")}>
                    <ArrowLeftIcon className="mr-1.5 size-4"/> Back to servers
                </Button>
                <Button onClick={() => checkToken()}>
                    <ArrowsClockwiseIcon className="mr-1.5 size-4"/> Retry
                </Button>
            </div>
        </div>
    );
};

const OfflineBanner = () => {
    const {activeServer, activeServerId, startServer} = useServerSelection();
    const {checkToken} = useContext(ServerInfoContext)!;
    const [busy, setBusy] = useState(false);

    const starting = busy || activeServer?.status === "starting";

    const start = async () => {
        if (!activeServerId) return;
        setBusy(true);
        try {
            await startServer(activeServerId);
            await checkToken();
        } catch (err) {
            toast({description: (err as Error).message, variant: "destructive"});
        } finally {
            setBusy(false);
        }
    };

    return (
        <motion.div layout
                    initial={{opacity: 0, y: -8}}
                    animate={{opacity: 1, y: 0}}
                    exit={{opacity: 0, y: -8}}
                    transition={{duration: 0.25, ease: [0.22, 1, 0.36, 1]}}
                    className={`relative mx-4 mb-4 flex items-center gap-3 overflow-hidden rounded-xl border px-4 py-3 transition-colors ${
                        starting ? "border-primary/40 bg-primary/5" : "border-border/60 bg-muted/40"
                    }`}>
            {starting
                ? <SpinnerGapIcon className="size-5 shrink-0 animate-spin text-primary"/>
                : <PlugsIcon className="size-5 shrink-0 text-muted-foreground"/>}
            <div className="min-w-0 flex-1">
                <p className="text-sm font-medium">
                    {starting ? `Starting ${activeServer?.name || "server"}…` : `${activeServer?.name || "Server"} is offline`}
                </p>
                <p className="truncate text-xs text-muted-foreground">
                    {starting
                        ? "Booting up. Give it a sec..."
                        : "File management is available. Start the server for console, players and worlds."}
                </p>
            </div>
            <Button size="sm" className="vd-press" disabled={starting} onClick={start}>
                {starting
                    ? <><SpinnerGapIcon className="mr-1.5 size-4 animate-spin"/> Starting…</>
                    : <><PlayIcon weight="fill" className="mr-1.5 size-4"/> Start server</>}
            </Button>
            {starting && (
                <span className="absolute inset-x-0 bottom-0 h-0.5 overflow-hidden">
                    <span className="vd-indeterminate block h-full w-1/3 rounded-full bg-primary"/>
                </span>
            )}
        </motion.div>
    );
};

const RootLayout = () => {
    const {tokenValid, serverInfo} = useContext(ServerInfoContext)!;
    const location = useLocation();
    const navigate = useNavigate();

    return (
        <SidebarProvider>
            <Sidebar/>
            <SidebarInset className="isolate flex flex-col max-h-[var(--app-vh)] md:max-h-[calc(var(--app-vh)_-_1rem)] overflow-hidden">
                <AuroraBackground/>
                <header className="flex h-16 shrink-0 items-center gap-2">
                    <div className="flex items-center gap-2 px-4">
                        <SidebarTrigger className="-ml-1"/>
                        <Separator orientation="vertical" className="mr-2 h-4"/>
                        <Breadcrumb>
                            <BreadcrumbList>
                                <BreadcrumbItem className="hidden md:block">
                                    <BreadcrumbLink className="cursor-pointer" onClick={() => navigate("/")}>Home</BreadcrumbLink>
                                </BreadcrumbItem>
                                <BreadcrumbSeparator className="hidden md:block"/>
                                <BreadcrumbItem>
                                    <BreadcrumbPage>{getLocationByPath(location.pathname)?.name()}</BreadcrumbPage>
                                </BreadcrumbItem>
                            </BreadcrumbList>
                        </Breadcrumb>
                    </div>
                </header>
                <div className="flex-1 min-h-0 flex flex-col overflow-hidden">
                    {tokenValid === true ? (
                        <>
                            <AnimatePresence>
                                {serverInfo.offline && <OfflineBanner key="offline-banner"/>}
                            </AnimatePresence>
                            <Outlet/>
                        </>
                    ) : <ServerStatePanel/>}
                </div>
            </SidebarInset>
        </SidebarProvider>
    );
};

const MasterRoot = () => {
    const {loading, authenticated} = useMasterAuth();
    const {activeServerId} = useServerSelection();

    if (loading) return <Loader label="Loading VoxelDash…"/>;
    if (!authenticated) return <Navigate to="/login" replace/>;
    if (!activeServerId) return <Navigate to="/servers" replace/>;

    return (
        <ServerInfoProvider>
            <SocketProvider>
                <ResourcesProvider>
                    <RootLayout/>
                </ResourcesProvider>
            </SocketProvider>
        </ServerInfoProvider>
    );
};

const StandaloneRoot = () => {
    const {tokenValid} = useContext(ServerInfoContext)!;

    if (tokenValid === null) return <Loader label="Loading VoxelDash…"/>;
    if (!tokenValid) return <Navigate to="/login" replace/>;

    return (
        <ResourcesProvider>
            <RootLayout/>
        </ResourcesProvider>
    );
};

const Root = () => isMasterMode() ? <MasterRoot/> : <StandaloneRoot/>;

export default Root;
