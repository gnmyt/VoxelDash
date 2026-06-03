import {registry} from "./tunnel/registry.js";

export const processes = new Map();

const progressStore = new Map();
const MAX_LOG = 300;

export function logProgress(serverId, line) {
    let log = progressStore.get(serverId);
    if (!log) progressStore.set(serverId, (log = []));
    log.push(line);
    if (log.length > MAX_LOG) log.shift();
    console.log(`[server ${serverId}] ${line}`);
}

export function getLog(serverId) {
    return progressStore.get(serverId) || [];
}

export function clearLog(serverId) {
    progressStore.delete(serverId);
}

export function effectiveStatus(server) {
    if (server.status === "installing" || server.status === "install_failed") return server.status;
    if (registry.isOnline(server.id)) return "online";
    if (processes.has(server.id)) return "starting";
    return "offline";
}
