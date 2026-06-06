package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.entities.players.TeleportCapabilities;
import de.gnm.voxeldash.api.pipes.players.TeleportPipe;
import de.gnm.voxeldash.util.BukkitUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class TeleportPipeImpl implements TeleportPipe {

    @Override
    public TeleportCapabilities getCapabilities() {
        return new TeleportCapabilities(true, true, true, false);
    }

    @Override
    public void teleportToCoords(String playerName, double x, double y, double z, String world) {
        BukkitUtil.runOnMainThread(() -> {
            Player player = Bukkit.getPlayer(playerName);
            if (player == null) {
                return;
            }
            World target = world != null && !world.isEmpty() ? Bukkit.getWorld(world) : player.getWorld();
            if (target == null) {
                target = player.getWorld();
            }
            player.teleport(new Location(target, x, y, z));
        });
    }

    @Override
    public void teleportToPlayer(String playerName, String targetName) {
        BukkitUtil.runOnMainThread(() -> {
            Player player = Bukkit.getPlayer(playerName);
            Player target = Bukkit.getPlayer(targetName);
            if (player != null && target != null) {
                player.teleport(target.getLocation());
            }
        });
    }

    @Override
    public void teleportToSpawn(String playerName) {
        BukkitUtil.runOnMainThread(() -> {
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                player.teleport(player.getWorld().getSpawnLocation());
            }
        });
    }

    @Override
    public void teleportToServer(String playerName, String serverName) {
        // Not applicable on a single Bukkit server.
    }
}
