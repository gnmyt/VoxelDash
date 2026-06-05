import {requireMasterAuth} from "../auth.js";
import {isAdmin} from "../permissions.js";
import {getSettings, setAutoUpdate, setChannel, isAutoUpdate} from "./settings.js";
import {listUpdatableServers, updateServer} from "./servers.js";
import {nodeStatus, updateNode} from "./selfupdate.js";

const requireAdmin = (req, res, next) => {
    requireMasterAuth(req, res, () => {
        if (!isAdmin(req.user.id)) return res.status(403).json({error: "Admin only"});
        next();
    });
};

export const mountUpdateRoutes = (app) => {
    app.get("/master/updates/status", requireAdmin, async (req, res) => {
        try {
            res.json({...getSettings(), node: await nodeStatus()});
        } catch (err) {
            res.status(502).json({error: err.message});
        }
    });

    app.get("/master/updates/servers", requireAdmin, async (req, res) => {
        try {
            res.json({servers: await listUpdatableServers()});
        } catch (err) {
            res.status(502).json({error: err.message});
        }
    });

    app.post("/master/updates/settings", requireAdmin, (req, res) => {
        const {channel, autoUpdate} = req.body || {};
        try {
            if (channel !== undefined) setChannel(channel);
            if (autoUpdate !== undefined) setAutoUpdate(!!autoUpdate);
            res.json(getSettings());
        } catch (err) {
            res.status(400).json({error: err.message});
        }
    });

    app.post("/master/updates/servers/:id", requireAdmin, async (req, res) => {
        try {
            res.json(await updateServer(req.params.id));
        } catch (err) {
            res.status(400).json({error: err.message});
        }
    });

    app.post("/master/updates/node", requireAdmin, async (req, res) => {
        try {
            res.json(await updateNode());
        } catch (err) {
            res.status(400).json({error: err.message});
        }
    });
};

const CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000;

const runAutoUpdate = async () => {
    if (!isAutoUpdate()) return;
    try {
        const servers = await listUpdatableServers();
        for (const server of servers.filter((s) => s.updatable)) {
            try {
                console.log(`[updater] auto-updating server ${server.name} (${server.id})`);
                await updateServer(server.id);
            } catch (err) {
                console.warn(`[updater] failed to update server ${server.id}: ${err.message}`);
            }
        }

        const node = await nodeStatus();
        if (node.updatable && node.selfUpdate.supported) {
            console.log(`[updater] auto-updating node to ${node.latest}`);
            await updateNode((line) => console.log(`[updater] ${line}`));
        }
    } catch (err) {
        console.warn(`[updater] auto-update check failed: ${err.message}`);
    }
};

export const startAutoUpdater = () => {
    setTimeout(runAutoUpdate, 30_000);
    setInterval(runAutoUpdate, CHECK_INTERVAL_MS);
};
