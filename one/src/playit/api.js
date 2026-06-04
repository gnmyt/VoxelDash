import {config} from "../config.js";

const API_BASE = "https://api.playit.gg";

export const playitCall = async (path, body = {}, secret) => {
    const headers = {"Content-Type": "application/json", "User-Agent": config.userAgent};
    if (secret) headers["Authorization"] = `Agent-Key ${secret.trim()}`;

    const response = await fetch(`${API_BASE}${path}`, {
        method: "POST",
        headers,
        body: JSON.stringify(body),
    });

    const text = await response.text();
    let parsed;
    try {
        parsed = text ? JSON.parse(text) : {};
    } catch {
        throw new Error(`playit API ${path} returned non-JSON (${response.status})`);
    }

    if (parsed && typeof parsed === "object" && "status" in parsed) {
        if (parsed.status === "success") return parsed.data;
        const detail = typeof parsed.data === "string" ? parsed.data : JSON.stringify(parsed.data);
        throw new Error(`playit API ${path} ${parsed.status}: ${detail}`);
    }

    if (!response.ok) throw new Error(`playit API ${path} failed (${response.status})`);
    return parsed;
};

export const claimSetup = (code) =>
    playitCall("/claim/setup", {code, agent_type: "self-managed", version: "voxeldash-one"});

export const claimExchange = (code) => playitCall("/claim/exchange", {code});

export const agentRunData = (secret) => playitCall("/agents/rundata", {}, secret);

export const createTunnel = (secret, {name, agentId, localPort}) =>
    playitCall(
        "/tunnels/create",
        {
            name,
            tunnel_type: "minecraft-java",
            port_type: "tcp",
            port_count: 1,
            origin: {type: "agent", data: {agent_id: agentId, local_ip: "127.0.0.1", local_port: localPort}},
            enabled: true,
            alloc: null,
            firewall_id: null,
            proxy_protocol: null,
        },
        secret
    );

export const deleteTunnel = (secret, tunnelId) => playitCall("/tunnels/delete", {tunnel_id: tunnelId}, secret);
