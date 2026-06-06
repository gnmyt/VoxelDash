package de.gnm.voxeldash.api.pipes.players;

import de.gnm.voxeldash.api.pipes.BasePipe;

public interface MessagePipe extends BasePipe {

    /**
     * Sends a private message to a player.
     *
     * @param playerName the recipient
     * @param message    the message text
     */
    void whisper(String playerName, String message);
}
