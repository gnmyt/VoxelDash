import {basename, join} from "node:path";
import {existsSync, readdirSync, statSync} from "node:fs";
import {config} from "../config.js";
import {findServerAsset, latestForChannel, normalizeVersion} from "../updater/releases.js";
import {getChannel} from "../updater/settings.js";

const ARTIFACT_MODULE = {
    spigot: "spigot",
    bungeecord: "bungeecord",
    fabric8: "fabric/mc1.8",
    fabric12: "fabric/mc1.12",
    fabric14: "fabric/mc1.14",
    fabric16: "fabric/mc1.16",
    fabric20: "fabric/mc1.20",
    fabric: "fabric/mc1.21",
    fabric26: "fabric/mc26",
    forge8: "forge/mc1.8",
    forge12: "forge/mc1.12",
    forge16: "forge/mc1.16",
    forge20: "forge/mc1.20",
    forge: "forge/mc1.21",
    forge26: "forge/mc26",
    vanilla: "vanilla",
};

const versionFromJarName = (artifact, name) => {
    const prefix = `voxeldash-${artifact}-`;
    if (!name.startsWith(prefix) || !name.endsWith(".jar")) return null;
    const version = name.slice(prefix.length, -".jar".length);
    return version || null;
};

export const resolveVoxelDashJar = async (artifact, onLog) => {
    const moduleName = ARTIFACT_MODULE[artifact];
    if (!moduleName) throw new Error(`Unknown VoxelDash artifact: ${artifact}`);

    const moduleDir = join(config.repoRoot, "modules", moduleName);
    const candidates = [];
    for (const sub of ["target", "build/libs"]) {
        const dir = join(moduleDir, sub);
        if (!existsSync(dir)) continue;
        for (const f of readdirSync(dir)) {
            if (!f.startsWith(`voxeldash-${artifact}-`) || !f.endsWith(".jar")) continue;
            if (f.startsWith("original-") || /-(sources|dev|dev-all)\.jar$/.test(f)) continue;
            candidates.push({path: join(dir, f), mtime: statSync(join(dir, f)).mtimeMs});
        }
    }
    const newest = candidates.sort((a, b) => b.mtime - a.mtime)[0];
    if (newest) {
        onLog?.(`Using locally built ${artifact}: ${newest.path}`);
        return {path: newest.path, version: versionFromJarName(artifact, basename(newest.path))};
    }

    return downloadFromRelease(artifact, onLog);
};

const fetchRelease = async () => {
    const headers = {"User-Agent": config.userAgent, Accept: "application/vnd.github+json"};
    const pinned = process.env.VOXELDASH_RELEASE;

    if (pinned) {
        const response = await fetch(`https://api.github.com/repos/${config.repo}/releases/tags/${pinned}`, {headers});
        if (!response.ok) throw new Error(`GitHub release lookup failed: ${response.status}`);
        return response.json();
    }

    const release = await latestForChannel(getChannel());
    if (!release) throw new Error(`No published release found for ${config.repo}`);
    return release;
};

const downloadFromRelease = async (artifact, onLog) => {
    return downloadArtifactFromRelease(artifact, await fetchRelease(), onLog);
};

export const downloadArtifactFromRelease = async (artifact, release, onLog) => {
    if (!ARTIFACT_MODULE[artifact]) throw new Error(`Unknown VoxelDash artifact: ${artifact}`);
    onLog?.(`Fetching ${artifact} from release ${release.tag_name}...`);

    const asset = findServerAsset(release, artifact);
    if (!asset) throw new Error(`No ${artifact} asset found in release ${release.tag_name}`);

    const version = normalizeVersion(release.tag_name);
    const dest = join(config.paths.cache, asset.name);
    if (existsSync(dest)) {
        onLog?.(`Using cached ${asset.name}`);
        return {path: dest, version};
    }

    onLog?.(`Downloading ${asset.name}...`);
    const dl = await fetch(asset.browser_download_url, {headers: {"User-Agent": config.userAgent}, redirect: "follow"});
    if (!dl.ok) throw new Error(`Failed to download ${asset.name}: ${dl.status}`);
    await Bun.write(dest, await dl.arrayBuffer());
    return {path: dest, version};
};
