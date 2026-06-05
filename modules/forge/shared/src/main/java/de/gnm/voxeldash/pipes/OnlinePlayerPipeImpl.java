package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.entities.OnlinePlayer;
import de.gnm.voxeldash.api.pipes.players.OnlinePlayerPipe;
import de.gnm.voxeldash.util.ForgeUtil;

import java.util.ArrayList;

public class OnlinePlayerPipeImpl implements OnlinePlayerPipe {

    @Override
    public ArrayList<OnlinePlayer> getOnlinePlayers() {
        return ForgeUtil.compat().onlinePlayers();
    }

    @Override
    public void kickPlayer(String playerName, String reason) {
        ForgeUtil.compat().kickPlayer(playerName, reason);
    }

    @Override
    public void setGamemode(String playerName, String gamemode) {
        ForgeUtil.compat().setGamemode(playerName, gamemode);
    }

    @Override
    public void teleportToWorld(String playerName, String worldName) {
        ForgeUtil.compat().teleportToWorld(playerName, worldName);
    }
}
