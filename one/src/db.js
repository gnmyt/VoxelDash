import {Database} from "bun:sqlite";
import {config} from "./config.js";

const MIGRATIONS = [
    `CREATE TABLE IF NOT EXISTS master_users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
  );
  CREATE TABLE IF NOT EXISTS master_sessions (
    token TEXT PRIMARY KEY,
    user_id INTEGER NOT NULL,
    last_used TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (user_id) REFERENCES master_users(id) ON DELETE CASCADE
  );
  CREATE TABLE IF NOT EXISTS servers (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    software TEXT NOT NULL,
    mc_version TEXT,
    build TEXT,
    game_port INTEGER,
    api_port INTEGER,
    api_token TEXT,
    java_major INTEGER,
    memory_mb INTEGER NOT NULL DEFAULT 2048,
    status TEXT NOT NULL DEFAULT 'installing',
    auto_start INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
  );`,
];

function migrate(database) {
    const {user_version: version} = database.query("PRAGMA user_version").get();
    for (let v = version; v < MIGRATIONS.length; v++) {
        database.transaction(() => {
            database.exec(MIGRATIONS[v]);
            database.exec(`PRAGMA user_version = ${v + 1}`);
        })();
    }
}

export const db = new Database(config.paths.db, {create: true});
db.exec("PRAGMA journal_mode = WAL");
db.exec("PRAGMA foreign_keys = ON");
migrate(db);
