import paperLogo from "@/assets/images/software/paper.png";
import spigotLogo from "@/assets/images/software/spigot.png";
import foliaLogo from "@/assets/images/software/folia.png";
import purpurLogo from "@/assets/images/software/purpur.svg";
import pufferfishLogo from "@/assets/images/software/pufferfish.png";
import fabricLogo from "@/assets/images/software/fabric.png";
import forgeLogo from "@/assets/images/software/forge.svg";
import neoforgeLogo from "@/assets/images/software/neoforge.png";
import vanillaLogo from "@/assets/images/software/vanilla.png";
import bungeecordLogo from "@/assets/images/software/bungeecord.svg";

export interface SoftwareMeta {
    name: string;
    accent: string;
    short: string;
    logo?: string;
    kind: string;
}

export const SOFTWARE: Record<string, SoftwareMeta> = {
    paper: {name: "Paper", accent: "#4f8ff7", short: "PA", logo: paperLogo, kind: "Plugins"},
    spigot: {name: "Spigot", accent: "#f0a020", short: "SP", logo: spigotLogo, kind: "Plugins"},
    folia: {name: "Folia", accent: "#5bbf8f", short: "FL", logo: foliaLogo, kind: "Plugins"},
    purpur: {name: "Purpur", accent: "#9b6cf2", short: "PU", logo: purpurLogo, kind: "Plugins"},
    pufferfish: {name: "Pufferfish", accent: "#f0922f", short: "PF", logo: pufferfishLogo, kind: "Plugins"},
    fabric: {name: "Fabric", accent: "#c9b27f", short: "FA", logo: fabricLogo, kind: "Mods"},
    forge: {name: "Forge", accent: "#8295ad", short: "FO", logo: forgeLogo, kind: "Mods"},
    neoforge: {name: "NeoForge", accent: "#e8732a", short: "NF", logo: neoforgeLogo, kind: "Mods"},
    vanilla: {name: "Vanilla", accent: "#6abf4b", short: "VA", logo: vanillaLogo, kind: "Pure"},
    bungeecord: {name: "BungeeCord", accent: "#d6932f", short: "BC", logo: bungeecordLogo, kind: "Proxy"},
};

export const softwareMeta = (key: string): SoftwareMeta =>
    SOFTWARE[key] || {name: key, accent: "#8b5cf6", short: key.slice(0, 2).toUpperCase(), kind: "Server"};

export interface StatusMeta {
    label: string;
    text: string;
    dot: string;
    pulse: string;
}

export const STATUS: Record<string, StatusMeta> = {
    online: {label: "Online", text: "text-emerald-500", dot: "bg-emerald-500", pulse: "142 70% 45%"},
    starting: {label: "Starting", text: "text-amber-500", dot: "bg-amber-500", pulse: "38 92% 50%"},
    installing: {label: "Installing", text: "text-sky-500", dot: "bg-sky-500", pulse: "199 89% 48%"},
    install_failed: {label: "Install failed", text: "text-red-500", dot: "bg-red-500", pulse: "0 84% 60%"},
    offline: {label: "Offline", text: "text-muted-foreground", dot: "bg-muted-foreground/40", pulse: "0 0% 50%"},
};

export const statusMeta = (status: string): StatusMeta => STATUS[status] || STATUS.offline;
