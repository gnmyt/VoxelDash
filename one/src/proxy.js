import express from "express";
import {registry} from "./tunnel/registry.js";
import {randomId} from "./util.js";
import {requireFeature, requireServerAccess} from "./auth.js";
import {LEVEL} from "./permissions.js";

const PROXY_TIMEOUT_MS = 60_000;
const SKIP_REQUEST_HEADERS = new Set(["host", "connection", "content-length", "authorization", "upgrade"]);

export const mountProxy = (app) => {
    app.all(
        "/api/proxy/:serverId/*",
        requireFeature("Servers", LEVEL.READ),
        requireServerAccess,
        express.raw({type: () => true, limit: "64mb"}),
        (req, res) => {
            const entry = registry.get(req.params.serverId);
            if (!entry || entry.socket.readyState !== 1) {
                return res.status(503).json({error: "Server is offline"});
            }

            const path = "/api/" + (req.params[0] || "");
            const qIndex = req.originalUrl.indexOf("?");
            const query = qIndex >= 0 ? req.originalUrl.slice(qIndex + 1) : "";

            const headers = {};
            for (const [key, value] of Object.entries(req.headers)) {
                if (SKIP_REQUEST_HEADERS.has(key.toLowerCase())) continue;
                headers[key] = Array.isArray(value) ? value.join(",") : value;
            }

            const bodyB64 = Buffer.isBuffer(req.body) && req.body.length ? req.body.toString("base64") : undefined;

            const id = randomId();
            const timer = setTimeout(() => {
                if (entry.pending.delete(id) && !res.headersSent) res.status(504).json({error: "Upstream timeout"});
            }, PROXY_TIMEOUT_MS);

            entry.pending.set(id, {res, timer});
            res.on("close", () => {
                clearTimeout(timer);
                entry.pending.delete(id);
            });

            entry.socket.send(JSON.stringify({type: "http", id, method: req.method, path, query, headers, bodyB64}));
        }
    );
};

export const handleConsoleConnection = (browserWs, serverId) => {
    const entry = registry.get(serverId);
    if (!entry || entry.socket.readyState !== 1) {
        browserWs.close(4004, "Server offline");
        return;
    }

    const streamId = randomId();
    const stream = {ws: browserWs, ready: false, queue: []};
    entry.consoleStreams.set(streamId, stream);
    entry.socket.send(JSON.stringify({type: "ws-open", streamId}));

    browserWs.on("message", (data) => {
        if (entry.socket.readyState !== 1) return;
        const text = data.toString();
        if (stream.ready) entry.socket.send(JSON.stringify({type: "ws-msg", streamId, data: text}));
        else stream.queue.push(text);
    });
    browserWs.on("close", () => {
        entry.consoleStreams.delete(streamId);
        if (entry.socket.readyState === 1) entry.socket.send(JSON.stringify({type: "ws-close", streamId}));
    });
};
