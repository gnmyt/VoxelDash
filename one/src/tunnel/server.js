import {registry} from "./registry.js";
import {db} from "../db.js";

const HOP_BY_HOP = new Set(["content-length", "transfer-encoding", "connection", "keep-alive", "upgrade"]);

export const handleTunnelConnection = (socket) => {
    let serverId = null;

    socket.on("message", (raw) => {
        let msg;
        try {
            msg = JSON.parse(raw.toString());
        } catch {
            return;
        }

        if (!serverId) {
            if (msg.type !== "hello") return;
            const row = db.query("SELECT id FROM servers WHERE api_token = ?").get(msg.token);
            if (!row) {
                socket.close(4001, "unknown token");
                return;
            }
            serverId = row.id;
            registry.attach(serverId, socket);
            socket.send(JSON.stringify({type: "hello-ack"}));
            console.log(`[tunnel] server ${serverId} connected`);
            return;
        }

        routeFrame(serverId, msg);
    });

    socket.on("close", () => {
        if (serverId) {
            registry.detach(serverId, socket);
            console.log(`[tunnel] server ${serverId} disconnected`);
        }
    });
    socket.on("error", () => {
        if (serverId) registry.detach(serverId, socket);
    });
};

const routeFrame = (serverId, msg) => {
    const entry = registry.get(serverId);
    if (!entry) return;

    switch (msg.type) {
        case "http-res": {
            const pending = entry.pending.get(msg.id);
            if (!pending) break;
            clearTimeout(pending.timer);
            try {
                pending.res.status(msg.status || 502);
                for (const [key, value] of Object.entries(msg.headers || {})) {
                    if (!HOP_BY_HOP.has(key.toLowerCase()) && value != null) pending.res.setHeader(key, value);
                }
            } catch {
            }
            if (!msg.hasBody) {
                try {
                    pending.res.end();
                } catch {
                }
                entry.pending.delete(msg.id);
            }
            break;
        }

        case "http-body-chunk": {
            const pending = entry.pending.get(msg.id);
            if (!pending) break;
            try {
                if (msg.dataB64) pending.res.write(Buffer.from(msg.dataB64, "base64"));
                if (msg.last) {
                    pending.res.end();
                    entry.pending.delete(msg.id);
                }
            } catch {
                entry.pending.delete(msg.id);
            }
            break;
        }

        case "ws-msg": {
            const stream = entry.consoleStreams.get(msg.streamId);
            if (stream && stream.ws.readyState === 1) stream.ws.send(msg.data);
            break;
        }

        case "ws-open-ack": {
            const stream = entry.consoleStreams.get(msg.streamId);
            if (stream) {
                stream.ready = true;
                for (const queued of stream.queue) {
                    entry.socket.send(JSON.stringify({type: "ws-msg", streamId: msg.streamId, data: queued}));
                }
                stream.queue = [];
            }
            break;
        }

        case "ws-open-err":
        case "ws-close": {
            const stream = entry.consoleStreams.get(msg.streamId);
            if (stream) {
                try {
                    stream.ws.close();
                } catch {
                }
            }
            entry.consoleStreams.delete(msg.streamId);
            break;
        }

        case "ping":
            entry.socket.send(JSON.stringify({type: "pong", ts: msg.ts}));
            break;
    }
};
