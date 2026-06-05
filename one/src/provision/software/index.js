import {paper} from "./paper.js";
import {fabric} from "./fabric.js";
import {forge} from "./forge.js";
import {vanilla} from "./vanilla.js";
import {bungeecord} from "./bungeecord.js";

const ADAPTERS = {paper, fabric, forge, vanilla, bungeecord};

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
        key: "forge",
        name: "Forge",
        kind: "server",
        accent: "#8295ad",
        tagline: "The original Minecraft mod loader"
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

export const getSoftware = (key) => {
    return ADAPTERS[key] || null;
};

export const listSoftware = () => {
    return CATALOG;
};
