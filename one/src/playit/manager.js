import {db} from "../db.js";
import {randomToken} from "../util.js";
import {agentRunData, claimExchange, claimSetup, createTunnel, deleteTunnel} from "./api.js";
import {ensureAgentBinary, getAgentHealth, isAgentRunning, startAgent, stopAgent} from "./agent.js";

const RUNDATA_TTL_MS = 3000;
const ASSIGN_POLL_ATTEMPTS = 6;
const ASSIGN_POLL_INTERVAL_MS = 800;

const loadConfig = () => db.query("SELECT * FROM playit_config WHERE id = 1").get() || null;

const saveConfig = (patch) => {
    const current = loadConfig() || {secret: null, agent_id: null, claim_code: null, enabled: 0};
    const next = {...current, ...patch};
    db.query(
        `INSERT INTO playit_config (id, secret, agent_id, claim_code, enabled, updated_at)
     VALUES (1, ?, ?, ?, ?, datetime('now'))
     ON CONFLICT(id) DO UPDATE SET
       secret = excluded.secret, agent_id = excluded.agent_id,
       claim_code = excluded.claim_code, enabled = excluded.enabled, updated_at = excluded.updated_at`
    ).run(next.secret ?? null, next.agent_id ?? null, next.claim_code ?? null, next.enabled ? 1 : 0);
    return loadConfig();
};

const secretOf = () => loadConfig()?.secret || null;

let runDataCache = {at: 0, value: null};

const fetchRunData = async (secret, force = false) => {
    const now = Date.now();
    if (!force && runDataCache.value && now - runDataCache.at < RUNDATA_TTL_MS) return runDataCache.value;
    const value = await agentRunData(secret);
    runDataCache = {at: now, value};
    return value;
};

export const getStatus = () => {
    const cfg = loadConfig();
    const agent = getAgentHealth();
    return {
        linked: !!cfg?.secret,
        enabled: !!cfg?.enabled,
        agentRunning: isAgentRunning(),
        agentState: agent.state,
        agentConnected: agent.connected,
        agentRestarts: agent.restarts,
        agentId: cfg?.agent_id || null,
        claimPending: !!(cfg?.claim_code && !cfg?.secret),
    };
};

export const startClaim = async () => {
    const code = randomToken(5);
    await claimSetup(code);
    saveConfig({claim_code: code, secret: null, enabled: 0});
    return {code, url: `https://playit.gg/claim/${code}`};
};

const ensureAgentId = async () => {
    const cfg = loadConfig();
    if (!cfg?.secret) return null;
    if (cfg.agent_id) return cfg.agent_id;
    const data = await fetchRunData(cfg.secret, true);
    if (data?.agent_id) saveConfig({agent_id: data.agent_id});
    return data?.agent_id || null;
};

export const pollClaim = async () => {
    const cfg = loadConfig();
    if (cfg?.secret) return {linked: true, state: "UserAccepted"};
    if (!cfg?.claim_code) return {linked: false, state: "NoClaim"};

    const state = await claimSetup(cfg.claim_code);
    if (state !== "UserAccepted") return {linked: false, state};

    const {secret_key: secret} = await claimExchange(cfg.claim_code);
    saveConfig({secret, claim_code: null, enabled: 1});
    await ensureAgentBinary();
    await startAgent(secret);
    await ensureAgentId();
    return {linked: true, state: "UserAccepted"};
};

export const disconnect = () => {
    stopAgent();
    runDataCache = {at: 0, value: null};
    db.query("DELETE FROM playit_tunnels").run();
    saveConfig({secret: null, agent_id: null, claim_code: null, enabled: 0});
};

const getServerRow = (serverId) =>
    db.query("SELECT id, name, game_port FROM servers WHERE id = ?").get(serverId);

const reconcileFromAgent = async (force = false) => {
    const secret = secretOf();
    if (!secret) return new Map();
    let data;
    try {
        data = await fetchRunData(secret, force);
    } catch {
        return new Map();
    }
    const byId = new Map();
    for (const t of data?.tunnels || []) {
        const domain = t.custom_domain || t.assigned_domain || null;
        byId.set(t.id, {assignedDomain: domain, disabled: !!t.disabled, localPort: t.local_port});
        db.query("UPDATE playit_tunnels SET assigned_domain = ? WHERE tunnel_id = ?").run(domain, t.id);
    }
    return byId;
};

export const listTunnels = async (force = false) => {
    const live = await reconcileFromAgent(force);
    const rows = db
        .query(
            `SELECT pt.*, s.name AS server_name
       FROM playit_tunnels pt LEFT JOIN servers s ON s.id = pt.server_id
       ORDER BY pt.created_at DESC`
        )
        .all();
    return rows.map((r) => ({
        tunnelId: r.tunnel_id,
        serverId: r.server_id,
        serverName: r.server_name,
        localPort: r.local_port,
        name: r.name,
        assignedDomain: live.get(r.tunnel_id)?.assignedDomain ?? r.assigned_domain ?? null,
        disabled: live.get(r.tunnel_id)?.disabled ?? false,
        proto: r.proto || "tcp",
    }));
};

export const createTunnelForServer = async (serverId) => {
    const secret = secretOf();
    if (!secret) throw new Error("playit is not linked");

    const server = getServerRow(serverId);
    if (!server) throw new Error("Server not found");

    const existing = db.query("SELECT tunnel_id FROM playit_tunnels WHERE server_id = ?").get(serverId);
    if (existing) throw new Error("This server already has a forwarding");

    const agentId = await ensureAgentId();
    if (!agentId) throw new Error("playit agent is not ready yet, try again in a moment");

    const created = await createTunnel(secret, {name: server.name, agentId, localPort: server.game_port});
    const tunnelId = created?.id || created;

    db.query(
        `INSERT INTO playit_tunnels (tunnel_id, server_id, local_port, name, proto)
     VALUES (?, ?, ?, ?, 'tcp')`
    ).run(tunnelId, serverId, server.game_port, server.name);

    for (let i = 0; i < ASSIGN_POLL_ATTEMPTS; i++) {
        const match = (await listTunnels(true)).find((t) => t.tunnelId === tunnelId);
        if (match?.assignedDomain) return match;
        await new Promise((r) => setTimeout(r, ASSIGN_POLL_INTERVAL_MS));
    }
    return (await listTunnels(true)).find((t) => t.tunnelId === tunnelId);
};

export const removeTunnel = async (tunnelId) => {
    const secret = secretOf();
    if (secret) {
        try {
            await deleteTunnel(secret, tunnelId);
        } catch {
        }
    }
    db.query("DELETE FROM playit_tunnels WHERE tunnel_id = ?").run(tunnelId);
};

export const removeTunnelsForServer = async (serverId) => {
    const rows = db.query("SELECT tunnel_id FROM playit_tunnels WHERE server_id = ?").all(serverId);
    for (const row of rows) await removeTunnel(row.tunnel_id);
};

export const bootstrapPlayit = async () => {
    const cfg = loadConfig();
    if (cfg?.enabled && cfg?.secret) {
        try {
            await ensureAgentBinary();
            await startAgent(cfg.secret);
        } catch (err) {
            console.warn(`[playit] failed to start agent on boot: ${err.message}`);
        }
    }
};
