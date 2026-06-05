import {config} from "../config.js";

const CACHE_TTL_MS = 5 * 60 * 1000;
let cache = {at: 0, releases: null};

const ghHeaders = () => ({"User-Agent": config.userAgent, Accept: "application/vnd.github+json"});

export const getReleases = async (force = false) => {
    if (!force && cache.releases && Date.now() - cache.at < CACHE_TTL_MS) return cache.releases;

    const response = await fetch(`https://api.github.com/repos/${config.repo}/releases?per_page=30`, {headers: ghHeaders()});
    if (!response.ok) throw new Error(`GitHub release lookup failed: ${response.status}`);

    const releases = (await response.json()).filter((r) => !r.draft);
    cache = {at: Date.now(), releases};
    return releases;
};

export const latestForChannel = async (channel) => {
    const releases = await getReleases();
    const candidates = channel === "release" ? releases.filter((r) => !r.prerelease) : releases;
    let best = null;
    for (const r of candidates) {
        if (!best || compareVersions(r.tag_name, best.tag_name) > 0) best = r;
    }
    return best;
};

export const normalizeVersion = (tag) => String(tag || "").replace(/^v/, "");

const parseVersion = (v) => {
    const [core, pre] = normalizeVersion(v).split("-", 2);
    const base = core.split(".").map((n) => parseInt(n, 10) || 0);
    const preParts = pre ? pre.split(".").map((p) => (/^\d+$/.test(p) ? parseInt(p, 10) : p)) : [];
    return {base, pre: preParts};
};

export const compareVersions = (a, b) => {
    const pa = parseVersion(a);
    const pb = parseVersion(b);
    for (let i = 0; i < Math.max(pa.base.length, pb.base.length); i++) {
        const diff = (pa.base[i] || 0) - (pb.base[i] || 0);
        if (diff) return diff;
    }
    if (pa.pre.length === 0 && pb.pre.length > 0) return 1;
    if (pa.pre.length > 0 && pb.pre.length === 0) return -1;
    for (let i = 0; i < Math.max(pa.pre.length, pb.pre.length); i++) {
        const x = pa.pre[i];
        const y = pb.pre[i];
        if (x === undefined) return -1;
        if (y === undefined) return 1;
        const xNum = typeof x === "number";
        const yNum = typeof y === "number";
        if (xNum && yNum) {
            if (x !== y) return x - y;
        } else if (xNum !== yNum) {
            return xNum ? -1 : 1;
        } else if (x !== y) {
            return x < y ? -1 : 1;
        }
    }
    return 0;
};

export const isNewer = (latest, current) => {
    if (!latest) return false;
    if (!current || current === "unknown") return true;
    return compareVersions(latest, current) > 0;
};

export const findServerAsset = (release, artifact) =>
    (release?.assets || []).find((a) => a.name.includes(`voxeldash-${artifact}-`) && a.name.endsWith(".jar")) || null;

export const findNodeAsset = (release, platform = config.platform) => {
    const suffix = platform === "windows" ? "-windows-x64.zip" : "-linux-x64.tar.gz";
    return (release?.assets || []).find((a) => a.name.startsWith("voxeldash-one-") && a.name.endsWith(suffix)) || null;
};
