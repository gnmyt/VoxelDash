import {join} from "node:path";
import {totalmem} from "node:os";
import {copyFileSync, existsSync, mkdirSync, readdirSync, rmSync} from "node:fs";
import {db} from "./db.js";
import {config} from "./config.js";
import {findFreePorts, randomId, randomToken} from "./util.js";
import {getSoftware, listSoftware} from "./provision/software/index.js";
import {availableJavaMajors, detectJarJavaMajor, ensureJre, requiredMajorForMc} from "./provision/jre.js";
import {resolveVoxelDashJar} from "./provision/voxeldash.js";
import {launchServer, stopServer} from "./provision/launcher.js";
import {clearLog, effectiveStatus, getLog, logProgress, processes} from "./runtime.js";
import {registry} from "./tunnel/registry.js";
import {requireServerAccess} from "./auth.js";
import {LEVEL, listAccessibleServerIds} from "./permissions.js";
import {removeTunnelsForServer} from "./playit/manager.js";

const serverDirFor = (id) => {
    return join(config.paths.servers, id);
};

const MEMORY_MINIMUM_MB = 1024;
const MEMORY_STEP_MB = 512;

const totalMemoryMb = () => Math.floor(totalmem() / (1024 * 1024));

const clampMemoryMb = (memoryMb) => {
    const snapped = Math.round(memoryMb / MEMORY_STEP_MB) * MEMORY_STEP_MB;
    return Math.min(Math.max(snapped, MEMORY_MINIMUM_MB), Math.max(totalMemoryMb(), MEMORY_MINIMUM_MB));
};

const serialize = (row) => {
    return {
        id: row.id,
        name: row.name,
        software: row.software,
        mcVersion: row.mc_version,
        build: row.build,
        gamePort: row.game_port,
        javaMajor: row.java_major,
        memoryMb: row.memory_mb,
        status: effectiveStatus(row),
        createdAt: row.created_at,
    };
};

const getServer = (id) => {
    return db.query("SELECT * FROM servers WHERE id = ?").get(id);
};

const setStatus = (id, status) => {
    db.query("UPDATE servers SET status = ? WHERE id = ?").run(status, id);
};

const downloadTo = async (url, dest, onLog) => {
    onLog?.(`Downloading ${url.split("/").pop()}...`);
    const response = await fetch(url, {headers: {"User-Agent": config.userAgent}, redirect: "follow"});
    if (!response.ok) throw new Error(`Download failed (${response.status}): ${url}`);
    await Bun.write(dest, await response.arrayBuffer());
};

const removeStaleVariants = (dirPath, fileName) => {
    const prefix = fileName.replace(/\d.*$/, "");
    if (prefix.length < 4 || prefix === fileName) return;
    for (const existing of readdirSync(dirPath)) {
        if (existing !== fileName && existing.startsWith(prefix)) rmSync(join(dirPath, existing), {force: true});
    }
};

const installArtifacts = async (server, mcVersion, dir, log) => {
    const software = getSoftware(server.software);
    if (!software) throw new Error(`Unsupported software: ${server.software}`);

    const serverJar = join(dir, "server.jar");
    const artifact = software.resolveArtifact ? software.resolveArtifact(mcVersion) : software.voxeldashArtifact;

    if (software.loaderJar) {
        log(`Installing ${software.name} loader...`);
        const loader = await resolveVoxelDashJar(artifact, log);
        copyFileSync(loader.path, serverJar);
        db.query("UPDATE servers SET voxeldash_version = ? WHERE id = ?").run(loader.version, server.id);
        log("Installed VoxelDash loader.");
    } else {
        log(`Resolving ${software.name} ${mcVersion}...`);
        const jar = await software.resolveServerJar(mcVersion);
        await downloadTo(jar.url, join(dir, software.installer ? "forge-installer.jar" : "server.jar"), log);
        db.query("UPDATE servers SET build = ? WHERE id = ?").run(jar.build || null, server.id);

        const installDir = software.installDir ? join(dir, software.installDir) : dir;
        mkdirSync(installDir, {recursive: true});
        const vd = await resolveVoxelDashJar(artifact, log);
        copyFileSync(vd.path, join(installDir, "voxeldash.jar"));
        db.query("UPDATE servers SET voxeldash_version = ? WHERE id = ?").run(vd.version, server.id);
        log("Installed VoxelDash.");

        if (software.extraMods) {
            const extras = await software.extraMods(mcVersion, log);
            for (const extra of extras) removeStaleVariants(installDir, extra.fileName);
            for (const extra of extras) {
                await downloadTo(extra.url, join(installDir, extra.fileName), log);
            }
        }
    }

    let major = software.javaMajor ? await software.javaMajor(mcVersion) : null;
    if (!major && !software.loaderJar && !software.installer) major = detectJarJavaMajor(serverJar);
    if (!major) major = requiredMajorForMc(mcVersion);
    if (software.minJava && major < software.minJava) {
        throw new Error(`Minecraft ${mcVersion} runs on Java ${major}, but VoxelDash requires Java ${software.minJava}+. Choose a newer Minecraft version.`);
    }
    log(`Required Java: ${major}`);
    db.query("UPDATE servers SET java_major = ? WHERE id = ?").run(major, server.id);
    const javaPath = await ensureJre(major, log);

    if (software.install) await software.install(dir, {mcVersion, javaPath, log});
    return javaPath;
};

