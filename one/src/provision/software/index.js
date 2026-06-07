import {paper} from "./paper.js";
import {spigot} from "./spigot.js";
import {folia} from "./folia.js";
import {purpur} from "./purpur.js";
import {pufferfish} from "./pufferfish.js";
import {fabric} from "./fabric.js";
import {forge} from "./forge.js";
import {neoforge} from "./neoforge.js";
import {vanilla} from "./vanilla.js";
import {bungeecord} from "./bungeecord.js";

const ADAPTERS = {paper, spigot, folia, purpur, pufferfish, fabric, forge, neoforge, vanilla, bungeecord};

const CATALOG = [
    {
        key: "paper",
        name: "Paper",
        kind: "server",
        category: "plugins",
        accent: "#4f8ff7",
        tagline: "High-performance Spigot fork"
    },
    {
        key: "spigot",
        name: "Spigot",
        kind: "server",
        category: "plugins",
        accent: "#f0a020",
        tagline: "The original Bukkit server"
    },
    {
        key: "purpur",
        name: "Purpur",
        kind: "server",
        category: "plugins",
        accent: "#9b6cf2",
        tagline: "Paper fork with extra customization"
    },
    {
        key: "pufferfish",
        name: "Pufferfish",
        kind: "server",
        category: "plugins",
        accent: "#f0922f",
        tagline: "Performance-tuned Paper fork"
    },
    {
        key: "folia",
        name: "Folia",
        kind: "server",
        category: "plugins",
        accent: "#5bbf8f",
        tagline: "Multithreaded Paper for huge servers"
    },
    {
        key: "fabric",
        name: "Fabric",
        kind: "server",
        category: "modded",
        accent: "#cbb389",
        tagline: "Lightweight, modular mod loader"
    },
    {
        key: "forge",
        name: "Forge",
        kind: "server",
        category: "modded",
        accent: "#8295ad",
        tagline: "The original Minecraft mod loader"
    },
    {
        key: "neoforge",
        name: "NeoForge",
        kind: "server",
        category: "modded",
        accent: "#e8732a",
        tagline: "Modern Forge-based mod loader"
    },
    {
        key: "vanilla",
        name: "Vanilla",
        kind: "server",
        category: "vanilla",
        accent: "#5cb85c",
        tagline: "Pure, unmodified Minecraft"
    },
    {
        key: "bungeecord",
        name: "BungeeCord",
        kind: "proxy",
        category: "proxy",
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
