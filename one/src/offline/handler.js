import {join} from "node:path";
import {config} from "../config.js";
import {db} from "../db.js";
import {isAdmin} from "../permissions.js";
import {ok} from "./fs-helpers.js";
import {fileRoutes} from "./files.js";
import {folderRoutes} from "./folder.js";

const FEATURES = ["FileManager"];

const info = (req, res, {serverId}) => {
    const row = db.query("SELECT software, mc_version FROM servers WHERE id = ?").get(serverId);
    return ok(res, {
        serverSoftware: row?.software ?? null,
        serverVersion: row?.mc_version ?? null,
        serverPort: null,
        availableFeatures: FEATURES,
        resourceTypes: [],
        isAdmin: isAdmin(req.user?.id),
        offline: true,
    });
};

const ROUTES = [{method: "GET", path: "info", handler: info}, ...fileRoutes, ...folderRoutes];

const matchRoute = (method, sub) => {
    const segs = sub.split("/").filter(Boolean);
    for (const r of ROUTES) {
        if (r.method !== method) continue;
        const pat = r.path.split("/").filter(Boolean);
        if (pat.length !== segs.length) continue;
        const params = {};
        let matched = true;
        for (let i = 0; i < pat.length; i++) {
            if (pat[i].startsWith(":")) params[pat[i].slice(1)] = segs[i];
            else if (pat[i] !== segs[i]) {
                matched = false;
                break;
            }
        }
        if (matched) return {handler: r.handler, params};
    }
    return null;
};

export const serveOffline = async (req, res, serverId, sub) => {
    const route = matchRoute(req.method, sub);
    if (!route) return false;
    const ctx = {root: join(config.paths.servers, serverId), serverId, params: route.params};
    try {
        return await route.handler(req, res, ctx);
    } catch {
        if (!res.headersSent) res.status(400).json({error: "The request could not be completed"});
        else res.end();
        return true;
    }
};
