import {config} from "../../config.js";
import {mojangJavaMajor} from "../mojang.js";
import {writeMinecraftProperties} from "./common.js";

const META = "https://meta.fabricmc.net/v2";
const MODRINTH = "https://api.modrinth.com/v2";

async function meta(path) {
    const response = await fetch(META + path, {
        headers: {"User-Agent": config.userAgent, Accept: "application/json"},
    });
    if (!response.ok) throw new Error(`FabricMC ${path} -> ${response.status}`);
    return response.json();
}

async function latestStable(component) {
    const list = await meta(`/versions/${component}`);
    return list.find((entry) => entry.stable) || list[0];
}

export const fabric = {
    key: "fabric",
    name: "Fabric",
    kind: "server",
    voxeldashArtifact: "fabric",
    installDir: "mods",
    minJava: 21,

    async listVersions() {
        return (await meta("/versions/game")).filter((g) => g.stable).map((g) => g.version);
    },

    async resolveServerJar(mcVersion) {
        const loader = await latestStable("loader");
        const installer = await latestStable("installer");
        return {
            url: `${META}/versions/loader/${mcVersion}/${loader.version}/${installer.version}/server/jar`,
            fileName: "server.jar",
            build: loader.version,
        };
    },

    async javaMajor(mcVersion) {
        return mojangJavaMajor(mcVersion);
    },

    async extraMods(mcVersion, onLog) {
        onLog?.("Resolving Fabric API...");
        const query =
            `game_versions=${encodeURIComponent(JSON.stringify([mcVersion]))}` +
            `&loaders=${encodeURIComponent(JSON.stringify(["fabric"]))}`;
        const response = await fetch(`${MODRINTH}/project/fabric-api/version?${query}`, {
            headers: {"User-Agent": config.userAgent},
        });
        if (!response.ok) throw new Error(`Modrinth fabric-api -> ${response.status}`);
        const versions = await response.json();
        if (!versions.length) throw new Error(`No Fabric API build available for ${mcVersion}`);
        const file = versions[0].files.find((f) => f.primary) || versions[0].files[0];
        return [{url: file.url, fileName: file.filename}];
    },

    layout: writeMinecraftProperties,

    launchArgs(jarFileName) {
        return ["-jar", jarFileName, "nogui"];
    },
};
