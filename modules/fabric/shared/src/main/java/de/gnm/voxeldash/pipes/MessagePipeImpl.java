package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.pipes.players.MessagePipe;
import de.gnm.voxeldash.util.FabricUtil;

public class MessagePipeImpl implements MessagePipe {

    @Override
    public void whisper(String playerName, String message) {
        FabricUtil.compat().whisper(playerName, message);
    }
}
