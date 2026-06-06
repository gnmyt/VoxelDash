package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.entities.OfflinePlayer;
import de.gnm.voxeldash.api.entities.players.PlayerProfile;
import de.gnm.voxeldash.api.helper.OfflinePlayerReader;
import de.gnm.voxeldash.api.pipes.players.ProfilePipe;
import de.gnm.voxeldash.util.BukkitUtil;
import de.gnm.voxeldash.util.VersionCompat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.UUID;

public class ProfilePipeImpl implements ProfilePipe {

    private OfflinePlayerReader reader;

    private OfflinePlayerReader reader() {
        if (reader == null) {
            reader = BukkitUtil.offlineReader();
        }
        return reader;
    }

    @Override
    public PlayerProfile getProfile(UUID uuid, boolean online) {
        if (online) {
            AtomicReference<PlayerProfile> ref = new AtomicReference<>();
            BukkitUtil.runOnMainThread(() -> {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null) {
                    return;
                }
                PlayerProfile profile = new PlayerProfile();
                profile.uuid = uuid.toString();
                profile.name = player.getName();
                profile.online = true;
                profile.health = player.getHealth();
                profile.foodLevel = player.getFoodLevel();
                profile.gamemode = player.getGameMode().name();
                profile.dimension = player.getWorld().getName();
                profile.op = player.isOp();
                profile.playtimeMillis = VersionCompat.playtimeMillis(player);
                try {
                    profile.firstJoin = player.getFirstPlayed();
                } catch (Throwable ignored) {
                }
                profile.banned = player.isBanned();
                profile.whitelisted = player.isWhitelisted();
                ref.set(profile);
            });
            PlayerProfile profile = ref.get();
            if (profile != null) {
                return profile;
            }
            // fall through to offline read if the player went offline meanwhile
        }

        String name = null;
        org.bukkit.OfflinePlayer bukkitOffline = Bukkit.getOfflinePlayer(uuid);
        if (bukkitOffline != null) {
            name = bukkitOffline.getName();
        }
        PlayerProfile profile = reader().readProfile(uuid, name);
        try {
            if (bukkitOffline != null) {
                profile.op = bukkitOffline.isOp();
                profile.banned = bukkitOffline.isBanned();
                profile.whitelisted = bukkitOffline.isWhitelisted();
                if (profile.firstJoin == 0) {
                    profile.firstJoin = bukkitOffline.getFirstPlayed();
                }
                if (profile.lastSeen == 0) {
                    profile.lastSeen = bukkitOffline.getLastPlayed();
                }
            }
        } catch (Throwable ignored) {
        }
        return profile;
    }

    @Override
    public ArrayList<OfflinePlayer> listAllPlayers() {
        return reader().listAllPlayers();
    }
}
