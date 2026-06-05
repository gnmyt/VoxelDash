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

            runServerCommand("time set " + ticks);
        });
    }

    @Override
    public void setWeather(String worldName, String weather) {
        ForgeUtil.runOnMainThread(() -> {
            ServerLevel world = findWorld(worldName);
            if (world == null) return;

            switch (weather.toLowerCase()) {
                case "clear":
                    runServerCommand("weather clear");
                    break;
                case "rain":
                    runServerCommand("weather rain");
                    break;
                case "thunder":
                    runServerCommand("weather thunder");
                    break;
            }
        });
    }

    private void runServerCommand(String command) {
        MinecraftServer server = VoxelDashMod.getServer();
        if (server == null) return;
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
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

            net.minecraft.core.BlockPos spawn = to.getRespawnData().pos();
            for (ServerPlayer player : new ArrayList<>(from.players())) {
                player.teleportTo(to,
                        spawn.getX() + 0.5,
                        spawn.getY(),
                        spawn.getZ() + 0.5,
                        java.util.Set.of(),
                        player.getYRot(),
                        player.getXRot(),
                        true);
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
            String worldId = level.dimension().identifier().toString();
            if (worldId.equals(worldName) || worldId.endsWith(":" + worldName) ||
                    level.dimension().identifier().getPath().equals(worldName)) {
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

        String environment = switch (level.dimension().identifier().getPath()) {
            case "the_nether" -> "NETHER";
            case "the_end" -> "THE_END";
            default -> "NORMAL";
        };

        return new World(
                level.dimension().identifier().toString(),
                environment,
                level.players().size(),
                level.getLevelData().getGameTime() % 24000L,
                weather,
                level.getDifficulty().getSerializedName().toUpperCase(),
                level.getSeed(),
                level.getLevelData().isHardcore(),
                "NORMAL"
        );
    }
}
