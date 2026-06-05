import {
    copyFileSync,
    existsSync,
    mkdirSync,
    readdirSync,
    renameSync,
    rmSync,
    statSync,
    unlinkSync,
    writeFileSync,
} from "node:fs";
import {appendFile, readFile, writeFile} from "node:fs/promises";
import {basename, dirname, join, resolve} from "node:path";
import {randomUUID} from "node:crypto";
import {fail, message, normalizePath, parseBody, ok, streamFile, withinRoot} from "./fs-helpers.js";

const listFiles = (req, res, {root}) => {
    const dir = normalizePath(root, req.query.path);
    if (!existsSync(dir) || !statSync(dir).isDirectory()) return fail(res, "The directory does not exist");
    const entries = [];
    for (const name of readdirSync(dir)) {
        if (name === "voxeldash.db") continue;
        let st;
        try {
            st = statSync(join(dir, name));
        } catch {
            continue;
        }
        entries.push({name, is_folder: !st.isFile(), last_modified: Math.trunc(st.mtimeMs), size: st.size});
    }
    return ok(res, {files: entries, count: entries.length});
};

const deleteFile = (req, res, {root}) => {
    const body = parseBody(req);
    if (!body.path) return fail(res, "Missing required field: path");
    const file = normalizePath(root, body.path);
    if (!existsSync(file) || statSync(file).isDirectory()) return fail(res, "The file does not exist");
    unlinkSync(file);
    return message(res, "File deleted");
};

const downloadFile = (req, res, {root}) => {
    const file = normalizePath(root, req.query.path);
    if (!existsSync(file) || statSync(file).isDirectory()) return fail(res, "The file does not exist");
    if (basename(file) === "voxeldash.db") return fail(res, "You are not allowed to download the database file");
    return streamFile(res, file);
};

const renameFile = (req, res, {root}) => {
    const body = parseBody(req);
    if (!body.path || !body.newName) return fail(res, "Missing required fields");
    const file = normalizePath(root, body.path);
    if (!existsSync(file) || statSync(file).isDirectory()) return fail(res, "The file does not exist");
    const target = resolve(dirname(file), body.newName);
    if (!withinRoot(root, target)) return fail(res, "Invalid destination");
    renameSync(file, target);
    return message(res, "File renamed");
};

const updateContent = (req, res, {root}) => {
    const body = parseBody(req);
    if (!body.path || body.content === undefined) return fail(res, "Missing required fields");
    const file = normalizePath(root, body.path);
    if (!existsSync(file) || statSync(file).isDirectory()) return fail(res, "The file does not exist");
    writeFileSync(file, body.content);
    return message(res, "File content updated");
};

const uploadInit = (req, res, {root}) => {
    const uuid = randomUUID();
    mkdirSync(join(root, "uploads", uuid), {recursive: true});
    return ok(res, {uuid, message: "Upload initialized"});
};

const uploadChunk = async (req, res, {root, params}) => {
    if (!/^[0-9a-f-]{36}$/i.test(params.uuid) || !/^\d+$/.test(params.id)) return fail(res, "Invalid upload session");
    const tempDir = join(root, "uploads", params.uuid);
    if (!existsSync(tempDir)) return fail(res, "Upload session does not exist");
    await writeFile(join(tempDir, `chunk_${params.id}`), req.body || Buffer.alloc(0));
    return message(res, `Chunk ${params.id} uploaded`);
};

const uploadStop = async (req, res, {root}) => {
    const body = parseBody(req);
    if (!body.uuid || !body.destinationPath) return fail(res, "Missing required fields");
    const tempDir = join(root, "uploads", body.uuid);
    if (!existsSync(tempDir)) return fail(res, "Upload session does not exist");
    const dest = normalizePath(root, body.destinationPath);
    const chunks = readdirSync(tempDir)
        .filter((n) => n.startsWith("chunk_"))
        .sort((a, b) => Number(a.split("_")[1]) - Number(b.split("_")[1]));
    if (chunks.length === 0) return fail(res, "No chunks found for upload");
    mkdirSync(dirname(dest), {recursive: true});
    await writeFile(dest, Buffer.alloc(0));
    for (const chunk of chunks) await appendFile(dest, await readFile(join(tempDir, chunk)));
    rmSync(tempDir, {recursive: true, force: true});
    return message(res, "File uploaded successfully");
};

const copyOrMove = (req, res, {root}, move) => {
    const body = parseBody(req);
    if (!body.sourcePath || !body.destinationPath) return fail(res, "Missing required fields");
    const source = normalizePath(root, body.sourcePath);
    const dest = normalizePath(root, body.destinationPath);
    if (!existsSync(source) || statSync(source).isDirectory()) return fail(res, "The source file does not exist");
    if (basename(source) === "voxeldash.db") return fail(res, "You are not allowed to copy the database file");
    if (existsSync(dest)) return fail(res, "A file already exists at the destination path");
    if (!move) {
        copyFileSync(source, dest);
        return message(res, "File copied successfully");
    }
    try {
        renameSync(source, dest);
    } catch {
        copyFileSync(source, dest);
        unlinkSync(source);
    }
    return message(res, "File moved successfully");
};

export const fileRoutes = [
    {method: "GET", path: "files/list", handler: listFiles},
    {method: "DELETE", path: "files", handler: deleteFile},
    {method: "GET", path: "files/download", handler: downloadFile},
    {method: "PATCH", path: "files/rename", handler: renameFile},
    {method: "PATCH", path: "files/content", handler: updateContent},
    {method: "POST", path: "files/upload/init", handler: uploadInit},
    {method: "PUT", path: "files/upload/chunk/:uuid/:id", handler: uploadChunk},
    {method: "POST", path: "files/upload/stop", handler: uploadStop},
    {method: "POST", path: "files/copy", handler: (req, res, ctx) => copyOrMove(req, res, ctx, false)},
    {method: "POST", path: "files/move", handler: (req, res, ctx) => copyOrMove(req, res, ctx, true)},
];
