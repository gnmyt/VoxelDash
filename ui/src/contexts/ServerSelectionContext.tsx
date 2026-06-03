import {createContext, ReactNode, useContext, useEffect, useState} from "react";
import {getActiveServerId, masterDelete, masterJson, masterPost, setActiveServerId} from "@/lib/RequestUtil.ts";
import {useMasterAuth} from "@/contexts/MasterAuthContext.tsx";

export interface ManagedServer {
    id: string;
    name: string;
    software: string;
    mcVersion?: string;
    build?: string;
    gamePort?: number;
    javaMajor?: number;
    memoryMb?: number;
    status: "installing" | "install_failed" | "offline" | "starting" | "online";
    createdAt?: string;
}

interface CreatePayload {
    name: string;
    software: string;
    mcVersion: string;
    memoryMb?: number;
}

interface ServerSelectionContextType {
    servers: ManagedServer[];
    loading: boolean;
    activeServerId: string | null;
    activeServer: ManagedServer | null;
    refresh: () => Promise<ManagedServer[]>;
    selectServer: (id: string | null) => void;
    createServer: (payload: CreatePayload) => Promise<ManagedServer>;
    startServer: (id: string) => Promise<void>;
    stopServer: (id: string) => Promise<void>;
    deleteServer: (id: string) => Promise<void>;
}

export const ServerSelectionContext = createContext<ServerSelectionContextType | undefined>(undefined);

export const useServerSelection = () => useContext(ServerSelectionContext)!;

export const ServerSelectionProvider = ({children}: { children: ReactNode }) => {
    const {authenticated} = useMasterAuth();
    const [servers, setServers] = useState<ManagedServer[]>([]);
    const [loading, setLoading] = useState(true);
    const [activeServerId, setActiveId] = useState<string | null>(getActiveServerId());

    const refresh = async () => {
        const data = await masterJson("servers");
        const list: ManagedServer[] = data.servers || [];
        setServers(list);
        setLoading(false);
        return list;
    };

    const selectServer = (id: string | null) => {
        setActiveServerId(id);
        setActiveId(id);
    };

    const createServer = async (payload: CreatePayload): Promise<ManagedServer> => {
        const res = await masterPost("servers", payload);
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || "Failed to create server");
        await refresh();
        return data.server;
    };

    const action = async (verb: string, id: string) => {
        const res = await masterPost(`servers/${id}/${verb}`);
        if (!res.ok) {
            const data = await res.json().catch(() => ({}));
            throw new Error(data.error || `Failed to ${verb} server`);
        }
        await refresh();
    };

    const startServer = (id: string) => action("start", id);
    const stopServer = (id: string) => action("stop", id);
    const deleteServer = async (id: string) => {
        const res = await masterDelete(`servers/${id}`);
        if (!res.ok) throw new Error("Failed to delete server");
        if (getActiveServerId() === id) selectServer(null);
        await refresh();
    };

    useEffect(() => {
        if (!authenticated) {
            setServers([]);
            setLoading(false);
            return;
        }
        refresh().catch(() => setLoading(false));
        const interval = setInterval(() => refresh().catch(() => {}), 4000);
        return () => clearInterval(interval);
    }, [authenticated]);

    const activeServer = servers.find((s) => s.id === activeServerId) || null;

    return (
        <ServerSelectionContext.Provider
            value={{servers, loading, activeServerId, activeServer, refresh, selectServer, createServer,
                startServer, stopServer, deleteServer}}>
            {children}
        </ServerSelectionContext.Provider>
    );
};
