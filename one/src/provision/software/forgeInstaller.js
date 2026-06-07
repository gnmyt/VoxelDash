import {join, relative} from "node:path";
import {existsSync, readdirSync, readFileSync, rmSync} from "node:fs";

export const runJava = async (javaPath, args, cwd, log) => {
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
    if (code !== 0) throw new Error(`Installer exited with code ${code}`);
};

const findUnixArgs = (root) => {
    if (!existsSync(root)) return null;
    for (const entry of readdirSync(root, {withFileTypes: true})) {
        if (!entry.isDirectory()) continue;
        const candidate = join(root, entry.name, "unix_args.txt");
        if (existsSync(candidate)) return candidate;
    }
    return null;
};

export const installForgeLike = async (dir, {javaPath, log, librariesSubpath, marker, label}) => {
    log(`Running ${label} installer (this downloads Minecraft and libraries)...`);
    const installerPath = join(dir, "forge-installer.jar");
    await runJava(javaPath, ["-jar", installerPath, "--installServer"], dir, log);

    const argsFile = findUnixArgs(join(dir, "libraries", ...librariesSubpath));
    if (!argsFile) throw new Error(`${label} installer did not produce a unix_args.txt launch file`);
    await Bun.write(join(dir, marker), `@${relative(dir, argsFile)}\nnogui\n`);

    rmSync(installerPath, {force: true});
    rmSync(`${installerPath}.log`, {force: true});
    log(`${label} server installed.`);
};

export const argfileLaunch = (serverDir, marker) =>
    readFileSync(join(serverDir, marker), "utf8").split("\n").map((l) => l.trim()).filter(Boolean);
