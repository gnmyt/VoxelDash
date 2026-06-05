package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.entities.World;
import de.gnm.voxeldash.api.pipes.worlds.WorldPipe;
import de.gnm.voxeldash.util.BukkitUtil;
import de.gnm.voxeldash.util.VersionCompat;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class WorldPipeImpl implements WorldPipe {

    @Override
    public ArrayList<World> getWorlds() {
        ArrayList<World> worlds = new ArrayList<>();

        for (org.bukkit.World world : Bukkit.getWorlds()) {
            worlds.add(convertWorld(world));
        }

        return worlds;
    }

    @Override
    public void setTime(String worldName, String time) {
        BukkitUtil.runOnMainThread(() -> {
            org.bukkit.World world = Bukkit.getWorld(worldName);
            if (world == null) return;

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

            world.setTime(ticks);
        });
    }

    @Override
    public void setWeather(String worldName, String weather) {
        BukkitUtil.runOnMainThread(() -> {
            org.bukkit.World world = Bukkit.getWorld(worldName);
            if (world == null) return;

            switch (weather.toLowerCase()) {
                case "clear":
                    world.setStorm(false);
                    world.setThundering(false);
                    break;
                case "rain":
                    world.setStorm(true);
                    world.setThundering(false);
                    break;
                case "thunder":
                    world.setStorm(true);
                    world.setThundering(true);
                    break;
            }
        });
    }

    @Override
    public void setDifficulty(String worldName, String difficulty) {
        BukkitUtil.runOnMainThread(() -> {
            org.bukkit.World world = Bukkit.getWorld(worldName);
            if (world == null) return;

            try {
                Difficulty diff = Difficulty.valueOf(difficulty.toUpperCase());
                world.setDifficulty(diff);
            } catch (IllegalArgumentException ignored) {
            }
        });
    }

    @Override
    public boolean createWorld(String worldName, String environment, String worldType, String seed) {
        AtomicBoolean success = new AtomicBoolean(false);

        BukkitUtil.runOnMainThread(() -> {
            try {
                WorldCreator creator = new WorldCreator(worldName);

                try {
                    org.bukkit.World.Environment env = org.bukkit.World.Environment.valueOf(environment.toUpperCase());
                    creator.environment(env);
                } catch (IllegalArgumentException e) {
                    creator.environment(org.bukkit.World.Environment.NORMAL);
                }

                try {
                    WorldType type = WorldType.valueOf(worldType.toUpperCase());
                    creator.type(type);
                } catch (IllegalArgumentException e) {
                    creator.type(WorldType.NORMAL);
                }

                if (seed != null && !seed.isEmpty()) {
                    try {
                        creator.seed(Long.parseLong(seed));
                    } catch (NumberFormatException e) {
                        creator.seed(seed.hashCode());
                    }
                }

                org.bukkit.World world = creator.createWorld();
                success.set(world != null);
            } catch (Exception e) {
                success.set(false);
            }
        });

        return success.get();
    }

    @Override
    public boolean deleteWorld(String worldName) {
        AtomicBoolean success = new AtomicBoolean(false);

        BukkitUtil.runOnMainThread(() -> {
            org.bukkit.World world = Bukkit.getWorld(worldName);
            if (world == null) {
                success.set(false);
                return;
            }

            org.bukkit.World mainWorld = Bukkit.getWorlds().get(0);
            if (mainWorld.equals(world)) {
                success.set(false);
                return;
            }

            for (Player player : world.getPlayers()) {
                player.teleport(mainWorld.getSpawnLocation());
            }

            File worldFolder = world.getWorldFolder();
            boolean unloaded = Bukkit.unloadWorld(world, false);

            if (unloaded) {
                success.set(deleteFolder(worldFolder));
            } else {
                success.set(false);
            }
        });

        return success.get();
    }

    @Override
    public void teleportPlayers(String fromWorld, String toWorld) {
        BukkitUtil.runOnMainThread(() -> {
            org.bukkit.World from = Bukkit.getWorld(fromWorld);
            org.bukkit.World to = Bukkit.getWorld(toWorld);

            if (from == null || to == null) return;

            for (Player player : from.getPlayers()) {
                player.teleport(to.getSpawnLocation());
            }
        });
    }

    @Override
    public void saveWorld(String worldName) {
        BukkitUtil.runOnMainThread(() -> {
            org.bukkit.World world = Bukkit.getWorld(worldName);
            if (world != null) {
                world.save();
            }
        });
    }

    /**
     * Converts a Bukkit World to an API World entity
     */
    private World convertWorld(org.bukkit.World world) {
        String weather;
        if (world.isThundering()) {
            weather = "THUNDER";
        } else if (world.hasStorm()) {
            weather = "RAIN";
        } else {
            weather = "CLEAR";
        }

        String worldType = "NORMAL";

        return new World(
                world.getName(),
                world.getEnvironment().name(),
                world.getPlayers().size(),
                world.getTime(),
                weather,
                world.getDifficulty().name(),
                world.getSeed(),
                VersionCompat.isHardcore(world),
                worldType
        );
    }

    /**
     * Recursively deletes a folder and its contents
     */
    private boolean deleteFolder(File folder) {
        if (folder == null || !folder.exists()) {
            return true;
        }

        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFolder(file);
                } else {
                    file.delete();
                }
            }
        }

        return folder.delete();
    }
}
