import {config} from "../../config.js";
import {mojangJavaMajor} from "../mojang.js";
import {writeMinecraftProperties} from "./common.js";

const META = "https://meta.fabricmc.net/v2";
const LEGACY_META = "https://meta.legacyfabric.net/v2";
const LEGACY_API_MAVEN = "https://maven.legacyfabric.net/net/legacyfabric/legacy-fabric-api";
const MODRINTH = "https://api.modrinth.com/v2";

const LEGACY_API_MODS_1_12 = [
    "legacy-fabric-api-base/1.0.0+b7f5520f52",
    "legacy-fabric-api-base-common/1.1.0+7c545fdbe692",
    "legacy-fabric-lifecycle-events-v1/1.0.1+1.12.2+095a5759d852",
    "legacy-fabric-lifecycle-events-v1-common/1.0.1+095a57597c92",
];

const LEGACY_API_MODS_1_8 = [
    "legacy-fabric-api-base/1.1.0+1.8.9+7c545fdbe239",
    "legacy-fabric-api-base-common/1.1.0+7c545fdbe692",
    "legacy-fabric-lifecycle-events-v1/1.0.1+1.8.9+095a5759f739",
    "legacy-fabric-lifecycle-events-v1-common/1.0.1+095a57597c92",
];

const LINES = [
    {test: (mc) => mc === "1.8.9", artifact: "fabric8", java: 8, legacy: true, mc: "1.8.9", legacyApiMods: LEGACY_API_MODS_1_8},
    {test: (mc) => mc === "1.12.2", artifact: "fabric12", java: 8, legacy: true, mc: "1.12.2", legacyApiMods: LEGACY_API_MODS_1_12},
    {test: (mc) => mc === "1.14.4", artifact: "fabric14", java: 8},
    {test: (mc) => mc === "1.16.5", artifact: "fabric16", java: 8},
    {test: (mc) => mc === "1.20.1", artifact: "fabric20", java: 17},
    {test: (mc) => /^1\.21(\.\d+)?$/.test(mc), artifact: "fabric", java: 21},
];

const SUPPORTED_MC = "1.8.9, 1.12.2, 1.14.4, 1.16.5, 1.20.1 and 1.21.x";

const lineFor = (mcVersion) => LINES.find((line) => line.test(mcVersion)) || null;

const meta = async (base, path) => {
    const response = await fetch(base + path, {
        headers: {"User-Agent": config.userAgent, Accept: "application/json"},
    });
    if (!response.ok) throw new Error(`Fabric meta ${path} -> ${response.status}`);
    return response.json();
};

const latestStable = async (base, component) => {
    const list = await meta(base, `/versions/${component}`);
    return list.find((entry) => entry.stable) || list[0];
};

export const fabric = {
    key: "fabric",
    name: "Fabric",
    kind: "server",
    voxeldashArtifact: "fabric",
    installDir: "mods",
    minJava: 8,

    async listVersions() {
        const games = (await meta(META, "/versions/game")).filter((g) => g.stable);
        const modern = games.map((g) => g.version).filter((v) => lineFor(v) && !lineFor(v).legacy);
        const legacy = LINES.filter((l) => l.legacy).map((l) => l.mc);
        return [...legacy, ...modern];
    },

    resolveArtifact(mcVersion) {
        return lineFor(mcVersion)?.artifact || this.voxeldashArtifact;
    },

    async resolveServerJar(mcVersion) {
        const line = lineFor(mcVersion);
        if (!line) {
            throw new Error(`VoxelDash has no Fabric mod for Minecraft ${mcVersion} (supported: ${SUPPORTED_MC}).`);
        }
        const base = line.legacy ? LEGACY_META : META;
        const loader = await latestStable(base, "loader");
        const installer = await latestStable(base, "installer");
        const loaderVersion = loader.loader ? loader.loader.version : loader.version;
        return {
            url: `${base}/versions/loader/${mcVersion}/${loaderVersion}/${installer.version}/server/jar`,
            fileName: "server.jar",
            build: loaderVersion,
        };
    },

    async javaMajor(mcVersion) {
        return (await mojangJavaMajor(mcVersion)) || lineFor(mcVersion)?.java || 21;
    },

    async extraMods(mcVersion, onLog) {
        const line = lineFor(mcVersion);

        if (line && line.legacy) {
            onLog?.("Resolving Legacy Fabric API modules...");
            return (line.legacyApiMods || []).map((coord) => {
                const [artifact, version] = coord.split("/");
                const fileName = `${artifact}-${version}.jar`;
                return {url: `${LEGACY_API_MAVEN}/${artifact}/${version}/${fileName}`, fileName};
            });
        }

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
