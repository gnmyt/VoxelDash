package de.gnm.voxeldash.api.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gnm.voxeldash.api.entities.OfflinePlayer;
import de.gnm.voxeldash.api.entities.players.InventoryItem;
import de.gnm.voxeldash.api.entities.players.InventoryView;
import de.gnm.voxeldash.api.entities.players.PlayerProfile;
import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OfflinePlayerReader {

    private static final Logger LOG = Logger.getLogger("VoxelDash");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final File serverRoot;

    public OfflinePlayerReader(File serverRoot) {
        this.serverRoot = serverRoot;
    }

    /**
     * Parses a single NBT item compound into the normalized {@link InventoryItem},
     * tolerating the pre-1.13 / 1.13-1.20.4 / 1.20.5+ layouts.
     */
    public static InventoryItem parseItem(CompoundTag itemTag) {
        try {
            InventoryItem item = new InventoryItem();
            item.slot = intOf(itemTag, "Slot", 0);

            Tag<?> idTag = itemTag.get("id");
            if (idTag instanceof StringTag) {
                item.id = ((StringTag) idTag).getValue();
            } else if (idTag instanceof NumberTag) {
                item.id = "minecraft:legacy_" + ((NumberTag<?>) idTag).asInt();
            } else {
                return null;
            }

            if (itemTag.containsKey("count")) {
                item.count = intOf(itemTag, "count", 1);
            } else {
                item.count = intOf(itemTag, "Count", 1);
            }

            CompoundTag components = itemTag.containsKey("components") ? itemTag.getCompoundTag("components") : null;
            CompoundTag tag = itemTag.containsKey("tag") ? itemTag.getCompoundTag("tag") : null;

            if (components != null && components.containsKey("minecraft:damage")) {
                item.damage = intOf(components, "minecraft:damage", 0);
            } else if (tag != null && tag.containsKey("Damage")) {
                item.damage = intOf(tag, "Damage", 0);
            }
            if (components != null && components.containsKey("minecraft:max_damage")) {
                item.maxDamage = intOf(components, "minecraft:max_damage", 0);
            }

            item.enchanted =
                    (components != null && (components.containsKey("minecraft:enchantments") || components.containsKey("minecraft:stored_enchantments")))
                            || (tag != null && (tag.containsKey("Enchantments") || tag.containsKey("ench") || tag.containsKey("StoredEnchantments")));

            String rawName = null;
            if (components != null && components.containsKey("minecraft:custom_name")) {
                rawName = components.getString("minecraft:custom_name");
            } else if (tag != null && tag.containsKey("display")) {
                CompoundTag display = tag.getCompoundTag("display");
                if (display != null && display.containsKey("Name")) {
                    rawName = display.getString("Name");
                }
            }
            item.name = stripJsonText(rawName);

            return item;
        } catch (Exception e) {
            LOG.log(Level.FINE, "Could not parse item", e);
            return null;
        }
    }

    private static int intOf(CompoundTag tag, String key, int fallback) {
        Tag<?> t = tag.get(key);
        if (t instanceof NumberTag) {
            return ((NumberTag<?>) t).asInt();
        }
        return fallback;
    }

    private static long longOf(CompoundTag tag, String key, long fallback) {
        Tag<?> t = tag.get(key);
        if (t instanceof NumberTag) {
            return ((NumberTag<?>) t).asLong();
        }
        return fallback;
    }

    private static float floatOf(CompoundTag tag, String key, float fallback) {
        Tag<?> t = tag.get(key);
        if (t instanceof NumberTag) {
            return ((NumberTag<?>) t).asFloat();
        }
        return fallback;
    }

    private static String gameModeName(int type) {
        switch (type) {
            case 1:
                return "CREATIVE";
            case 2:
                return "ADVENTURE";
            case 3:
                return "SPECTATOR";
            default:
                return "SURVIVAL";
        }
    }

    /**
     * Item display names are stored as chat-component JSON (modern) or plain text
     * (legacy). Extract a plain-text best effort; fall back to the raw string.
     */
    private static String stripJsonText(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.startsWith("{") || trimmed.startsWith("[") || trimmed.startsWith("\"")) {
            try {
                JsonNode node = MAPPER.readTree(trimmed);
                String text = extractText(node);
                if (text != null && !text.isEmpty()) {
                    return text;
                }
            } catch (Exception ignored) {
            }
        }
        return trimmed;
    }

    private static String extractText(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        StringBuilder sb = new StringBuilder();
        if (node.has("text")) {
            sb.append(node.get("text").asText());
        }
        if (node.has("extra")) {
            for (JsonNode child : node.get("extra")) {
                String childText = extractText(child);
                if (childText != null) {
                    sb.append(childText);
                }
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    /**
     * The main world folder (server root + {@code level-name} from
     * server.properties, defaulting to {@code world}). Player data, stats and
     * advancements live here.
     */
    public File getWorldFolder() {
        String levelName = "world";
        File props = new File(serverRoot, "server.properties");
        if (props.exists()) {
            try {
                Properties p = new Properties();
                try (FileInputStream in = new FileInputStream(props)) {
                    p.load(in);
                }
                String configured = p.getProperty("level-name");
                if (configured != null && !configured.trim().isEmpty()) {
                    levelName = configured.trim();
                }
            } catch (Exception e) {
                LOG.log(Level.FINE, "Could not read level-name from server.properties", e);
            }
        }
        return new File(serverRoot, levelName);
    }

    private File playerDataFile(UUID uuid) {
        return new File(getWorldFolder(), "playerdata/" + uuid + ".dat");
    }

    /**
     * Reads usercache.json into a UUID -> last-known-name map (empty on failure).
     */
    public Map<UUID, String> readUserCache() {
        Map<UUID, String> cache = new HashMap<>();
        File file = new File(serverRoot, "usercache.json");
        if (!file.exists()) {
            return cache;
        }
        try {
            JsonNode root = MAPPER.readTree(file);
            if (root != null && root.isArray()) {
                for (JsonNode entry : root) {
                    JsonNode uuid = entry.get("uuid");
                    JsonNode name = entry.get("name");
                    if (uuid != null && name != null) {
                        try {
                            cache.put(UUID.fromString(uuid.asText()), name.asText());
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "Could not read usercache.json", e);
        }
        return cache;
    }

    /**
     * Every player that has ever joined: the union of on-disk player data files
     * and the user cache. Names come from the user cache where available.
     */
    public ArrayList<OfflinePlayer> listAllPlayers() {
        Map<UUID, String> cache = readUserCache();
        Map<UUID, String> all = new LinkedHashMap<>(cache);

        File playerData = new File(getWorldFolder(), "playerdata");
        File[] files = playerData.listFiles((dir, name) -> name.endsWith(".dat"));
        if (files != null) {
            for (File f : files) {
                String base = f.getName().substring(0, f.getName().length() - 4);
                try {
                    UUID uuid = UUID.fromString(base);
                    if (!all.containsKey(uuid)) {
                        all.put(uuid, null);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        ArrayList<OfflinePlayer> players = new ArrayList<>();
        for (Map.Entry<UUID, String> e : all.entrySet()) {
            String name = e.getValue() != null ? e.getValue() : e.getKey().toString();
            players.add(new OfflinePlayer(name, e.getKey()));
        }
        return players;
    }

    /**
     * Reads a player's NBT compound, or null if there is no data on disk.
     */
    public CompoundTag readPlayerData(UUID uuid) {
        File file = playerDataFile(uuid);
        if (!file.exists()) {
            return null;
        }
        try {
            NamedTag named = NBTUtil.read(file);
            if (named.getTag() instanceof CompoundTag) {
                return (CompoundTag) named.getTag();
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "Could not read player data for " + uuid, e);
        }
        return null;
    }

    public boolean hasPlayerData(UUID uuid) {
        return playerDataFile(uuid).exists();
    }

    /**
     * Reads the main inventory (including armor and offhand) of an offline player.
     */
    public InventoryView readInventory(UUID uuid) {
        return readContainer(uuid, "Inventory", "inventory");
    }

    /**
     * Reads the ender chest of an offline player.
     */
    public InventoryView readEnderChest(UUID uuid) {
        return readContainer(uuid, "EnderItems", "enderchest");
    }

    private InventoryView readContainer(UUID uuid, String nbtKey, String type) {
        InventoryView view = new InventoryView(type, false);
        CompoundTag data = readPlayerData(uuid);
        if (data == null || !data.containsKey(nbtKey)) {
            return view;
        }
        try {
            ListTag<CompoundTag> list = data.getListTag(nbtKey).asCompoundTagList();
            for (CompoundTag itemTag : list) {
                InventoryItem item = parseItem(itemTag);
                if (item != null) {
                    view.items.add(item);
                }
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "Could not parse " + nbtKey + " for " + uuid, e);
        }
        return view;
    }

    public PlayerProfile readProfile(UUID uuid, String knownName) {
        PlayerProfile profile = new PlayerProfile();
        profile.uuid = uuid.toString();
        profile.online = false;

        String name = knownName;
        if (name == null) {
            name = readUserCache().get(uuid);
        }
        profile.name = name != null ? name : uuid.toString();

        CompoundTag data = readPlayerData(uuid);
        if (data != null) {
            profile.health = floatOf(data, "Health", 20.0f);
            profile.foodLevel = intOf(data, "foodLevel", 20);
            profile.gamemode = gameModeName(intOf(data, "playerGameType", 0));
            if (data.containsKey("Dimension")) {
                Tag<?> dim = data.get("Dimension");
                profile.dimension = dim instanceof StringTag ? ((StringTag) dim).getValue() : "minecraft:overworld";
            }

            if (data.containsKey("bukkit")) {
                CompoundTag bukkit = data.getCompoundTag("bukkit");
                profile.firstJoin = longOf(bukkit, "firstPlayed", 0);
                profile.lastSeen = longOf(bukkit, "lastPlayed", 0);
            }
        }

        if (profile.lastSeen == 0) {
            File f = playerDataFile(uuid);
            if (f.exists()) {
                profile.lastSeen = f.lastModified();
            }
        }

        profile.playtimeMillis = readPlaytimeMillis(uuid);
        return profile;
    }

    /**
     * Reads total playtime in milliseconds from stats/&lt;uuid&gt;.json, handling
     * the modern {@code minecraft:play_time}, the 1.13-1.16 {@code play_one_minute}
     * and the pre-1.13 flat {@code stat.playOneMinute} keys. Values are in ticks
     * (20 ticks = 1 second = 50 ms).
     */
    public long readPlaytimeMillis(UUID uuid) {
        File file = new File(getWorldFolder(), "stats/" + uuid + ".json");
        if (!file.exists()) {
            return 0;
        }
        try {
            JsonNode root = MAPPER.readTree(file);
            if (root == null) {
                return 0;
            }
            JsonNode stats = root.has("stats") ? root.get("stats") : root;
            JsonNode custom = stats.get("minecraft:custom");
            if (custom != null) {
                JsonNode ticks = custom.get("minecraft:play_time");
                if (ticks == null) {
                    ticks = custom.get("minecraft:play_one_minute");
                }
                if (ticks != null) {
                    return ticks.asLong() * 50L;
                }
            }
            if (root.has("stat.playOneMinute")) {
                return root.get("stat.playOneMinute").asLong() * 50L;
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "Could not read playtime for " + uuid, e);
        }
        return 0;
    }
}
