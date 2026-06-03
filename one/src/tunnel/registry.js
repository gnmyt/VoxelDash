const tunnels = new Map();

export const registry = {
    attach(serverId, socket) {
        this.detach(serverId);
        const entry = {serverId, socket, pending: new Map(), consoleStreams: new Map()};
        tunnels.set(serverId, entry);
        return entry;
    },

    get(serverId) {
        return tunnels.get(serverId);
    },

    isOnline(serverId) {
        const entry = tunnels.get(serverId);
        return !!(entry && entry.socket.readyState === 1);
    },

    detach(serverId, socket) {
        const entry = tunnels.get(serverId);
        if (!entry) return;
        if (socket && entry.socket !== socket) return;

        for (const pending of entry.pending.values()) {
            clearTimeout(pending.timer);
            try {
                if (!pending.res.headersSent) pending.res.status(503).json({error: "Server disconnected"});
                else pending.res.end();
            } catch {
            }
        }
        for (const stream of entry.consoleStreams.values()) {
            try {
                stream.ws.close();
            } catch {
            }
        }
        tunnels.delete(serverId);
    },
};