const provision = async (server) => {
    const log = (line) => logProgress(server.id, line);
    const dir = serverDirFor(server.id);
    mkdirSync(dir, {recursive: true});

    try {
        const javaPath = await installArtifacts(server, server.mc_version, dir, log);
        const software = getSoftware(server.software);

        await software.layout(dir, {gamePort: server.game_port, name: server.name});

        setStatus(server.id, "offline");
        log("Provisioning complete. Starting server...");
        launchServer(getServer(server.id), javaPath, dir);
    } catch (err) {
        logProgress(server.id, `ERROR: ${err.message}`);
        setStatus(server.id, "install_failed");
    }
};

const changeVersion = async (server, mcVersion) => {
    const log = (line) => logProgress(server.id, line);
    const dir = serverDirFor(server.id);

    try {
        const software = getSoftware(server.software);
        if (software.loaderJar) rmSync(join(dir, "minecraft_server.jar"), {force: true});

        await installArtifacts(server, mcVersion, dir, log);
        db.query("UPDATE servers SET mc_version = ? WHERE id = ?").run(mcVersion, server.id);

        setStatus(server.id, "offline");
        log(`Version change complete. ${software.name} is now on ${mcVersion}. Start the server to apply.`);
    } catch (err) {
        logProgress(server.id, `ERROR: ${err.message}`);
        setStatus(server.id, "install_failed");
    }
};

