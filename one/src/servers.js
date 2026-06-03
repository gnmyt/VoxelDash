import {join} from "node:path";
import {copyFileSync, existsSync, mkdirSync, rmSync} from "node:fs";
import {db} from "./db.js";
import {config} from "./config.js";
import {findFreePorts, randomId, randomToken} from "./util.js";
import {getSoftware, listSoftware} from "./provision/software/index.js";
import {detectJarJavaMajor, ensureJre, requiredMajorForMc} from "./provision/jre.js";
import {resolveVoxelDashJar} from "./provision/voxeldash.js";
import {launchServer, stopServer} from "./provision/launcher.js";
import {clearLog, effectiveStatus, getLog, logProgress, processes} from "./runtime.js";
import {registry} from "./tunnel/registry.js";

function serverDirFor(id) {
    return join(config.paths.servers, id);
}

function serialize(row) {
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
}

function getServer(id) {
    return db.query("SELECT * FROM servers WHERE id = ?").get(id);
}

function setStatus(id, status) {
    db.query("UPDATE servers SET status = ? WHERE id = ?").run(status, id);
}

async function downloadTo(url, dest, onLog) {
    onLog?.(`Downloading ${url.split("/").pop()}...`);
    const response = await fetch(url, {headers: {"User-Agent": config.userAgent}, redirect: "follow"});
    if (!response.ok) throw new Error(`Download failed (${response.status}): ${url}`);
    await Bun.write(dest, await response.arrayBuffer());
}

async function provision(server) {
    const log = (line) => logProgress(server.id, line);
    const dir = serverDirFor(server.id);
    mkdirSync(dir, {recursive: true});

    try {
        const software = getSoftware(server.software);
        if (!software) throw new Error(`Unsupported software: ${server.software}`);

        const serverJar = join(dir, "server.jar");

        if (software.loaderJar) {
            log(`Installing ${software.name} loader...`);
            copyFileSync(await resolveVoxelDashJar(software.voxeldashArtifact, log), serverJar);
            log("Installed VoxelDash loader.");
        } else {
            log(`Resolving ${software.name} ${server.mc_version}...`);
            const jar = await software.resolveServerJar(server.mc_version);
            await downloadTo(jar.url, serverJar, log);
            db.query("UPDATE servers SET build = ? WHERE id = ?").run(jar.build || null, server.id);

            const installDir = software.installDir ? join(dir, software.installDir) : dir;
            mkdirSync(installDir, {recursive: true});
            copyFileSync(await resolveVoxelDashJar(software.voxeldashArtifact, log), join(installDir, "voxeldash.jar"));
            log("Installed VoxelDash.");

            if (software.extraMods) {
                for (const extra of await software.extraMods(server.mc_version, log)) {
                    await downloadTo(extra.url, join(installDir, extra.fileName), log);
                }
            }
        }

        let major = software.javaMajor ? await software.javaMajor(server.mc_version) : null;
        if (!major && !software.loaderJar) major = detectJarJavaMajor(serverJar);
        if (!major) major = requiredMajorForMc(server.mc_version);
        if (software.minJava && major < software.minJava) {
            throw new Error(`Minecraft ${server.mc_version} runs on Java ${major}, but VoxelDash requires Java ${software.minJava}+. Choose a newer Minecraft version.`);
        }
        log(`Required Java: ${major}`);
        db.query("UPDATE servers SET java_major = ? WHERE id = ?").run(major, server.id);
        const javaPath = await ensureJre(major, log);

        await software.layout(dir, {gamePort: server.game_port, name: server.name});

        setStatus(server.id, "offline");
        log("Provisioning complete. Starting server...");
        launchServer(getServer(server.id), javaPath, dir);
    } catch (err) {
        logProgress(server.id, `ERROR: ${err.message}`);
        setStatus(server.id, "install_failed");
    }
}

export function mountServerRoutes(app, requireMasterAuth) {
    app.get("/master/software", requireMasterAuth, (req, res) => {
        res.json({software: listSoftware()});
    });

    app.get("/master/software/:key/versions", requireMasterAuth, async (req, res) => {
        const software = getSoftware(req.params.key);
        if (!software) return res.status(404).json({error: "Unknown software"});
        try {
            res.json({versions: await software.listVersions()});
        } catch (err) {
            res.status(502).json({error: err.message});
        }
    });

    app.get("/master/servers", requireMasterAuth, (req, res) => {
        res.json({servers: db.query("SELECT * FROM servers ORDER BY created_at DESC").all().map(serialize)});
    });

    app.post("/master/servers", requireMasterAuth, async (req, res) => {
        const {name, software: softwareKey, mcVersion, memoryMb} = req.body || {};
        const software = getSoftware(softwareKey);
        const catalog = listSoftware().find((s) => s.key === softwareKey);

        if (!name || !softwareKey) return res.status(400).json({error: "name and software are required"});
        if (!software || !catalog?.available) return res.status(400).json({error: "That software is not available yet"});
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
        ).run(id, name, softwareKey, mcVersion, gamePort, apiPort, randomToken(24), memoryMb || 2048);

        clearLog(id);
        logProgress(id, `Creating ${software.name} server "${name}" (${mcVersion})`);
        provision(getServer(id));

        res.status(201).json({server: serialize(getServer(id))});
    });

    app.get("/master/servers/:id", requireMasterAuth, (req, res) => {
        const row = getServer(req.params.id);
        if (!row) return res.status(404).json({error: "Not found"});
        res.json({server: serialize(row), log: getLog(row.id)});
    });

    app.post("/master/servers/:id/start", requireMasterAuth, async (req, res) => {
        const row = getServer(req.params.id);
        if (!row) return res.status(404).json({error: "Not found"});
        if (processes.has(row.id) || registry.isOnline(row.id)) {
            return res.status(409).json({error: "Server is already running"});
        }
        const dir = serverDirFor(row.id);
        if (!existsSync(join(dir, "server.jar"))) return res.status(409).json({error: "Server is not provisioned"});
        try {
            const javaPath = await ensureJre(row.java_major || requiredMajorForMc(row.mc_version));
            logProgress(row.id, "Starting server...");
            launchServer(row, javaPath, dir);
            res.json({server: serialize(getServer(row.id))});
        } catch (err) {
            res.status(500).json({error: err.message});
        }
    });

    app.post("/master/servers/:id/stop", requireMasterAuth, (req, res) => {
        const row = getServer(req.params.id);
        if (!row) return res.status(404).json({error: "Not found"});
        if (!stopServer(row)) return res.status(409).json({error: "Server is not running in this session"});
        res.json({ok: true});
    });

    app.delete("/master/servers/:id", requireMasterAuth, (req, res) => {
        const row = getServer(req.params.id);
        if (!row) return res.status(404).json({error: "Not found"});
        stopServer(row);
        registry.detach(row.id);
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
}
