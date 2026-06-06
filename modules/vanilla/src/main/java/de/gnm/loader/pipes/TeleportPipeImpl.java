package de.gnm.loader.pipes;

import de.gnm.voxeldash.api.entities.players.TeleportCapabilities;
import de.gnm.voxeldash.api.pipes.players.TeleportPipe;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class TeleportPipeImpl implements TeleportPipe {

    private final BufferedWriter consoleWriter;

    public TeleportPipeImpl(OutputStream console) {
        this.consoleWriter = new BufferedWriter(new OutputStreamWriter(console));
    }

    private void send(String command) {
        try {
            consoleWriter.write(command + System.lineSeparator());
            consoleWriter.flush();
        } catch (IOException ignored) {
        }
    }

    @Override
    public TeleportCapabilities getCapabilities() {
        return new TeleportCapabilities(true, true, false, false);
    }

    @Override
    public void teleportToCoords(String playerName, double x, double y, double z, String world) {
        String tp = "tp " + playerName + " " + x + " " + y + " " + z;
        send(world != null && !world.isEmpty() ? "execute in " + world + " run " + tp : tp);
    }

    @Override
    public void teleportToPlayer(String playerName, String targetName) {
        send("tp " + playerName + " " + targetName);
    }

    @Override
    public void teleportToSpawn(String playerName) {
        // No vanilla console command teleports to the world spawn.
    }

    @Override
    public void teleportToServer(String playerName, String serverName) {
        // Not applicable on a single server.
    }
}
