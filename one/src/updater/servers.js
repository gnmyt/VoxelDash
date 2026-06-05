import {join} from "node:path";
import {copyFileSync, existsSync} from "node:fs";
import {db} from "../db.js";
import {config} from "../config.js";
import {getSoftware} from "../provision/software/index.js";
import {downloadArtifactFromRelease} from "../provision/voxeldash.js";
import {ensureJre, requiredMajorForMc} from "../provision/jre.js";
import {launchServer, stopServer} from "../provision/launcher.js";
import {effectiveStatus, logProgress, processes} from "../runtime.js";
import {getChannel} from "./settings.js";
import {isNewer, latestForChannel, normalizeVersion} from "./releases.js";

const serverDirFor = (id) => join(config.paths.servers, id);

const artifactFor = (software, mcVersion) =>
    software.resolveArtifact ? software.resolveArtifact(mcVersion) : software.voxeldashArtifact;

const jarPathFor = (software, dir) => {
    if (software.loaderJar) return join(dir, "server.jar");
    const installDir = software.installDir ? join(dir, software.installDir) : dir;
    return join(installDir, "voxeldash.jar");
};

export const listUpdatableServers = async () => {
    const release = await latestForChannel(getChannel());
    const latest = release ? normalizeVersion(release.tag_name) : null;
    const rows = db.query("SELECT * FROM servers ORDER BY created_at DESC").all();

    return rows.map((row) => {
        const software = getSoftware(row.software);
        const current = row.voxeldash_version || null;
        const provisioned = row.status !== "installing" && row.status !== "install_failed" && !!software;
        return {
            id: row.id,
            name: row.name,
            software: row.software,
            artifact: software ? artifactFor(software, row.mc_version) : null,
            current: current || "unknown",
            latest,
            updatable: provisioned && isNewer(latest, current),
        };
    });
};

export const updateServer = async (id, onLog = () => {}) => {
    const row = db.query("SELECT * FROM servers WHERE id = ?").get(id);
    if (!row) throw new Error("Server not found");

    const software = getSoftware(row.software);
    if (!software) throw new Error(`Unknown software: ${row.software}`);

    const channel = getChannel();
    const release = await latestForChannel(channel);
    if (!release) throw new Error(`No release available on the ${channel} channel`);

    const log = (line) => { onLog(line); logProgress(id, line); };

    const dir = serverDirFor(id);
    const target = jarPathFor(software, dir);
    if (!existsSync(dir)) throw new Error("Server is not provisioned");

    const artifact = artifactFor(software, row.mc_version);
    log(`Updating VoxelDash (${artifact}) to ${release.tag_name}...`);
    const download = await downloadArtifactFromRelease(artifact, release, log);
    copyFileSync(download.path, target);
    db.query("UPDATE servers SET voxeldash_version = ? WHERE id = ?").run(download.version, id);
    log(`Installed VoxelDash ${download.version} on ${row.name}.`);

    if (effectiveStatus(row) === "online") {
        if (stopServer(row)) {
            log("Restarting server to load the new version...");
            await restart(row, dir, log);
        } else {
            log("Update applied. Restart the server to load the new version.");
        }
    }

    return {version: download.version};
};

const restart = async (row, dir, log) => {
    const deadline = Date.now() + 20_000;
    while (processes.has(row.id) && Date.now() < deadline) {
        await new Promise((r) => setTimeout(r, 500));
    }
    try {
        const javaPath = await ensureJre(row.java_major || requiredMajorForMc(row.mc_version));
        launchServer(row, javaPath, dir);
        log("Server restarted.");
    } catch (err) {
        log(`Failed to restart automatically: ${err.message}. Start it manually.`);
    }
};
