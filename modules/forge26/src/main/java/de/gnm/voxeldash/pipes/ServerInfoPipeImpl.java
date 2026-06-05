package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.VoxelDashMod;
import de.gnm.voxeldash.api.pipes.ServerInfoPipe;
import net.minecraft.server.MinecraftServer;

public class ServerInfoPipeImpl implements ServerInfoPipe {

    @Override
    public String getServerSoftware() {
        return "forge";
    }

    @Override
    public String getServerVersion() {
        MinecraftServer server = VoxelDashMod.getServer();
        if (server != null) {
            return server.getServerVersion();
        }
        return "Unknown";
    }

    @Override
    public int getServerPort() {
        MinecraftServer server = VoxelDashMod.getServer();
        if (server != null) {
            return server.getPort();
        }
        return 25565;
    }
}
