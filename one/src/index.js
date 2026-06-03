import express from "express";
import {createServer} from "node:http";
import {WebSocketServer} from "ws";
import {existsSync} from "node:fs";
import {join} from "node:path";
import {config} from "./config.js";
import {db} from "./db.js";
import {getUserByToken, mountAuthRoutes, requireMasterAuth} from "./auth.js";
import {mountServerRoutes} from "./servers.js";
import {handleConsoleConnection, mountProxy} from "./proxy.js";
import {handleTunnelConnection} from "./tunnel/server.js";

const app = express();
app.disable("x-powered-by");
app.use("/master", express.json({limit: "2mb"}));

mountAuthRoutes(app);
mountServerRoutes(app, requireMasterAuth);
mountProxy(app, requireMasterAuth);

const uiDist = config.uiDist;
if (existsSync(uiDist)) {
    app.use(express.static(uiDist));
    app.get("*", (req, res, next) => {
        if (req.path.startsWith("/api") || req.path.startsWith("/master")) return next();
        res.sendFile(join(uiDist, "index.html"));
    });
} else {
    console.warn(`[ui] ${uiDist} not found - run "bun run build" in ui/ to serve the dashboard`);
}

const server = createServer(app);
const tunnelWss = new WebSocketServer({noServer: true});
const proxyWss = new WebSocketServer({noServer: true});

server.on("upgrade", (req, socket, head) => {
    const url = new URL(req.url, "http://localhost");

    if (url.pathname === "/tunnel") {
        tunnelWss.handleUpgrade(req, socket, head, (ws) => handleTunnelConnection(ws));
        return;
    }

    const match = url.pathname.match(/^\/api\/proxy\/([^/]+)\/ws$/);
    if (match && getUserByToken(url.searchParams.get("token"))) {
        proxyWss.handleUpgrade(req, socket, head, (ws) => handleConsoleConnection(ws, match[1]));
        return;
    }

    socket.destroy();
});

server.listen(config.port, () => {
    console.log(`VoxelDash One listening on http://${config.masterHost}:${config.port}`);
    console.log(`Data home: ${config.home}`);
    const {c} = db.query("SELECT COUNT(*) AS c FROM servers").get();
    console.log(`${c} server(s) registered. Orphaned servers will re-attach via the tunnel.`);
});
