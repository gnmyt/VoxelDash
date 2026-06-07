import {config} from "../../config.js";
import {mojangJavaMajor} from "../mojang.js";
import {writeMinecraftProperties} from "./common.js";

const BASE = "https://api.purpurmc.org/v2/purpur";

const api = async (path) => {
    const response = await fetch(BASE + path, {
        headers: {"User-Agent": config.userAgent, Accept: "application/json"},
    });
    if (!response.ok) throw new Error(`PurpurMC ${path} -> ${response.status}`);
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

export const purpur = {
    key: "purpur",
    name: "Purpur",
    kind: "server",
    voxeldashArtifact: "spigot",
    installDir: "plugins",
    minJava: 8,

    async listVersions() {
        const versions = (await api("")).versions || [];
        return [...versions].sort((a, b) => compareVersion(b, a));
    },

    async javaMajor(mcVersion) {
        return mojangJavaMajor(mcVersion);
    },

    async resolveServerJar(mcVersion) {
        const build = await api(`/${mcVersion}/latest`);
        return {
            url: `${BASE}/${mcVersion}/${build.build}/download`,
            fileName: `purpur-${mcVersion}-${build.build}.jar`,
            build: String(build.build),
        };
    },

    layout: writeMinecraftProperties,

    launchArgs(jarFileName) {
        return ["-jar", jarFileName, "nogui"];
    },
};
