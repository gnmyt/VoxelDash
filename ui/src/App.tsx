import Login from "@/states/Login/Login.tsx";

import i18n from "./i18n.ts";
import {useEffect, useState} from "react";
import {createBrowserRouter, RouterProvider} from "react-router-dom";

import {getRoutes} from "@/states/Root/routes.tsx";
import {ThemeProvider} from "@/components/theme-provider.tsx";
import Root from "@/states/Root/Root.tsx";
import {Toaster} from "@/components/ui/toaster.tsx";
import {MasterAuthProvider} from "@/contexts/MasterAuthContext.tsx";
import {ServerSelectionProvider} from "@/contexts/ServerSelectionContext.tsx";
import {ServerInfoProvider} from "@/contexts/ServerInfoContext.tsx";
import {SocketProvider} from "@/contexts/SocketContext.tsx";
import {detectInstanceMode} from "@/lib/RequestUtil.ts";
import {isDesktop} from "@/lib/desktop.ts";
import {TitleBar} from "@/components/TitleBar.tsx";
import {PWAManager} from "@/components/PWAManager.tsx";
import {ReactNode} from "react";
import Servers from "@/states/Servers/Servers.tsx";
import Forwardings from "@/states/Servers/Forwardings/Forwardings.tsx";
import MasterUsers from "@/states/Servers/Users/Users.tsx";
import Updates from "@/states/Servers/Updates/Updates.tsx";
import { ResourceList, ResourceStore, ResourceDetail } from "@/states/Root/pages/Resources";
import NotFound from "@/states/Root/pages/NotFound/NotFound.tsx";

export const SUPPORT_URL = "https://ko-fi.com/gnmyt";

const resourceRoutes = [
    { path: "/resources/:type/store", element: <ResourceStore /> },
    { path: "/resources/:type/:fileName", element: <ResourceDetail /> },
    { path: "/resources/:type", element: <ResourceList /> },
];

const Shell = ({children}: { children: ReactNode }) =>
    isDesktop()
        ? (
            <div className="flex h-[100vh] flex-col overflow-hidden">
                <TitleBar/>
                <div className="min-h-0 flex-1">{children}</div>
            </div>
        )
        : <>{children}<PWAManager/></>;

const App = () => {
    const [translationsLoaded, setTranslationsLoaded] = useState(false);
    const [mode, setMode] = useState<"master" | "standalone" | null>(null);

    i18n.on("initialized", () => setTranslationsLoaded(true));

    useEffect(() => {
        detectInstanceMode().then((isMaster) => setMode(isMaster ? "master" : "standalone"));
    }, []);

    if (!translationsLoaded || mode === null) return <div>Loading...</div>;

    const dashboardRoute = {
        path: "/",
        element: <Root />,
        children: [...getRoutes(), ...resourceRoutes, { path: "*", element: <NotFound /> }],
    };

    if (mode === "standalone") {
        const router = createBrowserRouter([
            {path: "/login", element: <Login />},
            dashboardRoute,
        ]);
        return (
            <Shell>
                <ThemeProvider defaultTheme="dark" storageKey="theme">
                    <ServerInfoProvider>
                        <SocketProvider>
                            <Toaster />
                            <RouterProvider router={router}/>
                        </SocketProvider>
                    </ServerInfoProvider>
                </ThemeProvider>
            </Shell>
        );
    }

    const router = createBrowserRouter([
        {path: "/login", element: <Login />},
        {path: "/servers", element: <Servers />},
        {path: "/forwardings", element: <Forwardings />},
        {path: "/users", element: <MasterUsers />},
        {path: "/updates", element: <Updates />},
        dashboardRoute,
    ]);
    return (
        <Shell>
            <ThemeProvider defaultTheme="dark" storageKey="theme">
                <MasterAuthProvider>
                    <ServerSelectionProvider>
                        <Toaster />
                        <RouterProvider router={router}/>
                    </ServerSelectionProvider>
                </MasterAuthProvider>
            </ThemeProvider>
        </Shell>
    );
};

export default App;
