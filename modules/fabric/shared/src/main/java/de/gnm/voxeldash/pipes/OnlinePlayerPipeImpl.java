package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.entities.OnlinePlayer;
import de.gnm.voxeldash.api.pipes.players.OnlinePlayerPipe;
import de.gnm.voxeldash.util.FabricUtil;

import java.util.ArrayList;

public class OnlinePlayerPipeImpl implements OnlinePlayerPipe {

    @Override
    public ArrayList<OnlinePlayer> getOnlinePlayers() {
        return FabricUtil.compat().onlinePlayers();
    }

    @Override
    public void kickPlayer(String playerName, String reason) {
        FabricUtil.compat().kickPlayer(playerName, reason);
    }

    @Override
    public void setGamemode(String playerName, String gamemode) {
        FabricUtil.compat().setGamemode(playerName, gamemode);
    }

    @Override
    public void teleportToWorld(String playerName, String worldName) {
        FabricUtil.compat().teleportToWorld(playerName, worldName);
    }
}
