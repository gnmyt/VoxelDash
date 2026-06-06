package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.pipes.players.MessagePipe;
import de.gnm.voxeldash.util.BukkitUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class MessagePipeImpl implements MessagePipe {

    @Override
    public void whisper(String playerName, String message) {
        BukkitUtil.runOnMainThread(() -> {
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                player.sendMessage(message);
            }
        });
    }
}
