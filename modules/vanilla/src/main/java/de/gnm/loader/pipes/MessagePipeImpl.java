package de.gnm.loader.pipes;

import de.gnm.voxeldash.api.pipes.players.MessagePipe;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class MessagePipeImpl implements MessagePipe {

    private final BufferedWriter consoleWriter;

    public MessagePipeImpl(OutputStream console) {
        this.consoleWriter = new BufferedWriter(new OutputStreamWriter(console));
    }

    @Override
    public void whisper(String playerName, String message) {
        try {
            consoleWriter.write("tell " + playerName + " " + message + System.lineSeparator());
            consoleWriter.flush();
        } catch (IOException ignored) {
        }
    }
}
