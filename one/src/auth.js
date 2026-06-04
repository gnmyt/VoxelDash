import {db} from "./db.js";
import {randomToken} from "./util.js";
import {canAccessServer, describeUser, hasLevel, LEVEL} from "./permissions.js";

const HASH_OPTS = {algorithm: "bcrypt", cost: 10};

export const userCount = () => {
    return db.query("SELECT COUNT(*) AS c FROM master_users").get().c;
};

export const createUser = async (username, password) => {
    const hash = await Bun.password.hash(password, HASH_OPTS);
    return db.query("INSERT INTO master_users (username, password) VALUES (?, ?)").run(username, hash).lastInsertRowid;
};

export const verifyLogin = async (username, password) => {
    const row = db.query("SELECT * FROM master_users WHERE username = ?").get(username);
    if (!row) return null;
    return (await Bun.password.verify(password, row.password)) ? row : null;
};

export const createSession = (userId) => {
    const token = randomToken(48);
    db.query("INSERT INTO master_sessions (token, user_id) VALUES (?, ?)").run(token, userId);
    return token;
};

export const getUserByToken = (token) => {
    if (!token) return null;
    const row = db
        .query("SELECT u.id, u.username FROM master_sessions s JOIN master_users u ON u.id = s.user_id WHERE s.token = ?")
        .get(token);
    if (row) db.query("UPDATE master_sessions SET last_used = datetime('now') WHERE token = ?").run(token);
    return row || null;
};

export const destroySession = (token) => {
    db.query("DELETE FROM master_sessions WHERE token = ?").run(token);
};

const bearer = (req) => {
    const header = req.headers["authorization"] || "";
    return header.startsWith("Bearer ") ? header.slice(7) : null;
};

export const requireMasterAuth = (req, res, next) => {
    const user = getUserByToken(bearer(req));
    if (!user) return res.status(401).json({error: "Unauthorized"});
    req.user = user;
    next();
};

export const requireFeature = (feature, level = LEVEL.READ) => (req, res, next) => {
    requireMasterAuth(req, res, () => {
        if (!hasLevel(req.user.id, feature, level)) {
            return res.status(403).json({error: "Insufficient permissions"});
        }
        next();
    });
};

export const requireServerAccess = (req, res, next) => {
    if (!canAccessServer(req.user.id, req.params.serverId || req.params.id)) {
        return res.status(403).json({error: "No access to this server"});
    }
    next();
};

export const mountAuthRoutes = (app) => {
    app.get("/master/status", (req, res) => {
        res.json({setupRequired: userCount() === 0, name: "VoxelDash One"});
    });

    app.post("/master/auth/setup", async (req, res) => {
        if (userCount() > 0) return res.status(409).json({error: "Already set up"});
        const {username, password} = req.body || {};
        if (!username || !password || password.length < 4) {
            return res.status(400).json({error: "Username and a password (min 4 chars) are required"});
        }
        const userId = await createUser(username, password);
        res.json({token: createSession(userId), user: {id: userId, username}});
    });

    app.post("/master/auth/login", async (req, res) => {
        const {username, password} = req.body || {};
        const user = await verifyLogin(username, password);
        if (!user) return res.status(401).json({error: "Invalid credentials"});
        res.json({token: createSession(user.id), user: {id: user.id, username: user.username}});
    });

    app.post("/master/auth/logout", requireMasterAuth, (req, res) => {
        destroySession(bearer(req));
        res.json({ok: true});
    });

    app.get("/master/me", requireMasterAuth, (req, res) => {
        res.json({user: {...req.user, ...describeUser(req.user.id)}});
    });
};
