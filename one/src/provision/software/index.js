import {paper} from "./paper.js";
import {fabric} from "./fabric.js";
import {vanilla} from "./vanilla.js";
import {bungeecord} from "./bungeecord.js";

const ADAPTERS = {paper, fabric, vanilla, bungeecord};

const CATALOG = [
    {
        key: "paper",
        name: "Paper",
        kind: "server",
        accent: "#4f8ff7",
        tagline: "High-performance Spigot fork"
    },
    {
        key: "fabric",
        name: "Fabric",
        kind: "server",
        accent: "#cbb389",
        tagline: "Lightweight, modular mod loader"
    },
    {
        key: "vanilla",
        name: "Vanilla",
        kind: "server",
        accent: "#5cb85c",
        tagline: "Pure, unmodified Minecraft"
    },
    {
        key: "bungeecord",
        name: "BungeeCord",
        kind: "proxy",
        accent: "#e0a82e",
        tagline: "Connect multiple servers"
    },
];

export function getSoftware(key) {
    return ADAPTERS[key] || null;
}

export function listSoftware() {
    return CATALOG;
}
