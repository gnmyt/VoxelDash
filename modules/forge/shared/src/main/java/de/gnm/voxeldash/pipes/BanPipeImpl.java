package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.entities.BannedPlayer;
import de.gnm.voxeldash.api.pipes.players.BanPipe;
import de.gnm.voxeldash.util.ForgeUtil;

import java.util.ArrayList;

public class BanPipeImpl implements BanPipe {

    @Override
    public ArrayList<BannedPlayer> getBannedPlayers() {
        return ForgeUtil.compat().bannedPlayers();
    }

    @Override
    public void banPlayer(String playerName, String reason) {
        ForgeUtil.compat().banPlayer(playerName, reason);
    }

    @Override
    public void unbanPlayer(String playerName) {
        ForgeUtil.compat().unbanPlayer(playerName);
    }
}
