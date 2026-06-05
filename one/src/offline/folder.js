import {cpSync, existsSync, mkdirSync, renameSync, rmSync, statSync} from "node:fs";
import {basename, resolve} from "node:path";
import {collectEntries, fail, message, normalizePath, parseBody, safeFilename, zipEntriesToStream} from "./fs-helpers.js";

const createFolder = (req, res, {root}) => {
    const body = parseBody(req);
    if (!body.path) return fail(res, "Missing required field: path");
    const dir = normalizePath(root, body.path);
    if (existsSync(dir)) return fail(res, "The directory already exists");
    mkdirSync(dir, {recursive: true});
    return message(res, "Directory created");
};

const deleteFolder = (req, res, {root}) => {
    const body = parseBody(req);
    if (!body.path) return fail(res, "Missing required field: path");
    const dir = normalizePath(root, body.path);
    if (!existsSync(dir) || !statSync(dir).isDirectory()) return fail(res, "The directory does not exist");
    if (resolve(dir) === resolve(root)) return fail(res, "Cannot delete the server root directory");
    rmSync(dir, {recursive: true, force: true});
    return message(res, "Directory deleted");
};

const renameFolder = (req, res, {root}) => {
    const body = parseBody(req);
    if (!body.path || !body.newPath) return fail(res, "Missing required fields");
    const dir = normalizePath(root, body.path);
    const newDir = normalizePath(root, body.newPath);
    if (!existsSync(dir) || !statSync(dir).isDirectory()) return fail(res, "The directory does not exist");
    if (existsSync(newDir)) return fail(res, "The new directory already exists");
    renameSync(dir, newDir);
    return message(res, "Directory renamed");
};

const downloadFolder = async (req, res, {root}) => {
    const dir = normalizePath(root, req.query.path);
    if (!existsSync(dir) || !statSync(dir).isDirectory()) return fail(res, "The directory does not exist");
    if (resolve(dir) === resolve(root)) return fail(res, "Cannot download the server root directory");
    res.setHeader("Content-Type", "application/zip");
    res.setHeader("Content-Disposition", `attachment; filename="${safeFilename(basename(dir))}.zip"`);
    res.status(200);
    await zipEntriesToStream(collectEntries([], dir, basename(dir)), res);
    return true;
};

const copyOrMove = (req, res, {root}, move) => {
    const body = parseBody(req);
    if (!body.sourcePath || !body.destinationPath) return fail(res, "Missing required fields");
    const source = normalizePath(root, body.sourcePath);
    const dest = normalizePath(root, body.destinationPath);
    if (!existsSync(source) || !statSync(source).isDirectory()) return fail(res, "The source directory does not exist");
    if (resolve(source) === resolve(root)) return fail(res, `Cannot ${move ? "move" : "copy"} the server root directory`);
    if (existsSync(dest)) return fail(res, "A directory already exists at the destination path");
    if (!move) {
        cpSync(source, dest, {recursive: true});
        return message(res, "Directory copied successfully");
    }
    try {
        renameSync(source, dest);
    } catch {
        cpSync(source, dest, {recursive: true});
        rmSync(source, {recursive: true, force: true});
    }
    return message(res, "Directory moved successfully");
};

export const folderRoutes = [
    {method: "PUT", path: "folder", handler: createFolder},
    {method: "DELETE", path: "folder", handler: deleteFolder},
    {method: "PATCH", path: "folder/rename", handler: renameFolder},
    {method: "GET", path: "folder/download", handler: downloadFolder},
    {method: "POST", path: "folder/copy", handler: (req, res, ctx) => copyOrMove(req, res, ctx, false)},
    {method: "POST", path: "folder/move", handler: (req, res, ctx) => copyOrMove(req, res, ctx, true)},
];
