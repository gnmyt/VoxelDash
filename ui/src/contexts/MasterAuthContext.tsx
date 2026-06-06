import {createContext, ReactNode, useContext, useEffect, useState} from "react";
import {getMasterToken, masterJson, masterRequest, setActiveServerId, setMasterToken} from "@/lib/RequestUtil.ts";
import {t} from "i18next";

export type MasterPermissionLevel = 0 | 1 | 2;

interface MasterUser {
    id: number;
    username: string;
    isAdmin?: boolean;
    permissions?: Record<string, MasterPermissionLevel>;
    allServers?: boolean;
    serverIds?: string[] | null;
}

interface MasterAuthContextType {
    loading: boolean;
    authenticated: boolean;
    setupRequired: boolean;
    user: MasterUser | null;
    can: (feature: string, level?: MasterPermissionLevel) => boolean;
    login: (username: string, password: string) => Promise<void>;
    setup: (username: string, password: string) => Promise<void>;
    logout: () => Promise<void>;
}

export const MasterAuthContext = createContext<MasterAuthContextType | undefined>(undefined);

export const useMasterAuth = () => useContext(MasterAuthContext)!;

export const MasterAuthProvider = ({children}: { children: ReactNode }) => {
    const [loading, setLoading] = useState(true);
    const [authenticated, setAuthenticated] = useState(false);
    const [setupRequired, setSetupRequired] = useState(false);
    const [user, setUser] = useState<MasterUser | null>(null);

    const bootstrap = async () => {
        try {
            const status = await masterJson("status");
            setSetupRequired(!!status.setupRequired);

            if (getMasterToken()) {
                const res = await masterRequest("me");
                if (res.ok) {
                    const data = await res.json();
                    setUser(data.user);
                    setAuthenticated(true);
                } else {
                    setMasterToken(null);
                }
            }
        } catch {} finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        bootstrap();
    }, []);

    const refreshMe = async () => {
        const res = await masterRequest("me");
        if (res.ok) {
            const data = await res.json();
            setUser(data.user);
        }
    };

    const handleAuthResponse = async (res: Response) => {
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || t("context.request_failed"));
        setMasterToken(data.token);
        setUser(data.user);
        setAuthenticated(true);
        setSetupRequired(false);
        await refreshMe();
    };

    const can = (feature: string, level: MasterPermissionLevel = 1) => {
        if (!user) return false;
        if (user.isAdmin) return true;
        return (user.permissions?.[feature] ?? 0) >= level;
    };

    const login = async (username: string, password: string) => {
        await handleAuthResponse(await masterRequest("auth/login", "POST", {username, password}));
    };

    const setup = async (username: string, password: string) => {
        await handleAuthResponse(await masterRequest("auth/setup", "POST", {username, password}));
    };

    const logout = async () => {
        try {
            await masterRequest("auth/logout", "POST");
        } catch {}
        setMasterToken(null);
        setActiveServerId(null);
        setUser(null);
        setAuthenticated(false);
    };

    return (
        <MasterAuthContext.Provider value={{loading, authenticated, setupRequired, user, can, login, setup, logout}}>
            {children}
        </MasterAuthContext.Provider>
    );
};
