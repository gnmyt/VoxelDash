import {config} from "../config.js";

const MANIFEST = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";

let manifestCache = null;

export async function mojangManifest() {
    if (manifestCache) return manifestCache;
    const response = await fetch(MANIFEST, {headers: {"User-Agent": config.userAgent}});
    if (!response.ok) throw new Error(`Mojang manifest -> ${response.status}`);
    return (manifestCache = await response.json());
}

export async function mojangReleases() {
    const data = await mojangManifest();
    return (data.versions || []).filter((v) => v.type === "release").map((v) => v.id);
}

export async function mojangJavaMajor(mcVersion) {
    try {
        const data = await mojangManifest();
        const entry = (data.versions || []).find((v) => v.id === mcVersion);
        if (!entry) return null;
        const response = await fetch(entry.url, {headers: {"User-Agent": config.userAgent}});
        if (!response.ok) return null;
        const info = await response.json();
        return info?.javaVersion?.majorVersion || null;
    } catch {
        return null;
    }
}
