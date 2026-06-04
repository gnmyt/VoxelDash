import {join} from "node:path";
import {arch as osArch, platform as osPlatform} from "node:os";
import {chmodSync, existsSync, mkdirSync, rmSync} from "node:fs";
import {config} from "../config.js";

const RELEASE = "https://github.com/playit-cloud/playit-agent/releases/latest/download";

const playitDir = () => join(config.home, "playit");

const assetName = () => {
    if (osPlatform() === "win32") return "playit-windows-x86_64.exe";
    if (osPlatform() === "linux") {
        const arch = {x64: "amd64", arm64: "aarch64", arm: "armv7", ia32: "i686"}[osArch()];
        if (!arch) throw new Error(`Unsupported Linux architecture for playit: ${osArch()}`);
        return `playit-linux-${arch}`;
    }
    throw new Error(`playit.gg is only supported on Linux and Windows (got ${osPlatform()})`);
};

const binaryPath = () => join(playitDir(), osPlatform() === "win32" ? "playit.exe" : "playit");

export const ensureAgentBinary = async (onLog) => {
    const dest = binaryPath();
    if (existsSync(dest)) return dest;

    mkdirSync(playitDir(), {recursive: true});
    const url = `${RELEASE}/${assetName()}`;
    onLog?.(`Downloading playit agent (${assetName()})...`);
    const response = await fetch(url, {headers: {"User-Agent": config.userAgent}, redirect: "follow"});
    if (!response.ok) throw new Error(`Failed to download playit agent (${response.status})`);
    await Bun.write(dest, await response.arrayBuffer());
    if (osPlatform() !== "win32") chmodSync(dest, 0o755);
    onLog?.("playit agent ready");
    return dest;
};

let agentProc = null;
let wantRunning = false;

const QUICK_EXIT_MS = 8000;
const RESTART_DELAY_MS = 3000;

const health = {connected: false, lastSpawnAt: 0, quickExits: 0, restarts: 0};

const markLine = (line) => {
    if (line.includes("agent registered") || line.includes("udp session details received")) {
        health.connected = true;
        health.quickExits = 0;
    }
};

const pipe = async (stream) => {
    if (!stream) return;
    const reader = stream.getReader();
    const decoder = new TextDecoder();
    let buffer = "";
    try {
        while (true) {
            const {done, value} = await reader.read();
            if (done) break;
            buffer += decoder.decode(value, {stream: true});
            let idx;
            while ((idx = buffer.indexOf("\n")) >= 0) {
                const line = buffer.slice(0, idx).trim();
                if (line) {
                    console.log(`[playit] ${line}`);
                    markLine(line);
                }
                buffer = buffer.slice(idx + 1);
            }
        }
    } catch {
    }
};

const socketPath = () =>
    osPlatform() === "win32" ? "\\\\.\\pipe\\voxeldash-playitd" : join(playitDir(), "playitd.sock");

export const startAgent = async (secret) => {
    if (agentProc) return;
    const binary = await ensureAgentBinary();
    wantRunning = true;

    const spawn = () => {
        if (!wantRunning) return;
        const socket = socketPath();
        if (osPlatform() !== "win32" && existsSync(socket)) {
            try {
                rmSync(socket);
            } catch {
            }
        }
        health.lastSpawnAt = Date.now();
        agentProc = Bun.spawn([binary, "--secret", secret, "--socket-path", socket], {
            cwd: playitDir(),
            stdout: "pipe",
            stderr: "pipe",
            onExit() {
                agentProc = null;
                const uptime = Date.now() - health.lastSpawnAt;
                health.connected = false;
                health.restarts++;
                health.quickExits = uptime < QUICK_EXIT_MS ? health.quickExits + 1 : 0;
                if (wantRunning) setTimeout(spawn, RESTART_DELAY_MS);
            },
        });
        pipe(agentProc.stdout);
        pipe(agentProc.stderr);
        console.log("[playit] agent started");
    };

    spawn();
};

export const stopAgent = () => {
    wantRunning = false;
    health.connected = false;
    health.quickExits = 0;
    if (agentProc) {
        try {
            agentProc.kill();
        } catch {
        }
        agentProc = null;
    }
};

export const isAgentRunning = () => !!agentProc;

export const getAgentHealth = () => {
    if (!wantRunning) return {state: "stopped", connected: false, restarts: health.restarts};
    if (health.connected) return {state: "connected", connected: true, restarts: health.restarts};
    if (health.quickExits >= 2) return {state: "error", connected: false, restarts: health.restarts};
    return {state: "connecting", connected: false, restarts: health.restarts};
};
