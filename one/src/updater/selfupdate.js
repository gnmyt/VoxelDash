import {join} from "node:path";
import {chmodSync, cpSync, existsSync, mkdirSync, readdirSync, renameSync, rmSync} from "node:fs";
import {config} from "../config.js";
import {getChannel} from "./settings.js";
import {findNodeAsset, isNewer, latestForChannel, normalizeVersion} from "./releases.js";

export const selfUpdateSupport = () => {
    if (config.isDesktop) return {supported: false, reason: "Self-update isn't available in the desktop app."};
    if (!config.standalone) {
        return {supported: false, reason: "Self-update is only available for the packaged binary (you're running from source)."};
    }
    return {supported: true, reason: null};
};

export const nodeStatus = async () => {
    let latest = null;
    try {
        const release = await latestForChannel(getChannel());
        latest = release ? normalizeVersion(release.tag_name) : null;
    } catch {
        latest = null;
    }
    return {
        current: config.version,
        latest,
        updatable: isNewer(latest, config.version),
        selfUpdate: selfUpdateSupport(),
    };
};

const extract = async (archivePath, staging, onLog) => {
    if (config.platform === "windows") {
        const {unzipSync} = await import("fflate");
        const bytes = new Uint8Array(await Bun.file(archivePath).arrayBuffer());
        const entries = unzipSync(bytes);
        for (const [name, data] of Object.entries(entries)) {
            if (name.endsWith("/")) continue;
            const dest = join(staging, name);
            mkdirSync(join(dest, ".."), {recursive: true});
            await Bun.write(dest, data);
        }
    } else {
        onLog("Extracting archive...");
        const proc = Bun.spawn(["tar", "-xzf", archivePath, "-C", staging], {stdout: "pipe", stderr: "pipe"});
        const code = await proc.exited;
        if (code !== 0) throw new Error(`tar exited with code ${code}`);
    }
};

export const updateNode = async (onLog = () => {}) => {
    const support = selfUpdateSupport();
    if (!support.supported) throw new Error(support.reason);

    const channel = getChannel();
    const release = await latestForChannel(channel);
    if (!release) throw new Error(`No release available on the ${channel} channel`);

    const version = normalizeVersion(release.tag_name);
    if (!isNewer(version, config.version)) throw new Error(`Already on the latest ${channel} version (${config.version}).`);

    const asset = findNodeAsset(release, config.platform);
    if (!asset) throw new Error(`No VoxelDash One ${config.platform} build found in release ${release.tag_name}`);

    const updatesDir = join(config.home, "updates");
    const staging = join(updatesDir, `stage-${version}`);
    rmSync(staging, {recursive: true, force: true});
    mkdirSync(staging, {recursive: true});

    const archivePath = join(updatesDir, asset.name);
    onLog(`Downloading ${asset.name}...`);
    const dl = await fetch(asset.browser_download_url, {headers: {"User-Agent": config.userAgent}, redirect: "follow"});
    if (!dl.ok) throw new Error(`Failed to download ${asset.name}: ${dl.status}`);
    await Bun.write(archivePath, await dl.arrayBuffer());

    await extract(archivePath, staging, onLog);

    const pkgDir = readdirSync(staging, {withFileTypes: true})
        .filter((e) => e.isDirectory())
        .map((e) => join(staging, e.name))[0];
    if (!pkgDir) throw new Error("Update archive had an unexpected layout (no package directory)");

    const binName = config.platform === "windows" ? "voxeldash-one.exe" : "voxeldash-one";
    const newBin = join(pkgDir, binName);
    const newUi = join(pkgDir, "ui");
    if (!existsSync(newBin)) throw new Error("Update archive is missing the VoxelDash One binary");
    if (!existsSync(newUi)) throw new Error("Update archive is missing the bundled UI");

    onLog("Installing new version...");
    const uiRoot = join(config.execDir, "ui");
    const stagedBin = config.execPath + ".new";
    const stagedUi = uiRoot + ".new";

    rmSync(stagedBin, {force: true});
    rmSync(stagedUi, {recursive: true, force: true});
    cpSync(newBin, stagedBin);
    cpSync(newUi, stagedUi, {recursive: true});
    chmodSync(stagedBin, 0o755);

    renameSync(stagedBin, config.execPath);
    if (existsSync(uiRoot)) {
        const backup = uiRoot + ".old";
        rmSync(backup, {recursive: true, force: true});
        renameSync(uiRoot, backup);
        renameSync(stagedUi, uiRoot);
        rmSync(backup, {recursive: true, force: true});
    } else {
        renameSync(stagedUi, uiRoot);
    }

    rmSync(staging, {recursive: true, force: true});
    rmSync(archivePath, {force: true});

    onLog(`Updated to ${version}. Restarting...`);
    setTimeout(() => process.exit(0), 750);
    return {version, restarting: true};
};
