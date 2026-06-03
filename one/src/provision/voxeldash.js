import {join} from "node:path";
import {existsSync, readdirSync, statSync} from "node:fs";
import {config} from "../config.js";

const ARTIFACT_MODULE = {
    spigot: "spigot",
    bungeecord: "bungeecord",
    fabric: "fabric",
    vanilla: "vanilla",
};

export async function resolveVoxelDashJar(artifact, onLog) {
    const moduleName = ARTIFACT_MODULE[artifact];
    if (!moduleName) throw new Error(`Unknown VoxelDash artifact: ${artifact}`);

    const moduleDir = join(config.repoRoot, "modules", moduleName);
    const candidates = [];
    for (const sub of ["target", "build/libs"]) {
        const dir = join(moduleDir, sub);
        if (!existsSync(dir)) continue;
        for (const f of readdirSync(dir)) {
            if (!f.startsWith(`voxeldash-${artifact}`) || !f.endsWith(".jar")) continue;
            if (f.startsWith("original-") || /-(sources|dev|dev-all)\.jar$/.test(f)) continue;
            candidates.push({path: join(dir, f), mtime: statSync(join(dir, f)).mtimeMs});
        }
    }
    const newest = candidates.sort((a, b) => b.mtime - a.mtime)[0];
    if (newest) {
        onLog?.(`Using locally built ${artifact}: ${newest.path}`);
        return newest.path;
    }

    onLog?.(`Fetching ${artifact} from release ${config.releaseTag}...`);
    return downloadFromRelease(artifact, onLog);
}

async function downloadFromRelease(artifact, onLog) {
    const url = `https://api.github.com/repos/${config.repo}/releases/tags/${config.releaseTag}`;
    const response = await fetch(url, {
        headers: {"User-Agent": config.userAgent, Accept: "application/vnd.github+json"},
    });
    if (!response.ok) throw new Error(`GitHub release lookup failed: ${response.status}`);
    const release = await response.json();

    const asset = (release.assets || []).find((a) => a.name.includes(artifact) && a.name.endsWith(".jar"));
    if (!asset) throw new Error(`No ${artifact} asset found in release ${config.releaseTag}`);

    const dest = join(config.paths.cache, asset.name);
    if (existsSync(dest)) {
        onLog?.(`Using cached ${asset.name}`);
        return dest;
    }

    onLog?.(`Downloading ${asset.name}...`);
    const dl = await fetch(asset.browser_download_url, {headers: {"User-Agent": config.userAgent}, redirect: "follow"});
    if (!dl.ok) throw new Error(`Failed to download ${asset.name}: ${dl.status}`);
    await Bun.write(dest, await dl.arrayBuffer());
    return dest;
}
