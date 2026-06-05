package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.VoxelDashMod;
import de.gnm.voxeldash.api.entities.OnlinePlayer;
import de.gnm.voxeldash.api.pipes.players.OnlinePlayerPipe;
import de.gnm.voxeldash.util.ForgeUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;

public class OnlinePlayerPipeImpl implements OnlinePlayerPipe {

    @Override
    public ArrayList<OnlinePlayer> getOnlinePlayers() {
        ArrayList<OnlinePlayer> players = new ArrayList<>();
        MinecraftServer server = VoxelDashMod.getServer();

        if (server == null) {
            return players;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String ipAddress = player.getIpAddress();
            if (ipAddress != null) {
                if (ipAddress.startsWith("/")) {
                    ipAddress = ipAddress.substring(1);
                }
                if (ipAddress.contains(":")) {
                    ipAddress = ipAddress.split(":")[0];
                }
            } else {
                ipAddress = "Unknown";
            }

            long playtime = player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) * 50L;

            OnlinePlayer onlinePlayer = new OnlinePlayer(
                    player.getName().getString(),
                    player.getUUID(),
                    player.level().dimension().location().toString(),
                    ipAddress,
                    player.getHealth(),
                    player.getFoodData().getFoodLevel(),
                    server.getPlayerList().isOp(player.getGameProfile()),
                    player.gameMode.getGameModeForPlayer().getName().toUpperCase(),
                    playtime
            );

            players.add(onlinePlayer);
        }

        return players;
    }

    @Override
    public void kickPlayer(String playerName, String reason) {
        ForgeUtil.runOnMainThread(() -> {
            MinecraftServer server = VoxelDashMod.getServer();
            if (server == null) return;

            ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);
            if (player != null) {
                player.connection.disconnect(Component.literal(reason));
            }
        });
    }

    @Override
    public void setGamemode(String playerName, String gamemode) {
        ForgeUtil.runOnMainThread(() -> {
            MinecraftServer server = VoxelDashMod.getServer();
            if (server == null) return;

            ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);
            if (player != null) {
                try {
                    GameType mode = GameType.byName(gamemode.toLowerCase(), null);
                    if (mode != null) {
                        player.setGameMode(mode);
                    }
                } catch (Exception ignored) {
                }
            }
        });
    }

    @Override
    public void teleportToWorld(String playerName, String worldName) {
        ForgeUtil.runOnMainThread(() -> {
            MinecraftServer server = VoxelDashMod.getServer();
            if (server == null) return;

            ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);
            if (player == null) return;

            ServerLevel targetWorld = null;

            for (ServerLevel level : server.getAllLevels()) {
                String worldId = level.dimension().location().toString();
                if (worldId.equals(worldName) || worldId.endsWith(":" + worldName) ||
                        level.dimension().location().getPath().equals(worldName)) {
                    targetWorld = level;
                    break;
                }
            }

            if (targetWorld != null) {
                player.teleportTo(targetWorld,
                        targetWorld.getSharedSpawnPos().getX() + 0.5,
                        targetWorld.getSharedSpawnPos().getY(),
                        targetWorld.getSharedSpawnPos().getZ() + 0.5,
                        player.getYRot(),
                        player.getXRot());
            }
        });
    }
}
