import {join} from "node:path";

export async function writeMinecraftProperties(serverDir, {gamePort, name}) {
    await Bun.write(join(serverDir, "eula.txt"), "eula=true\n");
    await Bun.write(
        join(serverDir, "server.properties"),
        [
            `server-port=${gamePort}`,
            "online-mode=true",
            "max-players=20",
            `motd=${name} - powered by VoxelDash One`,
        ].join("\n") + "\n"
    );
}
