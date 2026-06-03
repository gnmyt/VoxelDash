import {join} from "node:path";
import {config} from "../../config.js";

const JENKINS = "https://ci.md-5.net/job/BungeeCord/lastSuccessfulBuild";

export const bungeecord = {
    key: "bungeecord",
    name: "BungeeCord",
    kind: "proxy",
    voxeldashArtifact: "bungeecord",
    installDir: "plugins",
    minJava: 8,

    async listVersions() {
        return ["latest"];
    },

    async resolveServerJar() {
        let build = "latest";
        try {
            const response = await fetch(`${JENKINS}/buildNumber`, {headers: {"User-Agent": config.userAgent}});
            if (response.ok) build = `b${(await response.text()).trim()}`;
        } catch {
        }
        return {url: `${JENKINS}/artifact/bootstrap/target/BungeeCord.jar`, fileName: "server.jar", build};
    },

    async javaMajor() {
        return 21;
    },

    async layout(serverDir, ctx) {
        const configYml = [
            "listeners:",
            `- host: 0.0.0.0:${ctx.gamePort}`,
            `  motd: '${ctx.name} - powered by VoxelDash One'`,
            "  max_players: 100",
            "  priorities:",
            "  - lobby",
            "  force_default_server: false",
            "  bind_local_address: true",
            "  proxy_protocol: false",
            "  tab_list: GLOBAL_PING",
            "  tab_size: 60",
            "servers:",
            "  lobby:",
            `    motd: '${ctx.name}'`,
            "    address: 127.0.0.1:25566",
            "    restricted: false",
            "ip_forward: false",
            "online_mode: true",
            "network_compression_threshold: 256",
        ].join("\n");
        await Bun.write(join(serverDir, "config.yml"), configYml + "\n");
    },

    launchArgs(jarFileName) {
        return ["-jar", jarFileName];
    },
};