export const mountServerRoutes = (app, requireFeature) => {
    const canView = requireFeature("Servers", LEVEL.READ);
    const canManage = requireFeature("Servers", LEVEL.FULL);

    app.get("/master/system", canView, async (req, res) => {
        const javaMajors = await availableJavaMajors().catch(() => []);
        res.json({totalMemoryMb: totalMemoryMb(), javaMajors});
    });

    app.get("/master/software", canView, (req, res) => {
        res.json({software: listSoftware()});
    });

    app.get("/master/software/:key/versions", canView, async (req, res) => {
        const software = getSoftware(req.params.key);
        if (!software) return res.status(404).json({error: "Unknown software"});
        try {
            res.json({versions: await software.listVersions()});
        } catch (err) {
            res.status(502).json({error: err.message});
        }
    });

    app.get("/master/servers", canView, (req, res) => {
        const allowed = listAccessibleServerIds(req.user.id);
        const rows = db.query("SELECT * FROM servers ORDER BY created_at DESC").all();
        const visible = allowed === null ? rows : rows.filter((row) => allowed.includes(row.id));
        res.json({servers: visible.map(serialize)});
    });

    app.post("/master/servers", canManage, async (req, res) => {
        const {name, software: softwareKey, mcVersion, memoryMb} = req.body || {};
        const software = getSoftware(softwareKey);

        if (!name || !softwareKey) return res.status(400).json({error: "name and software are required"});
        if (!mcVersion) return res.status(400).json({error: "mcVersion is required"});

        const requiredJava = (software.javaMajor ? await software.javaMajor(mcVersion) : 0) || requiredMajorForMc(mcVersion);
        if (software.minJava && requiredJava < software.minJava) {
            return res.status(400).json({
                error: `Minecraft ${mcVersion} runs on Java ${requiredJava}, but the VoxelDash ${software.name} build needs Java ${software.minJava}+. Choose a newer Minecraft version.`,
            });
        }

        const id = randomId();
        const [gamePort, apiPort] = await findFreePorts(2);

        db.query(
            `INSERT INTO servers (id, name, software, mc_version, game_port, api_port, api_token, memory_mb, status)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'installing')`
        ).run(id, name, softwareKey, mcVersion, gamePort, apiPort, randomToken(24), clampMemoryMb(memoryMb || 2048));

        clearLog(id);
        logProgress(id, `Creating ${software.name} server "${name}" (${mcVersion})`);
        provision(getServer(id));

        res.status(201).json({server: serialize(getServer(id))});
    });

    app.get("/master/servers/:id", canView, requireServerAccess, (req, res) => {
        const row = getServer(req.params.id);
        if (!row) return res.status(404).json({error: "Not found"});
        res.json({server: serialize(row), log: getLog(row.id)});
    });

    app.patch("/master/servers/:id", canManage, requireServerAccess, async (req, res) => {
        const row = getServer(req.params.id);
        if (!row) return res.status(404).json({error: "Not found"});
        const {memoryMb, javaMajor} = req.body || {};
        if (memoryMb === undefined && javaMajor === undefined) {
            return res.status(400).json({error: "memoryMb or javaMajor is required"});
        }
        if (memoryMb !== undefined) {
            if (typeof memoryMb !== "number" || !Number.isFinite(memoryMb)) {
                return res.status(400).json({error: "memoryMb must be a number"});
            }
            db.query("UPDATE servers SET memory_mb = ? WHERE id = ?").run(clampMemoryMb(memoryMb), row.id);
        }
        if (javaMajor !== undefined) {
            if (!Number.isInteger(javaMajor) || javaMajor < 8) {
                return res.status(400).json({error: "javaMajor must be an integer of 8 or higher"});
            }
            const known = await availableJavaMajors().catch(() => null);
            if (known && !known.includes(javaMajor)) {
                return res.status(400).json({error: `No Temurin ${javaMajor} release is available`});
            }
            const software = getSoftware(row.software);
            if (software?.minJava && javaMajor < software.minJava) {
                return res.status(400).json({error: `VoxelDash ${software.name} needs Java ${software.minJava}+`});
            }
            db.query("UPDATE servers SET java_major = ? WHERE id = ?").run(javaMajor, row.id);
        }
        res.json({server: serialize(getServer(row.id))});
    });

    app.post("/master/servers/:id/version", canManage, requireServerAccess, async (req, res) => {
        const row = getServer(req.params.id);
        if (!row) return res.status(404).json({error: "Not found"});
        if (processes.has(row.id) || registry.isOnline(row.id)) {
            return res.status(409).json({error: "Stop the server before changing its version"});
        }
        if (row.status === "installing") return res.status(409).json({error: "Server is already installing"});

        const {mcVersion} = req.body || {};
        if (!mcVersion || typeof mcVersion !== "string") return res.status(400).json({error: "mcVersion is required"});

        const software = getSoftware(row.software);
        const requiredJava = (software.javaMajor ? await software.javaMajor(mcVersion) : 0) || requiredMajorForMc(mcVersion);
        if (software.minJava && requiredJava < software.minJava) {
            return res.status(400).json({
                error: `Minecraft ${mcVersion} runs on Java ${requiredJava}, but the VoxelDash ${software.name} build needs Java ${software.minJava}+. Choose a newer Minecraft version.`,
            });
        }

        setStatus(row.id, "installing");
        clearLog(row.id);
        logProgress(row.id, `Changing ${software.name} server "${row.name}" from ${row.mc_version} to ${mcVersion}`);
        changeVersion(row, mcVersion);

        res.json({server: serialize(getServer(row.id))});
    });

    app.post("/master/servers/:id/start", canManage, requireServerAccess, async (req, res) => {
        const row = getServer(req.params.id);
        if (!row) return res.status(404).json({error: "Not found"});
        if (processes.has(row.id) || registry.isOnline(row.id)) {
            return res.status(409).json({error: "Server is already running"});
        }
        const dir = serverDirFor(row.id);
        const marker = getSoftware(row.software)?.provisionedMarker || "server.jar";
        if (!existsSync(join(dir, marker))) return res.status(409).json({error: "Server is not provisioned"});
        try {
            const javaPath = await ensureJre(row.java_major || requiredMajorForMc(row.mc_version));
            logProgress(row.id, "Starting server...");
            launchServer(row, javaPath, dir);
            res.json({server: serialize(getServer(row.id))});
        } catch (err) {
            res.status(500).json({error: err.message});
        }
    });

    app.post("/master/servers/:id/stop", canManage, requireServerAccess, (req, res) => {
        const row = getServer(req.params.id);
        if (!row) return res.status(404).json({error: "Not found"});
        if (!stopServer(row)) return res.status(409).json({error: "Server is not running in this session"});
        res.json({ok: true});
    });

    app.delete("/master/servers/:id", canManage, requireServerAccess, (req, res) => {
        const row = getServer(req.params.id);
        if (!row) return res.status(404).json({error: "Not found"});
        stopServer(row);
        registry.detach(row.id);
        removeTunnelsForServer(row.id).catch(() => {});
        setTimeout(() => {
            try {
                rmSync(serverDirFor(row.id), {recursive: true, force: true});
            } catch {
            }
        }, 1000);
        db.query("DELETE FROM servers WHERE id = ?").run(row.id);
        clearLog(row.id);
        res.json({ok: true});
    });
};
