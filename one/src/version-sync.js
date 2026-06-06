import {db} from "./db.js";
import {registry} from "./tunnel/registry.js";
import {randomId} from "./util.js";
import {normalizeVersion} from "./updater/releases.js";

const fetchServerInfo = (serverId) =>
    new Promise((resolve) => {
        const entry = registry.get(serverId);
        if (!entry || entry.socket.readyState !== 1) {
            resolve(null);
            return;
        }

        const id = randomId();
        const chunks = [];
        let status = 502;
        let settled = false;

        const finish = () => {
            if (settled) return;
            settled = true;
            clearTimeout(timer);
            entry.pending.delete(id);
            if (status !== 200) {
                resolve(null);
                return;
            }
            try {
                resolve(JSON.parse(Buffer.concat(chunks).toString("utf8")));
            } catch {
                resolve(null);
            }
        };

        const res = {
            headersSent: false,
            status(code) {
                status = code;
                this.headersSent = true;
                return this;
            },
            setHeader() {},
            write(chunk) {
                if (chunk) chunks.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk));
                return true;
            },
            end(chunk) {
                this.write(chunk);
                finish();
            },
            json() {
                finish();
            },
        };

        const timer = setTimeout(finish, 10_000);
        entry.pending.set(id, {res, timer});
        entry.socket.send(JSON.stringify({type: "http", id, method: "GET", path: "/api/info", query: "", headers: {}}));
    });

export const syncVoxelDashVersion = async (serverId) => {
    const info = await fetchServerInfo(serverId);
    const reported = info?.voxeldashVersion;
    if (!reported || typeof reported !== "string" || reported === "unknown") return;

    const version = normalizeVersion(reported);
    const row = db.query("SELECT voxeldash_version FROM servers WHERE id = ?").get(serverId);
    if (row && row.voxeldash_version !== version) {
        db.query("UPDATE servers SET voxeldash_version = ? WHERE id = ?").run(version, serverId);
    }
};
