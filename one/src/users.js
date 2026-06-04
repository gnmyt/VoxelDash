import {db} from "./db.js";
import {createUser} from "./auth.js";
import {describeUser, isAdmin, LEVEL, setPermissions, setServerAccess} from "./permissions.js";

const getUserRow = (id) => db.query("SELECT id, username FROM master_users WHERE id = ?").get(id);

const serialize = (row) => ({id: row.id, username: row.username, ...describeUser(row.id)});

export const mountUserRoutes = (app, requireFeature) => {
    const guard = requireFeature("UserManagement", LEVEL.FULL);

    app.get("/master/users/features", guard, (req, res) => {
        res.json({features: ["Servers", "Forwardings", "UserManagement"]});
    });

    app.get("/master/users", guard, (req, res) => {
        const rows = db.query("SELECT id, username FROM master_users ORDER BY id ASC").all();
        res.json({users: rows.map(serialize)});
    });

    app.post("/master/users", guard, async (req, res) => {
        const {username, password} = req.body || {};
        if (!username || username.length < 3) return res.status(400).json({error: "Username must be at least 3 characters"});
        if (!password || password.length < 4) return res.status(400).json({error: "Password must be at least 4 characters"});
        if (db.query("SELECT 1 FROM master_users WHERE username = ?").get(username)) {
            return res.status(409).json({error: "Username already exists"});
        }
        const id = await createUser(username, password);
        res.status(201).json({user: serialize(getUserRow(id))});
    });

    app.put("/master/users/:id/permissions", guard, (req, res) => {
        const id = parseInt(req.params.id, 10);
        const row = getUserRow(id);
        if (!row) return res.status(404).json({error: "User not found"});
        if (isAdmin(id)) return res.status(400).json({error: "The admin always has full access"});

        const {permissions, allServers, serverIds} = req.body || {};
        if (permissions) setPermissions(id, permissions);
        if (allServers !== undefined || serverIds !== undefined) {
            setServerAccess(id, !!allServers, Array.isArray(serverIds) ? serverIds : []);
        }
        res.json({user: serialize(row)});
    });

    app.put("/master/users/:id/password", guard, async (req, res) => {
        const id = parseInt(req.params.id, 10);
        if (!getUserRow(id)) return res.status(404).json({error: "User not found"});
        const {password} = req.body || {};
        if (!password || password.length < 4) return res.status(400).json({error: "Password must be at least 4 characters"});
        const hash = await Bun.password.hash(password, {algorithm: "bcrypt", cost: 10});
        db.query("UPDATE master_users SET password = ? WHERE id = ?").run(hash, id);
        res.json({ok: true});
    });

    app.put("/master/users/:id/username", guard, (req, res) => {
        const id = parseInt(req.params.id, 10);
        if (!getUserRow(id)) return res.status(404).json({error: "User not found"});
        const {username} = req.body || {};
        if (!username || username.length < 3) return res.status(400).json({error: "Username must be at least 3 characters"});
        if (db.query("SELECT 1 FROM master_users WHERE username = ? AND id != ?").get(username, id)) {
            return res.status(409).json({error: "Username already exists"});
        }
        db.query("UPDATE master_users SET username = ? WHERE id = ?").run(username, id);
        res.json({user: serialize(getUserRow(id))});
    });

    app.delete("/master/users/:id", guard, (req, res) => {
        const id = parseInt(req.params.id, 10);
        if (!getUserRow(id)) return res.status(404).json({error: "User not found"});
        if (isAdmin(id)) return res.status(400).json({error: "Cannot delete the admin account"});
        if (id === req.user.id) return res.status(400).json({error: "You cannot delete your own account"});
        db.query("DELETE FROM master_users WHERE id = ?").run(id);
        res.json({ok: true});
    });
};
