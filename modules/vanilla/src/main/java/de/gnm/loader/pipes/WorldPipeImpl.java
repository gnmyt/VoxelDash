package de.gnm.loader.pipes;

import de.gnm.loader.helper.NBTHelper;
import de.gnm.voxeldash.api.entities.World;
import de.gnm.voxeldash.api.helper.PropertyHelper;
import de.gnm.voxeldash.api.pipes.worlds.WorldPipe;
import net.querz.nbt.tag.CompoundTag;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class WorldPipeImpl implements WorldPipe {

    private static final Logger LOG = Logger.getLogger("VoxelDashVanilla");

    private final BufferedWriter consoleWriter;
    private final File serverRoot;

    public WorldPipeImpl(OutputStream console, File serverRoot) {
        this.consoleWriter = new BufferedWriter(new OutputStreamWriter(console));
        this.serverRoot = serverRoot;
    }

    @Override
    public ArrayList<World> getWorlds() {
        ArrayList<World> worlds = new ArrayList<>();

        String mainWorldName = getMainWorldName();
        File mainWorldFolder = new File(serverRoot, mainWorldName);

        if (mainWorldFolder.exists()) {
            World overworld = parseWorld(mainWorldFolder, "NORMAL");
            if (overworld != null) {
                worlds.add(overworld);
            }
        }

        File netherFolder = new File(mainWorldFolder, "DIM-1");
        if (netherFolder.exists()) {
            World nether = parseNetherWorld(mainWorldFolder, netherFolder);
            if (nether != null) {
                worlds.add(nether);
            }
        }

        File endFolder = new File(mainWorldFolder, "DIM1");
        if (endFolder.exists()) {
            World end = parseEndWorld(mainWorldFolder, endFolder);
            if (end != null) {
                worlds.add(end);
            }
        }

        return worlds;
    }

    /**
     * Parses the main world from its level.dat
     */
    private World parseWorld(File worldFolder, String environment) {
        CompoundTag levelData = NBTHelper.readLevelData(worldFolder);
        if (levelData == null) {
            return null;
        }

        String name = NBTHelper.getLevelName(levelData);
        long time = NBTHelper.getWorldTime(levelData);
        String weather = getWeather(levelData);
        String difficulty = NBTHelper.getDifficulty(levelData);
        long seed = NBTHelper.getWorldSeed(levelData);
        boolean hardcore = NBTHelper.isHardcore(levelData);

        int playerCount = 0;

        return new World(
                name,
                environment,
                playerCount,
                time,
                weather,
                difficulty,
                seed,
                hardcore,
                "NORMAL"
        );
    }

    /**
     * Parses the nether dimension
     */
    private World parseNetherWorld(File mainWorldFolder, File netherFolder) {
        CompoundTag levelData = NBTHelper.readLevelData(mainWorldFolder);
        if (levelData == null) {
            return null;
        }

        String name = NBTHelper.getLevelName(levelData) + "_nether";
        long time = NBTHelper.getWorldTime(levelData);
        String difficulty = NBTHelper.getDifficulty(levelData);
        long seed = NBTHelper.getWorldSeed(levelData);
        boolean hardcore = NBTHelper.isHardcore(levelData);

        return new World(
                name,
                "NETHER",
                0,
                time,
                "CLEAR",
                difficulty,
                seed,
                hardcore,
                "NORMAL"
        );
    }

    /**
     * Parses the end dimension
     */
    private World parseEndWorld(File mainWorldFolder, File endFolder) {
        CompoundTag levelData = NBTHelper.readLevelData(mainWorldFolder);
        if (levelData == null) {
            return null;
        }

        String name = NBTHelper.getLevelName(levelData) + "_the_end";
        long time = NBTHelper.getWorldTime(levelData);
        String difficulty = NBTHelper.getDifficulty(levelData);
        long seed = NBTHelper.getWorldSeed(levelData);
        boolean hardcore = NBTHelper.isHardcore(levelData);

        return new World(
                name,
                "THE_END",
                0,
                time,
                "CLEAR",
                difficulty,
                seed,
                hardcore,
                "NORMAL"
        );
    }

    /**
     * Gets the weather string from level data
     */
    private String getWeather(CompoundTag levelData) {
        if (NBTHelper.isThundering(levelData)) {
            return "THUNDER";
        } else if (NBTHelper.isRaining(levelData)) {
            return "RAIN";
        }
        return "CLEAR";
    }

    @Override
    public void setTime(String worldName, String time) {
        try {
            long ticks;
            switch (time.toLowerCase()) {
                case "day":
                    ticks = 1000;
                    break;
                case "noon":
                    ticks = 6000;
                    break;
                case "sunset":
                    ticks = 12000;
                    break;
                case "night":
                    ticks = 13000;
                    break;
                case "midnight":
                    ticks = 18000;
                    break;
                case "sunrise":
                    ticks = 23000;
                    break;
                default:
                    try {
                        ticks = Long.parseLong(time);
                    } catch (NumberFormatException e) {
                        return;
                    }
            }

            consoleWriter.write("time set " + ticks + System.lineSeparator());
            consoleWriter.flush();
        } catch (Exception e) {
            LOG.error("Failed to set time to " + time, e);
        }
    }

    @Override
    public void setWeather(String worldName, String weather) {
        try {
            String command;
            switch (weather.toLowerCase()) {
                case "clear": command = "weather clear"; break;
                case "rain": command = "weather rain"; break;
                case "thunder": command = "weather thunder"; break;
                default: command = null; break;
            }

            if (command != null) {
                consoleWriter.write(command + System.lineSeparator());
                consoleWriter.flush();
            }
        } catch (Exception e) {
            LOG.error("Failed to set weather to " + weather, e);
        }
    }

    @Override
    public void setDifficulty(String worldName, String difficulty) {
        try {
            String diff = difficulty.toLowerCase();
            if (diff.equals("peaceful") || diff.equals("easy") || diff.equals("normal") || diff.equals("hard")) {
                consoleWriter.write("difficulty " + diff + System.lineSeparator());
                consoleWriter.flush();
            }
        } catch (Exception e) {
            LOG.error("Failed to set difficulty to " + difficulty, e);
        }
    }

    @Override
    public boolean createWorld(String worldName, String environment, String worldType, String seed) {
        LOG.warn("World creation is not supported on vanilla servers");
        return false;
    }

    @Override
    public boolean deleteWorld(String worldName) {
        LOG.warn("World deletion is not supported on running vanilla servers");
        return false;
    }

    @Override
    public void teleportPlayers(String fromWorld, String toWorld) {
        try {
            String toDimension = worldNameToDimension(toWorld);
            String fromDimension = worldNameToDimension(fromWorld);

            String tpCommand = "execute as @a[dimension=" + fromDimension + "] in " + toDimension + " run tp @s ~ ~ ~";

            consoleWriter.write(tpCommand + System.lineSeparator());
            consoleWriter.flush();
        } catch (Exception e) {
            LOG.error("Failed to teleport players from " + fromWorld + " to " + toWorld, e);
        }
    }

    @Override
    public void saveWorld(String worldName) {
        try {
            consoleWriter.write("save-all" + System.lineSeparator());
            consoleWriter.flush();
        } catch (Exception e) {
            LOG.error("Failed to save world", e);
        }
    }

    /**
     * Gets the main world name from server.properties
     */
    private String getMainWorldName() {
        String levelName = PropertyHelper.getProperty("level-name");
        return levelName != null ? levelName : "world";
    }

    /**
     * Converts a world name to a Minecraft dimension identifier
     */
    private String worldNameToDimension(String worldName) {
        if (worldName == null) return "minecraft:overworld";

        String lower = worldName.toLowerCase();
        if (lower.contains("nether")) {
            return "minecraft:the_nether";
        } else if (lower.contains("end")) {
            return "minecraft:the_end";
        }
        return "minecraft:overworld";
    }
}
