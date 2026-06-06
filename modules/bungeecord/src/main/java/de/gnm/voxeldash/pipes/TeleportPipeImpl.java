package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.entities.players.TeleportCapabilities;
import de.gnm.voxeldash.api.pipes.players.TeleportPipe;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class TeleportPipeImpl implements TeleportPipe {

    @Override
    public TeleportCapabilities getCapabilities() {
        return new TeleportCapabilities(false, false, false, true);
    }

    @Override
    public void teleportToCoords(String playerName, double x, double y, double z, String world) {
        // Not applicable on a proxy.
    }

    @Override
    public void teleportToPlayer(String playerName, String targetName) {
        // Not applicable on a proxy.
    }

    @Override
    public void teleportToSpawn(String playerName) {
        // Not applicable on a proxy.
    }

    @Override
    public void teleportToServer(String playerName, String serverName) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(playerName);
        ServerInfo info = ProxyServer.getInstance().getServerInfo(serverName);
        if (player != null && info != null) {
            player.connect(info);
        }
    }
}
