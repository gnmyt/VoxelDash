package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.entities.OfflinePlayer;
import de.gnm.voxeldash.api.pipes.players.WhitelistPipe;
import de.gnm.voxeldash.util.ForgeUtil;

import java.util.ArrayList;

public class WhitelistPipeImpl implements WhitelistPipe {

    @Override
    public void setStatus(boolean status) {
        ForgeUtil.compat().setWhitelistEnabled(status);
    }

    @Override
    public boolean getStatus() {
        return ForgeUtil.compat().whitelistEnabled();
    }

    @Override
    public ArrayList<OfflinePlayer> getWhitelistedPlayers() {
        return ForgeUtil.compat().whitelistedPlayers();
    }

    @Override
    public void addPlayer(String playerName) {
        ForgeUtil.compat().whitelistAdd(playerName);
    }

    @Override
    public void removePlayer(String playerName) {
        ForgeUtil.compat().whitelistRemove(playerName);
    }
}
