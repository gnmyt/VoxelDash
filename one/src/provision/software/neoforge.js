import {config} from "../../config.js";
import {mojangJavaMajor} from "../mojang.js";
import {writeMinecraftProperties} from "./common.js";
import {argfileLaunch, installForgeLike} from "./forgeInstaller.js";

const MAVEN = "https://maven.neoforged.net/releases/net/neoforged/neoforge";
const VERSIONS = "https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge";

const yearMajor = (mc) => parseInt(String(mc).split(".")[0], 10);

const lineFor = (mc) => {
    if (/^1\.20\.[2-6]$/.test(mc)) return "neoforge20";
    if (/^1\.21(\.\d+)?$/.test(mc)) return "neoforge";
    if (yearMajor(mc) >= 26) return "neoforge26";
    return null;
};

const javaFloor = (mc) => {
    const line = lineFor(mc);
    if (line === "neoforge26") return 25;
    if (line === "neoforge20") return 17;
    return 21;
};

const mcForVersion = (raw) => {
    const core = raw.replace(/-beta$/, "");
    const parts = core.split(".");
    if (!/^\d+$/.test(parts[0])) return null;
    const major = parseInt(parts[0], 10);
    if (major >= 26) {
        const [a, b, c] = parts;
        return c && c !== "0" ? `${a}.${b}.${c}` : `${a}.${b}`;
    }
    const [a, b] = parts;
    return b && b !== "0" ? `1.${a}.${b}` : `1.${a}`;
};

const compareMc = (a, b) => {
    const pa = a.split(".").map(Number);
    const pb = b.split(".").map(Number);
    for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
        const diff = (pa[i] || 0) - (pb[i] || 0);
        if (diff) return diff;
    }
    return 0;
};

const fetchVersions = async () => {
    const response = await fetch(VERSIONS, {headers: {"User-Agent": config.userAgent, Accept: "application/json"}});
    if (!response.ok) throw new Error(`NeoForge versions -> ${response.status}`);
    return (await response.json()).versions || [];
};

const resolveNeoVersion = async (mcVersion) => {
    const all = await fetchVersions();
    const matching = all.filter((v) => mcForVersion(v) === mcVersion);
    if (!matching.length) throw new Error(`No NeoForge build published for Minecraft ${mcVersion}`);
    const stable = matching.filter((v) => !v.endsWith("-beta"));
    return (stable.length ? stable : matching).pop();
};

export const neoforge = {
    key: "neoforge",
    name: "NeoForge",
    kind: "server",
    voxeldashArtifact: "neoforge",
    installDir: "mods",
    minJava: 17,

    installer: true,
    provisionedMarker: ".voxeldash-neoforge.args",

    async listVersions() {
        const versions = new Set();
        for (const raw of await fetchVersions()) {
            const mc = mcForVersion(raw);
            if (mc && lineFor(mc)) versions.add(mc);
        }
        return [...versions].sort((a, b) => compareMc(b, a));
    },

    async javaMajor(mcVersion) {
        const floor = javaFloor(mcVersion);
        return Math.max(floor, (await mojangJavaMajor(mcVersion)) || floor);
    },

    resolveArtifact(mcVersion) {
        return lineFor(mcVersion) || this.voxeldashArtifact;
    },

    async resolveServerJar(mcVersion) {
        const version = await resolveNeoVersion(mcVersion);
        return {
            url: `${MAVEN}/${version}/neoforge-${version}-installer.jar`,
            fileName: "forge-installer.jar",
            build: version,
        };
    },

    async install(dir, {javaPath, log}) {
        await installForgeLike(dir, {
            javaPath, log,
            librariesSubpath: ["net", "neoforged", "neoforge"],
            marker: this.provisionedMarker,
            label: "NeoForge",
        });
    },

    layout: writeMinecraftProperties,

    launchArgs(_jarFileName, serverDir) {
        return argfileLaunch(serverDir, this.provisionedMarker);
    },
};
