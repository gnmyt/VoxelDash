import {createContext, ReactNode, useContext} from "react";
import {useState, useEffect} from 'react';
import useWebSocket, {ReadyState} from 'react-use-websocket';
import {ServerInfoContext} from "@/contexts/ServerInfoContext.tsx";
import {isMasterMode} from "@/lib/RequestUtil.ts";

interface SocketContextType {
    attachEventListener: (eventName: string) => void;
    detachEventListener: (eventName: string) => void;
    lastMessage: MessageEvent | null;
    readyState: ReadyState;
}

export const SocketContext = createContext<SocketContextType | undefined>(undefined);

interface SocketProps {
    children: ReactNode;
}

export const SocketProvider = (props: SocketProps) => {
    const {serverInfo} = useContext(ServerInfoContext)!;

    const [socketUrl, setSocketUrl] = useState<string | null>(null);
    const {sendMessage, lastMessage, readyState} = useWebSocket(socketUrl);

    const attachEventListener = (eventName: string) => {
        sendMessage(JSON.stringify({event: 'ATTACH', name: eventName}));
    }

    const detachEventListener = (eventName: string) => {
        sendMessage(JSON.stringify({event: 'DETACH', name: eventName}));
    }

    useEffect(() => {
        if (!serverInfo) return;

        const hostname = window.location.hostname;
        const port = window.location.port;
        const proto = window.location.protocol === 'https:' ? 'wss' : 'ws';
        const base = `${proto}://${hostname}:${port}`;

        if (isMasterMode()) {
            const masterToken = localStorage.getItem('masterToken');
            const serverId = localStorage.getItem('activeServerId');
            if (serverId) setSocketUrl(`${base}/api/proxy/${serverId}/ws?token=${masterToken}`);
        } else {
            const sessionToken = localStorage.getItem('sessionToken');
            setSocketUrl(`${base}/api/ws?sessionToken=${sessionToken}`);
        }
    }, [serverInfo]);

    return (
        <SocketContext.Provider value={{attachEventListener, detachEventListener, lastMessage, readyState}}>
            {props.children}
        </SocketContext.Provider>
    );
};
