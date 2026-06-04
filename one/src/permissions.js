import {db} from "./db.js";

export const FEATURES = ["Servers", "Forwardings", "UserManagement"];

export const LEVEL = {NONE: 0, READ: 1, FULL: 2};

const adminId = () => {
    const row = db.query("SELECT MIN(id) AS id FROM master_users").get();
    return row ? row.id : null;
};

export const isAdmin = (userId) => userId != null && userId === adminId();

const decode = (text) => {
    const map = {};
    for (const feature of FEATURES) map[feature] = LEVEL.NONE;
    for (const part of String(text || "").split(",")) {
        const [feature, level] = part.split(":");
        if (FEATURES.includes(feature)) map[feature] = Math.max(0, Math.min(2, parseInt(level, 10) || 0));
    }
    return map;
};

const encode = (map) => FEATURES.map((f) => `${f}:${map[f] ?? 0}`).join(",");

export const getPermissions = (userId) => {
    if (isAdmin(userId)) {
        const full = {};
        for (const feature of FEATURES) full[feature] = LEVEL.FULL;
        return full;
    }
    const row = db.query("SELECT permissions FROM master_permissions WHERE user_id = ?").get(userId);
    return decode(row?.permissions);
};

export const setPermissions = (userId, map) => {
    const clean = {};
    for (const feature of FEATURES) clean[feature] = Math.max(0, Math.min(2, parseInt(map?.[feature], 10) || 0));
    db.query(
        `INSERT INTO master_permissions (user_id, permissions) VALUES (?, ?)
     ON CONFLICT(user_id) DO UPDATE SET permissions = excluded.permissions`
    ).run(userId, encode(clean));
};

export const hasLevel = (userId, feature, level) => getPermissions(userId)[feature] >= level;
export const hasRead = (userId, feature) => hasLevel(userId, feature, LEVEL.READ);
export const hasFull = (userId, feature) => hasLevel(userId, feature, LEVEL.FULL);

const allServersFlag = (userId) => {
    const row = db.query("SELECT all_servers FROM master_users WHERE id = ?").get(userId);
    return !!(row && row.all_servers);
};

export const reachesAllServers = (userId) => isAdmin(userId) || allServersFlag(userId);

export const canAccessServer = (userId, serverId) => {
    if (reachesAllServers(userId)) return true;
    return !!db.query("SELECT 1 FROM master_server_access WHERE user_id = ? AND server_id = ?").get(userId, serverId);
};

export const listAccessibleServerIds = (userId) => {
    if (reachesAllServers(userId)) return null;
    return db.query("SELECT server_id FROM master_server_access WHERE user_id = ?").all(userId).map((r) => r.server_id);
};

export const setServerAccess = (userId, allServers, serverIds) => {
    db.transaction(() => {
        db.query("UPDATE master_users SET all_servers = ? WHERE id = ?").run(allServers ? 1 : 0, userId);
        db.query("DELETE FROM master_server_access WHERE user_id = ?").run(userId);
        if (!allServers) {
            const insert = db.query("INSERT OR IGNORE INTO master_server_access (user_id, server_id) VALUES (?, ?)");
            for (const id of serverIds || []) insert.run(userId, id);
        }
    })();
};

export const describeUser = (userId) => ({
    isAdmin: isAdmin(userId),
    permissions: getPermissions(userId),
    allServers: reachesAllServers(userId),
    serverIds: listAccessibleServerIds(userId),
});
