package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.entities.OfflinePlayer;
import de.gnm.voxeldash.api.pipes.players.WhitelistPipe;
import de.gnm.voxeldash.util.FabricUtil;

import java.util.ArrayList;

public class WhitelistPipeImpl implements WhitelistPipe {

    @Override
    public void setStatus(boolean status) {
        FabricUtil.compat().setWhitelistEnabled(status);
    }

    @Override
    public boolean getStatus() {
        return FabricUtil.compat().whitelistEnabled();
    }

    @Override
    public ArrayList<OfflinePlayer> getWhitelistedPlayers() {
        return FabricUtil.compat().whitelistedPlayers();
    }

    @Override
    public void addPlayer(String playerName) {
        FabricUtil.compat().whitelistAdd(playerName);
    }

    @Override
    public void removePlayer(String playerName) {
        FabricUtil.compat().whitelistRemove(playerName);
    }
}
