package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.entities.players.TeleportCapabilities;
import de.gnm.voxeldash.api.pipes.players.TeleportPipe;
import de.gnm.voxeldash.util.FabricUtil;

public class TeleportPipeImpl implements TeleportPipe {

    @Override
    public TeleportCapabilities getCapabilities() {
        return new TeleportCapabilities(true, true, true, false);
    }

    @Override
    public void teleportToCoords(String playerName, double x, double y, double z, String world) {
        FabricUtil.compat().teleportToCoords(playerName, x, y, z, world);
    }

    @Override
    public void teleportToPlayer(String playerName, String targetName) {
        FabricUtil.compat().teleportToPlayer(playerName, targetName);
    }

    @Override
    public void teleportToSpawn(String playerName) {
        FabricUtil.compat().teleportToSpawn(playerName);
    }

    @Override
    public void teleportToServer(String playerName, String serverName) {
        // Not applicable on a single Fabric server.
    }
}
