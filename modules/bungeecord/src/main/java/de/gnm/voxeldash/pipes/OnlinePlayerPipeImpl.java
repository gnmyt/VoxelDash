package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.entities.OnlinePlayer;
import de.gnm.voxeldash.api.pipes.players.OnlinePlayerPipe;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;

public class OnlinePlayerPipeImpl implements OnlinePlayerPipe {

    @Override
    public ArrayList<OnlinePlayer> getOnlinePlayers() {
        ArrayList<OnlinePlayer> players = new ArrayList<>();

        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            String ipAddress = "Unknown";
            if (player.getSocketAddress() != null) {
                ipAddress = player.getSocketAddress().toString();
                if (ipAddress.startsWith("/")) {
                    ipAddress = ipAddress.substring(1);
                }
                if (ipAddress.contains(":")) {
                    ipAddress = ipAddress.substring(0, ipAddress.indexOf(":"));
                }
            }

            String serverName = "Unknown";
            if (player.getServer() != null && player.getServer().getInfo() != null) {
                serverName = player.getServer().getInfo().getName();
            }

            OnlinePlayer onlinePlayer = new OnlinePlayer(
                    player.getName(),
                    player.getUniqueId(),
                    serverName,
                    ipAddress,
                    20.0,
                    20,
                    false,
                    "UNKNOWN",
                    0L
            );

            players.add(onlinePlayer);
        }

        return players;
    }

    @Override
    public void kickPlayer(String playerName, String reason) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(playerName);
        if (player != null) {
            player.disconnect(new TextComponent(reason));
        }
    }

    @Override
    public void setGamemode(String playerName, String gamemode) {
        // Gamemode changes are not possible from the proxy
    }

    @Override
    public void teleportToWorld(String playerName, String worldName) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(playerName);
        if (player != null) {
            ServerInfo serverInfo = ProxyServer.getInstance().getServerInfo(worldName);
            if (serverInfo != null) {
                player.connect(serverInfo);
            }
        }
    }
}
