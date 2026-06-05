import {existsSync, mkdirSync} from "node:fs";
import {dirname, join, resolve} from "node:path";
import pkg from "../package.json";

const STANDALONE = !existsSync(import.meta.dir);
const EXEC_DIR = dirname(process.execPath);

const REPO_ROOT = resolve(import.meta.dir, "..", "..");

const HOME = process.env.VOXELDASH_HOME
    ? resolve(process.env.VOXELDASH_HOME)
    : (STANDALONE ? join(EXEC_DIR, "data") : resolve(import.meta.dir, "..", "data"));

const UI_DIST = process.env.VOXELDASH_UI
    ? resolve(process.env.VOXELDASH_UI)
    : (STANDALONE ? join(EXEC_DIR, "ui", "dist") : join(REPO_ROOT, "ui", "dist"));

export const config = {
    repoRoot: REPO_ROOT,
    uiDist: UI_DIST,
    home: HOME,
    standalone: STANDALONE,
    version: pkg.version,
    platform: process.platform === "win32" ? "windows" : "linux",
    execPath: process.execPath,
    execDir: EXEC_DIR,
    isDesktop: !!process.env.VOXELDASH_DESKTOP,
    port: parseInt(process.env.PORT || "7867", 10),
    masterHost: process.env.MASTER_HOST || "127.0.0.1",
    repo: "gnmyt/VoxelDash",
    userAgent: "VoxelDash-One/0.1 (+https://github.com/gnmyt/VoxelDash)",
    paths: {
        db: join(HOME, "voxeldash.db"),
        jdks: join(HOME, "versions", "jdks"),
        cache: join(HOME, "versions", "cache"),
        servers: join(HOME, "servers"),
    },
};

for (const dir of [config.home, config.paths.jdks, config.paths.cache, config.paths.servers]) {
    mkdirSync(dir, {recursive: true});
}
