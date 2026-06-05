import { useState, createContext, useContext, useEffect, ReactNode } from "react";
import { isMasterMode, request } from "@/lib/RequestUtil.ts";
import { ResourceType } from "@/types/resource";
import { ServerSelectionContext } from "@/contexts/ServerSelectionContext.tsx";

interface ServerInfo {
    accountName?: string;
    serverSoftware?: string;
    serverVersion?: string;
    serverPort?: number;
    availableFeatures?: string[];
    resourceTypes?: ResourceType[];
    isAdmin?: boolean;
    offline?: boolean;
}

interface ServerInfoContextType {
    tokenValid: boolean | null;
    checkToken: () => Promise<boolean | undefined>;
    serverInfo: ServerInfo;
}

export const ServerInfoContext = createContext<ServerInfoContextType | undefined>(undefined);

interface ServerInfoProviderProps {
    children: ReactNode;
}

export const ServerInfoProvider = (props: ServerInfoProviderProps) => {
    const selection = useContext(ServerSelectionContext);
    const activeServerId = selection?.activeServerId ?? null;
    const activeStatus = selection?.activeServer?.status ?? null;
    const [tokenValid, setTokenValid] = useState<boolean | null>(null);
    const [serverInfo, setServerInfo] = useState<ServerInfo>({});

    const checkToken = async (): Promise<boolean | undefined> => {
        try {
            const r = await request("info");
            setTokenValid(r.status === 200);
            setServerInfo(r.status === 200 ? await r.json() : {});
            return r.status === 200;
        } catch {
            setTokenValid(false);
            setServerInfo({});
        }
    };

    useEffect(() => {
        setTokenValid(null);
        setServerInfo({});
        if (!isMasterMode() || activeServerId) checkToken();
    }, [activeServerId, activeStatus]);

    return (
        <ServerInfoContext.Provider value={{ tokenValid, checkToken, serverInfo }}>
            {props.children}
        </ServerInfoContext.Provider>
    );
};
