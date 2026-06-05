package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.VoxelDashMod;
import de.gnm.voxeldash.api.pipes.QuickActionPipe;
import de.gnm.voxeldash.util.ForgeUtil;
import net.minecraft.server.MinecraftServer;

public class QuickActionPipeImpl implements QuickActionPipe {

    @Override
    public void reloadServer() {
        ForgeUtil.runOnMainThread(() -> {
            MinecraftServer server = VoxelDashMod.getServer();
            if (server != null) {
                server.reloadResources(server.getPackRepository().getSelectedIds());
            }
        });
    }

    @Override
    public void stopServer() {
        ForgeUtil.runOnMainThread(() -> {
            MinecraftServer server = VoxelDashMod.getServer();
            if (server != null) {
                server.halt(false);
            }
        });
    }

    @Override
    public void sendCommand(String command) {
        ForgeUtil.runOnMainThread(() -> {
            MinecraftServer server = VoxelDashMod.getServer();
            if (server != null) {
                String cleanCommand = command.startsWith("/") ? command.substring(1) : command;
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), cleanCommand);
            }
        });
    }
}
