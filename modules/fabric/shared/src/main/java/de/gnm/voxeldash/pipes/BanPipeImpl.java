package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.entities.BannedPlayer;
import de.gnm.voxeldash.api.pipes.players.BanPipe;
import de.gnm.voxeldash.util.FabricUtil;

import java.util.ArrayList;

public class BanPipeImpl implements BanPipe {

    @Override
    public ArrayList<BannedPlayer> getBannedPlayers() {
        return FabricUtil.compat().bannedPlayers();
    }

    @Override
    public void banPlayer(String playerName, String reason) {
        FabricUtil.compat().banPlayer(playerName, reason);
    }

    @Override
    public void unbanPlayer(String playerName) {
        FabricUtil.compat().unbanPlayer(playerName);
    }
}
