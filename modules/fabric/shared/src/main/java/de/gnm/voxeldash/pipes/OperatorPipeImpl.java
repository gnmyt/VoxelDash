package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.entities.OfflinePlayer;
import de.gnm.voxeldash.api.pipes.players.OperatorPipe;
import de.gnm.voxeldash.util.FabricUtil;

import java.util.ArrayList;

public class OperatorPipeImpl implements OperatorPipe {

    @Override
    public ArrayList<OfflinePlayer> getOperators() {
        return FabricUtil.compat().operators();
    }

    @Override
    public void setOp(String playerName) {
        FabricUtil.compat().setOp(playerName);
    }

    @Override
    public void deOp(String playerName) {
        FabricUtil.compat().deOp(playerName);
    }
}
