package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.VoxelDashBungee;
import de.gnm.voxeldash.api.controller.PlayerDataController;
import de.gnm.voxeldash.api.entities.players.MuteCapabilities;
import de.gnm.voxeldash.api.pipes.players.PunishmentPipe;
import de.gnm.voxeldash.manager.BanManager;
import de.gnm.voxeldash.util.BungeeUtil;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;

public class PunishmentPipeImpl implements PunishmentPipe, Listener {

    @Override
    public MuteCapabilities getMuteCapabilities() {
        return new MuteCapabilities(true);
    }

    @Override
    public void tempBan(String playerName, String reason, long expiryMillis) {
        BanManager manager = BanManager.getInstance();
        if (manager == null) {
            return;
        }
        UUID uuid = BungeeUtil.getPlayerUUID(playerName);
        manager.banPlayer(playerName, uuid, reason, "VoxelDash", expiryMillis);
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(playerName);
        if (player != null) {
            player.disconnect(new TextComponent("You have been banned: " + reason));
        }
    }

    @Override
    public void onMuted(String playerName, String reason, long expiryMillis) {
        notify(playerName, "You have been muted: " + reason);
    }

    @Override
    public void onUnmuted(String playerName) {
        notify(playerName, "You have been unmuted.");
    }

    private void notify(String playerName, String message) {
        if (playerName == null) {
            return;
        }
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(playerName);
        if (player != null) {
            player.sendMessage(new TextComponent(message));
        }
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        if (event.isCancelled() || event.isCommand() || event.isProxyCommand()) {
            return;
        }
        if (!(event.getSender() instanceof ProxiedPlayer)) {
            return;
        }
        ProxiedPlayer player = (ProxiedPlayer) event.getSender();
        try {
            PlayerDataController data = VoxelDashBungee.getInstance().getLoader().getController(PlayerDataController.class);
            if (data != null && data.isMuted(player.getUniqueId())) {
                event.setCancelled(true);
                player.sendMessage(new TextComponent("You are muted and cannot chat."));
            }
        } catch (Throwable ignored) {
        }
    }
}
