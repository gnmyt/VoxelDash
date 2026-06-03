import {join} from "node:path";
import {existsSync, mkdirSync, readdirSync, rmSync} from "node:fs";
import {config} from "../config.js";
import {readZipEntry} from "./zip.js";

export function detectJarJavaMajor(jarPath) {
    try {
        const manifest = readZipEntry(jarPath, "META-INF/MANIFEST.MF");
        const match = manifest?.toString("utf8").match(/Main-Class:\s*([^\r\n]+)/);
        if (!match) return null;
        const bytes = readZipEntry(jarPath, match[1].trim().replaceAll(".", "/") + ".class");
        if (!bytes || bytes.length < 8) return null;
        const javaMajor = bytes.readUInt16BE(6) - 44;
        return javaMajor >= 8 ? javaMajor : null;
    } catch {
        return null;
    }
}

export function requiredMajorForMc(mcVersion) {
    const m = String(mcVersion || "").match(/^1\.(\d+)(?:\.(\d+))?/);
    if (!m) return 25;
    const minor = parseInt(m[1], 10);
    const patch = parseInt(m[2] || "0", 10);
    if (minor >= 21) return 21;
    if (minor === 20) return patch >= 5 ? 21 : 17;
    if (minor >= 18) return 17;
    if (minor === 17) return 16;
    return 8;
}

function findJavaBinary(dir) {
    if (!existsSync(dir)) return null;
    const stack = [dir];
    let depth = 0;
    while (stack.length && depth++ < 5000) {
        const current = stack.pop();
        let entries;
        try {
            entries = readdirSync(current, {withFileTypes: true});
        } catch {
            continue;
        }
        for (const entry of entries) {
            const full = join(current, entry.name);
            if (entry.isDirectory()) stack.push(full);
            else if (entry.name === "java" && current.endsWith("bin")) return full;
        }
    }
    return null;
}

export async function ensureJre(major, onLog) {
    const dir = join(config.paths.jdks, String(major));
    const existing = findJavaBinary(dir);
    if (existing) {
        onLog?.(`Using cached JRE ${major}`);
        return existing;
    }

    mkdirSync(dir, {recursive: true});
    const arch = process.arch === "arm64" ? "aarch64" : "x64";
    const os = process.platform === "darwin" ? "mac" : "linux";

    const tarPath = join(dir, "jre.tar.gz");
    let lastStatus = 0;
    let downloaded = false;
    for (const image of ["jre", "jdk"]) {
        const url = `https://api.adoptium.net/v3/binary/latest/${major}/ga/${os}/${arch}/${image}/hotspot/normal/eclipse`;
        onLog?.(`Downloading Temurin ${image.toUpperCase()} ${major} (${os}/${arch})...`);
        const response = await fetch(url, {headers: {"User-Agent": config.userAgent}, redirect: "follow"});
        if (response.ok) {
            await Bun.write(tarPath, await response.arrayBuffer());
            downloaded = true;
            break;
        }
        lastStatus = response.status;
    }
    if (!downloaded) {
        rmSync(dir, {recursive: true, force: true});
        throw new Error(`No Temurin ${major} build available for ${os}/${arch} (Adoptium ${lastStatus})`);
    }

    onLog?.("Extracting JRE...");
    const proc = Bun.spawnSync(["tar", "-xzf", tarPath, "-C", dir]);
    if (proc.exitCode !== 0) throw new Error("Failed to extract JRE: " + (proc.stderr?.toString() || "unknown error"));
    rmSync(tarPath, {force: true});

    const java = findJavaBinary(dir);
    if (!java) throw new Error("java binary not found after extracting JRE");
    Bun.spawnSync(["chmod", "+x", java]);
    onLog?.(`JRE ${major} ready`);
    return java;
}
