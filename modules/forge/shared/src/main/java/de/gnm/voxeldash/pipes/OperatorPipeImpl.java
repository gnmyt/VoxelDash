package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.entities.OfflinePlayer;
import de.gnm.voxeldash.api.pipes.players.OperatorPipe;
import de.gnm.voxeldash.util.ForgeUtil;

import java.util.ArrayList;

public class OperatorPipeImpl implements OperatorPipe {

    @Override
    public ArrayList<OfflinePlayer> getOperators() {
        return ForgeUtil.compat().operators();
    }

    @Override
    public void setOp(String playerName) {
        ForgeUtil.compat().setOp(playerName);
    }

    @Override
    public void deOp(String playerName) {
        ForgeUtil.compat().deOp(playerName);
    }
}
