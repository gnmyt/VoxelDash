package de.gnm.voxeldash.util;

import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Bridges Bukkit APIs that differ across supported server versions (1.8.9 to latest).
 */
public final class VersionCompat {

    private static int datapackSupport = -1;

    private VersionCompat() {
    }

    public static boolean isHardcore(World world) {
        try {
            return world != null && world.isHardcore();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static long playtimeMillis(Player player) {
        try {
            return player.getStatistic(Statistic.PLAY_ONE_MINUTE) * 50L;
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    public static boolean supportsDatapacks() {
        if (datapackSupport == -1) {
            datapackSupport = atLeast(13) ? 1 : 0;
        }
        return datapackSupport == 1;
    }

    /**
     * Whether the server exposes the off-hand slot (1.9+).
     */
    public static boolean hasOffhand() {
        return atLeast(9);
    }

    private static boolean atLeast(int minor) {
        try {
            String[] parts = Bukkit.getBukkitVersion().split("-")[0].split("\\.");
            int major = Integer.parseInt(parts[0]);
            return major > 1 || (major == 1 && parts.length > 1 && Integer.parseInt(parts[1]) >= minor);
        } catch (Throwable ignored) {
            return true;
        }
    }
}
