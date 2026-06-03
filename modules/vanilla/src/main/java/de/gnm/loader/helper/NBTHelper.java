package de.gnm.loader.helper;

import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class NBTHelper {

    private static final Logger LOG = Logger.getLogger("VoxelDashVanilla");

    /**
     * Reads player data from the playerdata folder
     *
     * @param worldFolder The world folder (e.g., "world")
     * @param uuid        The player's UUID
     * @return The player's CompoundTag data, or null if not found
     */
    public static CompoundTag readPlayerData(File worldFolder, UUID uuid) {
        File playerDataFile = new File(worldFolder, "playerdata/" + uuid.toString() + ".dat");
        if (!playerDataFile.exists()) {
            return null;
        }

        try {
            NamedTag namedTag = NBTUtil.read(playerDataFile);
            if (namedTag.getTag() instanceof CompoundTag) {
                return (CompoundTag) namedTag.getTag();
            }
        } catch (IOException e) {
            LOG.error("Failed to read player data for " + uuid, e);
        }
        return null;
    }

    /**
     * Gets the player's health from NBT data
     *
     * @param playerData The player's CompoundTag
     * @return The health value (0-20), or 20 if not found
     */
    public static double getHealth(CompoundTag playerData) {
        if (playerData == null) return 20.0;
        try {
            if (playerData.containsKey("Health")) {
                return playerData.getFloat("Health");
            }
        } catch (Exception e) {
            LOG.debug("Failed to read health from player data", e);
        }
        return 20.0;
    }

    /**
     * Gets the player's food level from NBT data
     *
     * @param playerData The player's CompoundTag
     * @return The food level (0-20), or 20 if not found
     */
    public static int getFoodLevel(CompoundTag playerData) {
        if (playerData == null) return 20;
        try {
            if (playerData.containsKey("foodLevel")) {
                return playerData.getInt("foodLevel");
            }
        } catch (Exception e) {
            LOG.debug("Failed to read food level from player data", e);
        }
        return 20;
    }

    /**
     * Gets the player's game mode from NBT data
     *
     * @param playerData The player's CompoundTag
     * @return The game mode name (SURVIVAL, CREATIVE, etc.)
     */
    public static String getGameMode(CompoundTag playerData) {
        if (playerData == null) return "SURVIVAL";
        try {
            if (playerData.containsKey("playerGameType")) {
                int gameType = playerData.getInt("playerGameType");
                switch (gameType) {
                    case 1: return "CREATIVE";
                    case 2: return "ADVENTURE";
                    case 3: return "SPECTATOR";
                    default: return "SURVIVAL";
                }
            }
        } catch (Exception e) {
            LOG.debug("Failed to read game mode from player data", e);
        }
        return "SURVIVAL";
    }

    /**
     * Gets the player's dimension/world from NBT data
     *
     * @param playerData The player's CompoundTag
     * @return The dimension name (e.g., "minecraft:overworld")
     */
    public static String getDimension(CompoundTag playerData) {
        if (playerData == null) return "minecraft:overworld";
        try {
            if (playerData.containsKey("Dimension")) {
                return playerData.getString("Dimension");
            }
        } catch (Exception e) {
            LOG.debug("Failed to read dimension from player data", e);
        }
        return "minecraft:overworld";
    }

    /**
     * Reads the level.dat file to get world information
     *
     * @param worldFolder The world folder
     * @return The world's Data CompoundTag, or null if not found
     */
    public static CompoundTag readLevelData(File worldFolder) {
        File levelFile = new File(worldFolder, "level.dat");
        if (!levelFile.exists()) {
            return null;
        }

        try {
            NamedTag namedTag = NBTUtil.read(levelFile);
            if (namedTag.getTag() instanceof CompoundTag) {
                return ((CompoundTag) namedTag.getTag()).getCompoundTag("Data");
            }
        } catch (IOException e) {
            LOG.error("Failed to read level.dat for " + worldFolder.getName(), e);
        }
        return null;
    }

    /**
     * Gets the world time from level.dat
     *
     * @param levelData The world's Data CompoundTag
     * @return The time of day (0-24000)
     */
    public static long getWorldTime(CompoundTag levelData) {
        if (levelData == null) return 0;
        try {
            if (levelData.containsKey("DayTime")) {
                return levelData.getLong("DayTime") % 24000;
            }
        } catch (Exception e) {
            LOG.debug("Failed to read world time", e);
        }
        return 0;
    }

    /**
     * Gets whether it's raining from level.dat
     *
     * @param levelData The world's Data CompoundTag
     * @return true if raining
     */
    public static boolean isRaining(CompoundTag levelData) {
        if (levelData == null) return false;
        try {
            if (levelData.containsKey("raining")) {
                return levelData.getByte("raining") == 1;
            }
        } catch (Exception e) {
            LOG.debug("Failed to read rain status", e);
        }
        return false;
    }

    /**
     * Gets whether it's thundering from level.dat
     *
     * @param levelData The world's Data CompoundTag
     * @return true if thundering
     */
    public static boolean isThundering(CompoundTag levelData) {
        if (levelData == null) return false;
        try {
            if (levelData.containsKey("thundering")) {
                return levelData.getByte("thundering") == 1;
            }
        } catch (Exception e) {
            LOG.debug("Failed to read thunder status", e);
        }
        return false;
    }

    /**
     * Gets the difficulty from level.dat
     *
     * @param levelData The world's Data CompoundTag
     * @return The difficulty name (PEACEFUL, EASY, NORMAL, HARD)
     */
    public static String getDifficulty(CompoundTag levelData) {
        if (levelData == null) return "NORMAL";
        try {
            if (levelData.containsKey("Difficulty")) {
                int difficulty = levelData.getByte("Difficulty");
                switch (difficulty) {
                    case 0: return "PEACEFUL";
                    case 1: return "EASY";
                    case 3: return "HARD";
                    default: return "NORMAL";
                }
            }
        } catch (Exception e) {
            LOG.debug("Failed to read difficulty", e);
        }
        return "NORMAL";
    }

    /**
     * Gets the world seed from level.dat
     *
     * @param levelData The world's Data CompoundTag
     * @return The world seed
     */
    public static long getWorldSeed(CompoundTag levelData) {
        if (levelData == null) return 0;
        try {
            if (levelData.containsKey("WorldGenSettings")) {
                CompoundTag worldGenSettings = levelData.getCompoundTag("WorldGenSettings");
                if (worldGenSettings.containsKey("seed")) {
                    return worldGenSettings.getLong("seed");
                }
            }
            if (levelData.containsKey("RandomSeed")) {
                return levelData.getLong("RandomSeed");
            }
        } catch (Exception e) {
            LOG.debug("Failed to read world seed", e);
        }
        return 0;
    }

    /**
     * Gets whether hardcore mode is enabled from level.dat
     *
     * @param levelData The world's Data CompoundTag
     * @return true if hardcore mode is enabled
     */
    public static boolean isHardcore(CompoundTag levelData) {
        if (levelData == null) return false;
        try {
            if (levelData.containsKey("hardcore")) {
                return levelData.getByte("hardcore") == 1;
            }
        } catch (Exception e) {
            LOG.debug("Failed to read hardcore status", e);
        }
        return false;
    }

    /**
     * Gets the level name from level.dat
     *
     * @param levelData The world's Data CompoundTag
     * @return The level name
     */
    public static String getLevelName(CompoundTag levelData) {
        if (levelData == null) return "world";
        try {
            if (levelData.containsKey("LevelName")) {
                return levelData.getString("LevelName");
            }
        } catch (Exception e) {
            LOG.debug("Failed to read level name", e);
        }
        return "world";
    }
}
