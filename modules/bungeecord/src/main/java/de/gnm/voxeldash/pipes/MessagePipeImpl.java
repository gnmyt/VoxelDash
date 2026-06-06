package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.pipes.players.MessagePipe;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class MessagePipeImpl implements MessagePipe {

    @Override
    public void whisper(String playerName, String message) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(playerName);
        if (player != null) {
            player.sendMessage(new TextComponent(message));
        }
    }
}
