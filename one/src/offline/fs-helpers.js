import {createReadStream, readdirSync, statSync} from "node:fs";
import {basename, resolve, sep} from "node:path";
import {AsyncZipDeflate, Zip} from "fflate";

export const normalizePath = (root, requested) => {
    const rootResolved = resolve(root);
    if (!requested) return rootResolved;
    const clean = requested.startsWith("/") ? requested.slice(1) : requested;
    if (!clean) return rootResolved;
    const resolved = resolve(rootResolved, clean);
    if (resolved !== rootResolved && !resolved.startsWith(rootResolved + sep)) return rootResolved;
    return resolved;
};

export const withinRoot = (root, target) => {
    const rootResolved = resolve(root);
    return target === rootResolved || target.startsWith(rootResolved + sep);
};

export const parseBody = (req) => {
    try {
        return req.body && req.body.length ? JSON.parse(req.body.toString("utf8")) : {};
    } catch {
        return {};
    }
};

export const ok = (res, fields = {}) => {
    res.status(200).json(fields);
    return true;
};

export const message = (res, msg) => ok(res, {message: msg});

export const fail = (res, msg, code = 400) => {
    res.status(code).json({error: msg});
    return true;
};

export const safeFilename = (name) => name.replace(/[\r\n"]/g, "_");

export const streamFile = (res, file) => {
    const st = statSync(file);
    res.setHeader("Content-Type", "application/octet-stream");
    res.setHeader("Content-Disposition", `attachment; filename="${safeFilename(basename(file))}"`);
    res.setHeader("Content-Length", String(st.size));
    createReadStream(file).pipe(res);
    return true;
};

export const collectEntries = (out, absPath, entryBase, skipDir) => {
    let st;
    try {
        st = statSync(absPath);
    } catch {
        return out;
    }
    if (st.isDirectory()) {
        if (skipDir && resolve(absPath) === resolve(skipDir)) return out;
        for (const child of readdirSync(absPath)) {
            collectEntries(out, resolve(absPath, child), entryBase ? `${entryBase}/${child}` : child, skipDir);
        }
        return out;
    }
    out.push({name: entryBase, absPath});
    return out;
};

export const zipEntriesToStream = (entries, outStream) =>
    new Promise((res, rej) => {
        const zip = new Zip();
        let failed = false;
        const die = (e) => {
            if (failed) return;
            failed = true;
            rej(e);
        };

        zip.ondata = (err, chunk, final) => {
            if (err) return die(err);
            outStream.write(Buffer.from(chunk));
            if (final) outStream.end(() => !failed && res());
        };
        outStream.on("error", die);

        (async () => {
            for (const {name, absPath} of entries) {
                if (failed) return;
                const file = new AsyncZipDeflate(name, {level: 6});
                zip.add(file);
                await new Promise((done, bad) => {
                    const rs = createReadStream(absPath);
                    rs.on("data", (c) => file.push(c, false));
                    rs.on("end", () => {
                        file.push(new Uint8Array(0), true);
                        done();
                    });
                    rs.on("error", bad);
                });
            }
            zip.end();
        })().catch(die);
    });
