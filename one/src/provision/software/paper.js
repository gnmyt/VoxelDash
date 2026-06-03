import {config} from "../../config.js";
import {writeMinecraftProperties} from "./common.js";

const BASE = "https://fill.papermc.io/v3";

const api = async (path) => {
    const response = await fetch(BASE + path, {
        headers: {"User-Agent": config.userAgent, Accept: "application/json"},
    });
    if (!response.ok) throw new Error(`PaperMC ${path} -> ${response.status}`);
    return response.json();
};

const compareVersion = (a, b) => {
    const pa = a.split(".").map(Number);
    const pb = b.split(".").map(Number);
    for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
        const diff = (pa[i] || 0) - (pb[i] || 0);
        if (diff) return diff;
    }
    return 0;
};

export const paper = {
    key: "paper",
    name: "Paper",
    kind: "server",
    voxeldashArtifact: "spigot",
    installDir: "plugins",
    minJava: 8,

    async listVersions() {
        const versions = (await api("/projects/paper")).versions;
        if (Array.isArray(versions)) return versions;
        return Object.keys(versions)
            .sort((a, b) => compareVersion(b, a))
            .flatMap((group) => versions[group]);
    },

    async javaMajor(mcVersion) {
        try {
            const info = await api(`/projects/paper/versions/${mcVersion}`);
            return info?.version?.java?.version?.minimum || null;
        } catch {
            return null;
        }
    },

    async resolveServerJar(mcVersion) {
        const build = await api(`/projects/paper/versions/${mcVersion}/builds/latest`);
        const download = build.downloads["server:default"];
        return {
            url: download.url,
            fileName: download.name,
            sha256: download.checksums?.sha256,
            build: String(build.id)
        };
    },

    layout: writeMinecraftProperties,

    launchArgs(jarFileName) {
        return ["-jar", jarFileName, "nogui"];
    },
};
