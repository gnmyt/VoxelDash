import {config} from "../config.js";
import {getSoftware} from "./software/index.js";
import {logProgress, processes} from "../runtime.js";

const AIKAR_FLAGS =
    "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions " +
    "-XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 " +
    "-XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 " +
    "-XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -Dusing.aikars.flags=https://mcflags.emc.gs " +
    "-Daikars.new.flags=true";

function jvmFlags(memoryMb) {
    return [`-Xms${Math.floor(memoryMb / 2)}M`, `-Xmx${memoryMb}M`, ...AIKAR_FLAGS.split(" ")];
}

export function launchServer(server, javaPath, serverDir) {
    const software = getSoftware(server.software);
    const launchArgs = software.launchArgs("server.jar");
    const args = [...jvmFlags(server.memory_mb || 2048), ...launchArgs];

    const env = {
        ...process.env,
        VOXELDASH_MC_VERSION: server.mc_version || "",
        VOXELDASH_MASTER: JSON.stringify({
            masterHost: config.masterHost,
            masterPort: config.port,
            token: server.api_token,
            apiPort: server.api_port,
        }),
    };

    logProgress(server.id, `Launching: java ${launchArgs.join(" ")}`);

    const proc = Bun.spawn([javaPath, ...args], {
        cwd: serverDir,
        env,
        stdout: "pipe",
        stderr: "pipe",
        stdin: "pipe",
        onExit(_proc, exitCode) {
            processes.delete(server.id);
            logProgress(server.id, `Process exited (code ${exitCode})`);
        },
    });

    processes.set(server.id, proc);
    pipeLogs(server.id, proc.stdout);
    pipeLogs(server.id, proc.stderr);
    return proc;
}

export function stopServer(server) {
    const proc = processes.get(server.id);
    if (!proc) return false;
    try {
        proc.stdin?.write(server.software === "bungeecord" ? "end\n" : "stop\n");
        proc.stdin?.flush?.();
    } catch {
    }
    setTimeout(() => {
        if (processes.has(server.id)) {
            try {
                proc.kill();
            } catch {
            }
        }
    }, 15_000);
    return true;
}

async function pipeLogs(serverId, stream) {
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
                logProgress(serverId, buffer.slice(0, idx));
                buffer = buffer.slice(idx + 1);
            }
        }
    } catch {
    }
}
