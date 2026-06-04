import {LEVEL} from "../permissions.js";
import {
    createTunnelForServer,
    disconnect,
    getStatus,
    listTunnels,
    pollClaim,
    removeTunnel,
    startClaim,
} from "./manager.js";

export const mountPlayitRoutes = (app, requireFeature) => {
    const canView = requireFeature("Forwardings", LEVEL.READ);
    const canManage = requireFeature("Forwardings", LEVEL.FULL);

    app.get("/master/playit/status", canView, (req, res) => {
        res.json(getStatus());
    });

    app.get("/master/playit/tunnels", canView, async (req, res) => {
        try {
            res.json({tunnels: await listTunnels()});
        } catch (err) {
            res.status(502).json({error: err.message});
        }
    });

    app.post("/master/playit/claim", canManage, async (req, res) => {
        try {
            res.json(await startClaim());
        } catch (err) {
            res.status(502).json({error: err.message});
        }
    });

    app.get("/master/playit/claim", canManage, async (req, res) => {
        try {
            res.json(await pollClaim());
        } catch (err) {
            res.status(502).json({error: err.message});
        }
    });

    app.post("/master/playit/disconnect", canManage, (req, res) => {
        disconnect();
        res.json({ok: true});
    });

    app.post("/master/playit/tunnels", canManage, async (req, res) => {
        const {serverId} = req.body || {};
        if (!serverId) return res.status(400).json({error: "serverId is required"});
        try {
            res.status(201).json({tunnel: await createTunnelForServer(serverId)});
        } catch (err) {
            res.status(400).json({error: err.message});
        }
    });

    app.delete("/master/playit/tunnels/:tunnelId", canManage, async (req, res) => {
        try {
            await removeTunnel(req.params.tunnelId);
            res.json({ok: true});
        } catch (err) {
            res.status(502).json({error: err.message});
        }
    });
};
