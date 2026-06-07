import {config} from "../../config.js";
import {mojangJavaMajor} from "../mojang.js";
import {writeMinecraftProperties} from "./common.js";

const BASE = "https://mcjars.app/api/v2/builds";

const compareVersion = (a, b) => {
    const pa = a.split(".").map(Number);
    const pb = b.split(".").map(Number);
    for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
        const diff = (pa[i] || 0) - (pb[i] || 0);
        if (diff) return diff;
    }
    return 0;
};

export const mcjarsSoftware = ({key, name, type}) => {
    const api = async (path) => {
        const response = await fetch(`${BASE}/${type}${path}`, {
            headers: {"User-Agent": config.userAgent, Accept: "application/json"},
        });
        if (!response.ok) throw new Error(`mcjars ${type}${path} -> ${response.status}`);
        return response.json();
    };

    return {
        key,
        name,
        kind: "server",
        voxeldashArtifact: "spigot",
        installDir: "plugins",
        minJava: 8,

        async listVersions() {
            const builds = (await api("")).builds || {};
            return Object.keys(builds)
                .filter((v) => /^\d+\.\d+(\.\d+)?$/.test(v))
                .sort((a, b) => compareVersion(b, a));
        },

        async javaMajor(mcVersion) {
            return mojangJavaMajor(mcVersion);
        },

        async resolveServerJar(mcVersion) {
            const builds = (await api(`/${mcVersion}`)).builds || [];
            const latest = builds[0];
            if (!latest?.jarUrl) throw new Error(`No ${name} build available for Minecraft ${mcVersion}`);
            return {
                url: latest.jarUrl,
                fileName: latest.jarUrl.split("/").pop() || "server.jar",
                build: latest.buildNumber != null ? String(latest.buildNumber) : null,
            };
        },

        layout: writeMinecraftProperties,

        launchArgs(jarFileName) {
            return ["-jar", jarFileName, "nogui"];
        },
    };
};
