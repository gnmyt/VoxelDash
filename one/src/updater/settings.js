import {db} from "../db.js";

const DEFAULTS = {update_channel: "beta", auto_update: "0"};

const get = (key) => {
    const row = db.query("SELECT value FROM master_settings WHERE key = ?").get(key);
    return row ? row.value : DEFAULTS[key];
};

const set = (key, value) => {
    db.query(
        `INSERT INTO master_settings (key, value) VALUES (?, ?)
     ON CONFLICT(key) DO UPDATE SET value = excluded.value`
    ).run(key, String(value));
};

export const getChannel = () => (get("update_channel") === "release" ? "release" : "beta");
export const isAutoUpdate = () => get("auto_update") === "1";

export const getSettings = () => ({channel: getChannel(), autoUpdate: isAutoUpdate()});

export const setChannel = (channel) => {
    if (channel !== "release" && channel !== "beta") throw new Error("Invalid channel");
    set("update_channel", channel);
};

export const setAutoUpdate = (enabled) => set("auto_update", enabled ? "1" : "0");
