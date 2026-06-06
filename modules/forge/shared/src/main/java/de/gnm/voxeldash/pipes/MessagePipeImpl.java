package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.pipes.players.MessagePipe;
import de.gnm.voxeldash.util.ForgeUtil;

public class MessagePipeImpl implements MessagePipe {

    @Override
    public void whisper(String playerName, String message) {
        ForgeUtil.compat().whisper(playerName, message);
    }
}
