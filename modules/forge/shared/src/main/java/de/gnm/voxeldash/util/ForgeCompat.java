package de.gnm.voxeldash.util;

import de.gnm.voxeldash.api.entities.BannedPlayer;
import de.gnm.voxeldash.api.entities.OfflinePlayer;
import de.gnm.voxeldash.api.entities.OnlinePlayer;
import de.gnm.voxeldash.api.entities.World;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The single version-specific seam for a Forge line. Every Minecraft/Forge call
 * that differs between versions lives behind this interface, implemented once per
 * line by that line's {@code VersionCompat}. As a result the pipes, widgets and
 * listener in {@code shared/} contain NO {@code net.minecraft} imports and are
 * reused unchanged across every supported version (1.8.9 → 26.x).
 * <p>
 * Invariant: the only classes that may import {@code net.minecraft} are the
 * per-line {@code VersionCompat}, {@code VoxelDashMod}, the command shell, and
 * (where the logging API differs) {@code ConsoleListener}.
 * <p>
 * Written in Java 8 so the legacy lines (1.8.9/1.12.2/1.16.5) can compile it.
 */
public interface ForgeCompat {

    enum Weather {
        CLEAR, RAIN, THUNDER
    }

    /** Aggregate world counters for the overview widgets. */
    final class WorldStats {
        public final int worldCount;
        public final int entityCount;
        public final int loadedChunkCount;

        public WorldStats(int worldCount, int entityCount, int loadedChunkCount) {
            this.worldCount = worldCount;
            this.entityCount = entityCount;
            this.loadedChunkCount = loadedChunkCount;
        }
    }

    /** Version-agnostic mod descriptor (from the loader or a jar's metadata file). */
    final class ModEntry {
        public final String modId;
        public final String name;
        public final String version;
        public final String description;
        public final String[] authors;
        public final String sourceFileName;

        public ModEntry(String modId, String name, String version, String description, String[] authors, String sourceFileName) {
            this.modId = modId;
            this.name = name;
            this.version = version;
            this.description = description;
            this.authors = authors;
            this.sourceFileName = sourceFileName;
        }
    }

    void runOnMainThread(Runnable runnable);

    void runOnMainThreadAsync(Runnable runnable);

    String serverVersion();

    int serverPort();

    double currentTps();

    int onlinePlayerCount();

    int maxPlayers();

    void reloadServer();

    void stopServer();

    void runCommand(String command);

    void broadcast(String message);

    ArrayList<OnlinePlayer> onlinePlayers();

    void kickPlayer(String playerName, String reason);

    void kickAll(String reason);

    void setGamemode(String playerName, String gamemode);

    void teleportToWorld(String playerName, String worldId);

    ArrayList<OfflinePlayer> operators();

    void setOp(String playerName);

    void deOp(String playerName);

    boolean isOperator(String playerName);

    boolean whitelistEnabled();

    void setWhitelistEnabled(boolean enabled);

    ArrayList<OfflinePlayer> whitelistedPlayers();

    void whitelistAdd(String playerName);

    void whitelistRemove(String playerName);

    ArrayList<BannedPlayer> bannedPlayers();

    void banPlayer(String playerName, String reason);

    void unbanPlayer(String playerName);

    ArrayList<World> worlds();

    void setTime(String worldId, long ticks);

    void setWeather(String worldId, Weather weather);

    void setDifficulty(String worldId, String difficulty);

    void teleportPlayers(String fromWorldId, String toWorldId);

    void saveWorld(String worldId);

    WorldStats worldStats();

    List<ModEntry> loadedMods();

    ModEntry parseJarMeta(File jarFile);

    File modsFolder();

    File configDir();

    boolean supportsDatapacks();

    /** Null when {@link #supportsDatapacks()} is false. */
    File datapacksFolder();
}
