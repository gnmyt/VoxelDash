package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.entities.players.TeleportCapabilities;
import de.gnm.voxeldash.api.pipes.players.TeleportPipe;
import de.gnm.voxeldash.util.ForgeUtil;

public class TeleportPipeImpl implements TeleportPipe {

    @Override
    public TeleportCapabilities getCapabilities() {
        return new TeleportCapabilities(true, true, true, false);
    }

    @Override
    public void teleportToCoords(String playerName, double x, double y, double z, String world) {
        ForgeUtil.compat().teleportToCoords(playerName, x, y, z, world);
    }

    @Override
    public void teleportToPlayer(String playerName, String targetName) {
        ForgeUtil.compat().teleportToPlayer(playerName, targetName);
    }

    @Override
    public void teleportToSpawn(String playerName) {
        ForgeUtil.compat().teleportToSpawn(playerName);
    }

    @Override
    public void teleportToServer(String playerName, String serverName) {
        // Not applicable on a single Forge server.
    }
}
