package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.VoxelDashSpigot;
import de.gnm.voxeldash.api.controller.PlayerDataController;
import de.gnm.voxeldash.api.entities.players.MuteCapabilities;
import de.gnm.voxeldash.api.pipes.players.PunishmentPipe;
import de.gnm.voxeldash.util.BanCompat;
import de.gnm.voxeldash.util.BukkitUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Date;

public class PunishmentPipeImpl implements PunishmentPipe, Listener {

    @Override
    public MuteCapabilities getMuteCapabilities() {
        return new MuteCapabilities(true);
    }

    @Override
    public void tempBan(String playerName, String reason, long expiryMillis) {
        BukkitUtil.runOnMainThread(() -> {
            BanCompat.tempBan(playerName, reason, new Date(expiryMillis), "VoxelDash");
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                player.kickPlayer(reason);
            }
        });
    }

    @Override
    public void onMuted(String playerName, String reason, long expiryMillis) {
        if (playerName == null) {
            return;
        }
        BukkitUtil.runOnMainThread(() -> {
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                player.sendMessage("You have been muted: " + reason);
            }
        });
    }

    @Override
    public void onUnmuted(String playerName) {
        if (playerName == null) {
            return;
        }
        BukkitUtil.runOnMainThread(() -> {
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                player.sendMessage("You have been unmuted.");
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        try {
            PlayerDataController data = VoxelDashSpigot.getInstance().getLoader().getController(PlayerDataController.class);
            if (data != null && data.isMuted(event.getPlayer().getUniqueId())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("You are muted and cannot chat.");
            }
        } catch (Throwable ignored) {
        }
    }
}
