package de.gnm.voxeldash.util;

import de.gnm.voxeldash.api.entities.BannedPlayer;
import de.gnm.voxeldash.api.entities.OfflinePlayer;
import de.gnm.voxeldash.api.entities.OnlinePlayer;
import de.gnm.voxeldash.api.entities.World;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import de.gnm.voxeldash.api.entities.players.InventoryView;
import de.gnm.voxeldash.api.entities.players.InventoryItem;

public interface FabricCompat {

    enum Weather {
        CLEAR, RAIN, THUNDER
    }

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

    /**
     * Enumerates the server's game rules with their current values. Implemented
     * per version using that line's mappings; lines that cannot enumerate them
     * fall back to this empty default (setting still works through the
     * {@code /gamerule} command).
     *
     * @return the current game rules (never {@code null})
     */
    default List<GameRuleEntry> gameRules() {
        return new ArrayList<>();
    }

    /**
     * Version-agnostic game-rule descriptor extracted from the live server.
     */
    final class GameRuleEntry {
        public final String key;
        public final String type;
        public final String value;

        public GameRuleEntry(String key, String type, String value) {
            this.key = key;
            this.type = type;
            this.value = value;
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

    File datapacksFolder();

    /**
     * Whether this line can read/edit the live inventories of online players.
     * Offline inventory viewing works regardless (it reads NBT off disk).
     */
    default boolean inventorySupported() {
        return false;
    }

    /**
     * Reads an online player's live inventory (main + armor + offhand). Versions
     * that support it override this; the default returns an empty view.
     */
    default InventoryView readInventory(UUID uuid) {
        return new InventoryView("inventory", true);
    }

    /**
     * Reads an online player's live ender chest.
     */
    default InventoryView readEnderChest(UUID uuid) {
        return new InventoryView("enderchest", true);
    }

    /**
     * Sets (or clears, when {@code item} is null) a slot of an online player.
     */
    default void setSlot(UUID uuid, boolean enderChest, int slot, InventoryItem item) {
    }

    /**
     * Gives an item to an online player.
     */
    default void giveItem(UUID uuid, String id, int count) {
    }

    /**
     * Swaps two slots of an online player's container, preserving full item data.
     */
    default void moveSlot(UUID uuid, boolean enderChest, int fromSlot, int toSlot) {
    }

    /**
     * Clears an online player's container.
     */
    default void clearInventory(UUID uuid, boolean enderChest) {
    }

    /**
     * Teleports a player to coordinates. The default uses the {@code /tp} command
     * (universal across versions); when a world is given it wraps it in
     * {@code execute in <world> run tp ...}.
     */
    default void teleportToCoords(String playerName, double x, double y, double z, String world) {
        String tp = "tp " + playerName + " " + x + " " + y + " " + z;
        runCommand(world != null && !world.isEmpty() ? "execute in " + world + " run " + tp : tp);
    }

    /**
     * Teleports a player to another player (default: the {@code /tp} command).
     */
    default void teleportToPlayer(String playerName, String targetName) {
        runCommand("tp " + playerName + " " + targetName);
    }

    /**
     * Teleports a player to the overworld spawn (default: reuse
     * {@link #teleportToWorld(String, String)}, which teleports to a world's spawn).
     */
    default void teleportToSpawn(String playerName) {
        teleportToWorld(playerName, "overworld");
    }

    /**
     * Sends a private message to a player (default: the {@code /tell} command).
     */
    default void whisper(String playerName, String message) {
        runCommand("tell " + playerName + " " + message);
    }

    /**
     * Temporarily bans a player until {@code expiryMillis}. The default falls back
     * to a permanent ban (no expiry); modern lines override to honour the expiry.
     */
    default void tempBan(String playerName, String reason, long expiryMillis) {
        banPlayer(playerName, reason);
    }

    /**
     * Whether this line can enforce mutes (needs a chat hook). Pre-1.19 Fabric
     * lines have no clean chat-allow callback and report false.
     */
    default boolean muteSupported() {
        return false;
    }

    /**
     * Registers a predicate the chat hook consults to suppress muted players'
     * messages. No-op where {@link #muteSupported()} is false.
     */
    default void registerMuteCheck(Predicate<UUID> mutedCheck) {
    }
}
