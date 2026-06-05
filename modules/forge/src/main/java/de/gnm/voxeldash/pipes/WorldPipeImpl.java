package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.VoxelDashMod;
import de.gnm.voxeldash.api.entities.World;
import de.gnm.voxeldash.api.pipes.worlds.WorldPipe;
import de.gnm.voxeldash.util.ForgeUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;

import java.util.ArrayList;

public class WorldPipeImpl implements WorldPipe {

    @Override
    public ArrayList<World> getWorlds() {
        ArrayList<World> worlds = new ArrayList<>();
        MinecraftServer server = VoxelDashMod.getServer();

        if (server == null) {
            return worlds;
        }

        for (ServerLevel level : server.getAllLevels()) {
            worlds.add(convertWorld(level));
        }

        return worlds;
    }

    @Override
    public void setTime(String worldName, String time) {
        ForgeUtil.runOnMainThread(() -> {
            ServerLevel world = findWorld(worldName);
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

            world.setDayTime(ticks);
        });
    }

    @Override
    public void setWeather(String worldName, String weather) {
        ForgeUtil.runOnMainThread(() -> {
            ServerLevel world = findWorld(worldName);
            if (world == null) return;

            switch (weather.toLowerCase()) {
                case "clear":
                    world.setWeatherParameters(6000, 0, false, false);
                    break;
                case "rain":
                    world.setWeatherParameters(0, 6000, true, false);
                    break;
                case "thunder":
                    world.setWeatherParameters(0, 6000, true, true);
                    break;
            }
        });
    }

    @Override
    public void setDifficulty(String worldName, String difficulty) {
        ForgeUtil.runOnMainThread(() -> {
            MinecraftServer server = VoxelDashMod.getServer();
            if (server == null) return;

            try {
                Difficulty diff = Difficulty.byName(difficulty.toLowerCase());
                if (diff != null) {
                    server.setDifficulty(diff, true);
                }
            } catch (Exception ignored) {
            }
        });
    }

    @Override
    public boolean createWorld(String worldName, String environment, String worldType, String seed) {
        return false;
    }

    @Override
    public boolean deleteWorld(String worldName) {
        return false;
    }

    @Override
    public void teleportPlayers(String fromWorld, String toWorld) {
        ForgeUtil.runOnMainThread(() -> {
            ServerLevel from = findWorld(fromWorld);
            ServerLevel to = findWorld(toWorld);

            if (from == null || to == null) return;

            for (ServerPlayer player : new ArrayList<>(from.players())) {
                player.teleportTo(to,
                        to.getSharedSpawnPos().getX() + 0.5,
                        to.getSharedSpawnPos().getY(),
                        to.getSharedSpawnPos().getZ() + 0.5,
                        player.getYRot(),
                        player.getXRot());
            }
        });
    }

    @Override
    public void saveWorld(String worldName) {
        ForgeUtil.runOnMainThread(() -> {
            ServerLevel world = findWorld(worldName);
            if (world != null) {
                world.save(null, false, false);
            }
        });
    }

    private ServerLevel findWorld(String worldName) {
        MinecraftServer server = VoxelDashMod.getServer();
        if (server == null) return null;

        for (ServerLevel level : server.getAllLevels()) {
            String worldId = level.dimension().location().toString();
            if (worldId.equals(worldName) || worldId.endsWith(":" + worldName) ||
                    level.dimension().location().getPath().equals(worldName)) {
                return level;
            }
        }
        return null;
    }

    private World convertWorld(ServerLevel level) {
        String weather;
        if (level.isThundering()) {
            weather = "THUNDER";
        } else if (level.isRaining()) {
            weather = "RAIN";
        } else {
            weather = "CLEAR";
        }

        String environment = switch (level.dimension().location().getPath()) {
            case "the_nether" -> "NETHER";
            case "the_end" -> "THE_END";
            default -> "NORMAL";
        };

        return new World(
                level.dimension().location().toString(),
                environment,
                level.players().size(),
                level.getDayTime(),
                weather,
                level.getDifficulty().getKey().toUpperCase(),
                level.getSeed(),
                level.getLevelData().isHardcore(),
                "NORMAL"
        );
    }
}
