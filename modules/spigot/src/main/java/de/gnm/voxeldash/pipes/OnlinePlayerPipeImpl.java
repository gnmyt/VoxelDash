package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.entities.OnlinePlayer;
import de.gnm.voxeldash.api.pipes.players.OnlinePlayerPipe;
import de.gnm.voxeldash.util.BukkitUtil;
import de.gnm.voxeldash.util.VersionCompat;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class OnlinePlayerPipeImpl implements OnlinePlayerPipe {

    @Override
    public ArrayList<OnlinePlayer> getOnlinePlayers() {
        ArrayList<OnlinePlayer> players = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            String ipAddress = "Unknown";
            if (player.getAddress() != null) {
                ipAddress = player.getAddress().getAddress().getHostAddress();
            }

            long playtime = VersionCompat.playtimeMillis(player);

            OnlinePlayer onlinePlayer = new OnlinePlayer(
                    player.getName(),
                    player.getUniqueId(),
                    player.getWorld().getName(),
                    ipAddress,
                    player.getHealth(),
                    player.getFoodLevel(),
                    player.isOp(),
                    player.getGameMode().name(),
                    playtime
            );

            players.add(onlinePlayer);
        }

        return players;
    }

    @Override
    public void kickPlayer(String playerName, String reason) {
        BukkitUtil.runOnMainThread(() -> {
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                player.kickPlayer(reason);
            }
        });
    }

    @Override
    public void setGamemode(String playerName, String gamemode) {
        BukkitUtil.runOnMainThread(() -> {
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                try {
                    GameMode mode = GameMode.valueOf(gamemode.toUpperCase());
                    player.setGameMode(mode);
                } catch (IllegalArgumentException ignored) {
                }
            }
        });
    }

    @Override
    public void teleportToWorld(String playerName, String worldName) {
        BukkitUtil.runOnMainThread(() -> {
            Player player = Bukkit.getPlayer(playerName);
            World world = Bukkit.getWorld(worldName);
            if (player != null && world != null) {
                player.teleport(world.getSpawnLocation());
            }
        });
    }

}
