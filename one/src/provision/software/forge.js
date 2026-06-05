import {join, relative} from "node:path";
import {existsSync, readdirSync, readFileSync, rmSync} from "node:fs";
import {config} from "../../config.js";
import {mojangJavaMajor} from "../mojang.js";
import {writeMinecraftProperties} from "./common.js";

const MAVEN = "https://maven.minecraftforge.net";
const PROMOS = "https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json";

const yearMajor = (mc) => parseInt(String(mc).split(".")[0], 10);

const LINES = [
    {test: (mc) => /^1\.21(\.\d+)?$/.test(mc), artifact: "forge", minForge: 52},
    {test: (mc) => yearMajor(mc) >= 26, artifact: "forge26", minForge: 62},
];

const lineFor = (mcVersion) => LINES.find((line) => line.test(mcVersion)) || null;

const fetchPromos = async () => {
    const response = await fetch(PROMOS, {headers: {"User-Agent": config.userAgent, Accept: "application/json"}});
    if (!response.ok) throw new Error(`Forge promotions -> ${response.status}`);
    return (await response.json()).promos || {};
};

const forgeMajor = (version) => parseInt(String(version).split(".")[0], 10) || 0;

const compareMc = (a, b) => {
    const pa = a.split(".").map(Number);
    const pb = b.split(".").map(Number);
    for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
        const diff = (pa[i] || 0) - (pb[i] || 0);
        if (diff) return diff;
    }
    return 0;
};

const resolveForgeVersion = async (mcVersion) => {
    const line = lineFor(mcVersion);
    if (!line) throw new Error(`VoxelDash has no Forge mod for Minecraft ${mcVersion} (supported: 1.21.x and 26.x).`);
    const promos = await fetchPromos();
    const version = promos[`${mcVersion}-recommended`] || promos[`${mcVersion}-latest`];
    if (!version) throw new Error(`No Forge build published for Minecraft ${mcVersion}`);
    if (forgeMajor(version) < line.minForge) {
        throw new Error(`Minecraft ${mcVersion} only has Forge ${version}; VoxelDash needs Forge ${line.minForge}+ for this line.`);
    }
    return version;
};

const findUnixArgs = (forgeRoot) => {
    if (!existsSync(forgeRoot)) return null;
    for (const entry of readdirSync(forgeRoot, {withFileTypes: true})) {
        if (!entry.isDirectory()) continue;
        const candidate = join(forgeRoot, entry.name, "unix_args.txt");
        if (existsSync(candidate)) return candidate;
    }
    return null;
};

const runJava = async (javaPath, args, cwd, log) => {
    const proc = Bun.spawn([javaPath, ...args], {cwd, stdout: "pipe", stderr: "pipe"});
    const pipe = async (stream) => {
        if (!stream) return;
        const reader = stream.getReader();
        const decoder = new TextDecoder();
        let buffer = "";
        while (true) {
            const {done, value} = await reader.read();
            if (done) break;
            buffer += decoder.decode(value, {stream: true});
            let idx;
            while ((idx = buffer.indexOf("\n")) >= 0) {
                log(buffer.slice(0, idx));
                buffer = buffer.slice(idx + 1);
            }
        }
    };
    await Promise.all([pipe(proc.stdout), pipe(proc.stderr)]);
    const code = await proc.exited;
    if (code !== 0) throw new Error(`Forge installer exited with code ${code}`);
};

export const forge = {
    key: "forge",
    name: "Forge",
    kind: "server",
    voxeldashArtifact: "forge",
    installDir: "mods",
    minJava: 21,

    installer: true,
    provisionedMarker: ".voxeldash-forge.args",

    async listVersions() {
        const promos = await fetchPromos();
        const versions = new Set();
        for (const key of Object.keys(promos)) {
            const mc = key.replace(/-(recommended|latest)$/, "");
            const line = lineFor(mc);
            if (!line) continue;
            if (forgeMajor(promos[key]) < line.minForge) continue;
            versions.add(mc);
        }
        return [...versions].sort((a, b) => compareMc(b, a));
    },

    async javaMajor(mcVersion) {
        return (await mojangJavaMajor(mcVersion)) || 21;
    },

    resolveArtifact(mcVersion) {
        return lineFor(mcVersion)?.artifact || this.voxeldashArtifact;
    },

    async resolveServerJar(mcVersion) {
        const forgeVersion = await resolveForgeVersion(mcVersion);
        const full = `${mcVersion}-${forgeVersion}`;
        return {
            url: `${MAVEN}/net/minecraftforge/forge/${full}/forge-${full}-installer.jar`,
            fileName: "forge-installer.jar",
            build: forgeVersion,
        };
    },

    async install(dir, {javaPath, log}) {
        log("Running Forge installer (this downloads Minecraft and libraries)...");
        const installerPath = join(dir, "forge-installer.jar");
        await runJava(javaPath, ["-jar", installerPath, "--installServer"], dir, log);

        const argsFile = findUnixArgs(join(dir, "libraries", "net", "minecraftforge", "forge"));
        if (!argsFile) throw new Error("Forge installer did not produce a unix_args.txt launch file");
        await Bun.write(join(dir, this.provisionedMarker), `@${relative(dir, argsFile)}\nnogui\n`);

        rmSync(installerPath, {force: true});
        rmSync(`${installerPath}.log`, {force: true});
        log("Forge server installed.");
    },

    layout: writeMinecraftProperties,

    launchArgs(_jarFileName, serverDir) {
        const descriptor = join(serverDir, this.provisionedMarker);
        return readFileSync(descriptor, "utf8").split("\n").map((l) => l.trim()).filter(Boolean);
    },
};
