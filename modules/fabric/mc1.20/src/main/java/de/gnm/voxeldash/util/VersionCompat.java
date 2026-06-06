package de.gnm.voxeldash.util;

import com.mojang.authlib.GameProfile;
import de.gnm.voxeldash.VoxelDashMod;
import de.gnm.voxeldash.api.entities.BannedPlayer;
import de.gnm.voxeldash.api.entities.OfflinePlayer;
import de.gnm.voxeldash.api.entities.OnlinePlayer;
import de.gnm.voxeldash.api.entities.World;
import net.minecraft.server.BannedPlayerEntry;
import net.minecraft.server.BannedPlayerList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.OperatorList;
import net.minecraft.server.Whitelist;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class VersionCompat implements FabricCompat {

    private static MinecraftServer server() {
        return VoxelDashMod.getServer();
    }

    @Override
    public void runOnMainThread(Runnable runnable) {
        MinecraftServer server = server();
        if (server == null || server.isOnThread()) {
            runnable.run();
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        server.execute(() -> {
            try {
                runnable.run();
            } finally {
                latch.countDown();
            }
        });
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public void runOnMainThreadAsync(Runnable runnable) {
        MinecraftServer server = server();
        if (server == null || server.isOnThread()) {
            runnable.run();
        } else {
            server.execute(runnable);
        }
    }

    @Override
    public String serverVersion() {
        return FabricPlatform.minecraftVersion();
    }

    @Override
    public int serverPort() {
        MinecraftServer s = server();
        return s != null ? s.getServerPort() : 25565;
    }

    @Override
    public double currentTps() {
        MinecraftServer s = server();
        if (s == null) return 20.0;
        try {
            float mspt = s.getTickTime();
            if (mspt <= 0.0f) return 20.0;
            return Math.min(20.0, 1000.0 / mspt);
        } catch (Throwable t) {
            return 20.0;
        }
    }

    @Override
    public int onlinePlayerCount() {
        MinecraftServer s = server();
        return s != null ? s.getCurrentPlayerCount() : 0;
    }

    @Override
    public int maxPlayers() {
        MinecraftServer s = server();
        return s != null ? s.getMaxPlayerCount() : 0;
    }

    @Override
    public void reloadServer() {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s != null) s.reloadResources(s.getDataPackManager().getEnabledNames());
        });
    }

    @Override
    public void stopServer() {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s != null) s.stop(false);
        });
    }

    @Override
    public void runCommand(String command) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            String clean = command.startsWith("/") ? command.substring(1) : command;
            s.getCommandManager().executeWithPrefix(s.getCommandSource(), clean);
        });
    }

    @Override
    public void broadcast(String message) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s != null) s.getPlayerManager().broadcast(Text.literal(message), false);
        });
    }

    @Override
    public ArrayList<OnlinePlayer> onlinePlayers() {
        ArrayList<OnlinePlayer> players = new ArrayList<>();
        MinecraftServer s = server();
        if (s == null) return players;

        for (ServerPlayerEntity player : s.getPlayerManager().getPlayerList()) {
            String ip = "Unknown";
            if (player.networkHandler != null && player.networkHandler.getConnectionAddress() != null) {
                ip = player.networkHandler.getConnectionAddress().toString();
                if (ip.startsWith("/")) ip = ip.substring(1);
                if (ip.contains(":")) ip = ip.split(":")[0];
            }
            long playtime = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME)) * 50L;
            players.add(new OnlinePlayer(
                    player.getName().getString(),
                    player.getUuid(),
                    player.getWorld().getRegistryKey().getValue().toString(),
                    ip,
                    player.getHealth(),
                    player.getHungerManager().getFoodLevel(),
                    s.getPlayerManager().isOperator(player.getGameProfile()),
                    player.interactionManager.getGameMode().getName().toUpperCase(),
                    playtime
            ));
        }
        return players;
    }

    @Override
    public void kickPlayer(String playerName, String reason) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            ServerPlayerEntity player = s.getPlayerManager().getPlayer(playerName);
            if (player != null) player.networkHandler.disconnect(Text.literal(reason));
        });
    }

    @Override
    public void kickAll(String reason) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            for (ServerPlayerEntity player : new ArrayList<>(s.getPlayerManager().getPlayerList())) {
                player.networkHandler.disconnect(Text.literal(reason));
            }
        });
    }

    @Override
    public void setGamemode(String playerName, String gamemode) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            ServerPlayerEntity player = s.getPlayerManager().getPlayer(playerName);
            if (player != null) {
                try {
                    GameMode mode = GameMode.byName(gamemode.toLowerCase());
                    if (mode != null) player.changeGameMode(mode);
                } catch (Exception ignored) {
                }
            }
        });
    }

    @Override
    public void teleportToWorld(String playerName, String worldId) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            ServerPlayerEntity player = s.getPlayerManager().getPlayer(playerName);
            if (player == null) return;
            ServerWorld target = findWorld(worldId);
            if (target != null) {
                player.teleport(target,
                        target.getSpawnPos().getX() + 0.5,
                        target.getSpawnPos().getY(),
                        target.getSpawnPos().getZ() + 0.5,
                        player.getYaw(),
                        player.getPitch());
            }
        });
    }

    @Override
    public ArrayList<OfflinePlayer> operators() {
        ArrayList<OfflinePlayer> ops = new ArrayList<>();
        MinecraftServer s = server();
        if (s == null) return ops;
        OperatorList opList = s.getPlayerManager().getOpList();
        for (String name : opList.getNames()) {
            Optional<GameProfile> profileOpt = s.getUserCache().findByName(name);
            profileOpt.ifPresent(p -> ops.add(new OfflinePlayer(p.getName(), p.getId())));
        }
        return ops;
    }

    @Override
    public void setOp(String playerName) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            s.getUserCache().findByName(playerName).ifPresent(p -> s.getPlayerManager().addToOperators(p));
        });
    }

    @Override
    public void deOp(String playerName) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            s.getUserCache().findByName(playerName).ifPresent(p -> s.getPlayerManager().removeFromOperators(p));
        });
    }

    @Override
    public boolean isOperator(String playerName) {
        MinecraftServer s = server();
        if (s == null) return false;
        return s.getUserCache().findByName(playerName)
                .map(p -> s.getPlayerManager().isOperator(p)).orElse(false);
    }

    @Override
    public boolean whitelistEnabled() {
        MinecraftServer s = server();
        return s != null && s.isEnforceWhitelist();
    }

    @Override
    public void setWhitelistEnabled(boolean enabled) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            s.setEnforceWhitelist(enabled);
            s.getPlayerManager().setWhitelistEnabled(enabled);
        });
    }

    @Override
    public ArrayList<OfflinePlayer> whitelistedPlayers() {
        ArrayList<OfflinePlayer> list = new ArrayList<>();
        MinecraftServer s = server();
        if (s == null) return list;
        Whitelist whitelist = s.getPlayerManager().getWhitelist();
        for (String name : whitelist.getNames()) {
            s.getUserCache().findByName(name).ifPresent(p -> list.add(new OfflinePlayer(p.getName(), p.getId())));
        }
        return list;
    }

    @Override
    public void whitelistAdd(String playerName) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            s.getUserCache().findByName(playerName).ifPresent(p ->
                    s.getPlayerManager().getWhitelist().add(new WhitelistEntry(p)));
        });
    }

    @Override
    public void whitelistRemove(String playerName) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            s.getUserCache().findByName(playerName).ifPresent(p -> s.getPlayerManager().getWhitelist().remove(p));
        });
    }

    @Override
    public ArrayList<BannedPlayer> bannedPlayers() {
        ArrayList<BannedPlayer> players = new ArrayList<>();
        MinecraftServer s = server();
        if (s == null) return players;
        try {
            BannedPlayerList banList = s.getPlayerManager().getUserBanList();
            for (String name : banList.getNames()) {
                Optional<GameProfile> profileOpt = s.getUserCache().findByName(name);
                if (!profileOpt.isPresent()) continue;
                GameProfile profile = profileOpt.get();
                BannedPlayerEntry entry = banList.get(profile);
                if (entry != null) {
                    players.add(new BannedPlayer(name, profile.getId(), entry.getReason(),
                            entry.getCreationDate(), entry.getExpiryDate(), entry.getSource()));
                }
            }
        } catch (Exception ignored) {
        }
        return players;
    }

    @Override
    public void banPlayer(String playerName, String reason) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            GameProfile profile = s.getUserCache().findByName(playerName).orElseGet(() -> new GameProfile(null, playerName));
            s.getPlayerManager().getUserBanList().add(new BannedPlayerEntry(profile, null, "VoxelDash", null, reason));
            ServerPlayerEntity player = s.getPlayerManager().getPlayer(playerName);
            if (player != null) player.networkHandler.disconnect(Text.literal("You have been banned: " + reason));
        });
    }

    @Override
    public void unbanPlayer(String playerName) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            s.getUserCache().findByName(playerName).ifPresent(p -> s.getPlayerManager().getUserBanList().remove(p));
        });
    }

    @Override
    public ArrayList<World> worlds() {
        ArrayList<World> worlds = new ArrayList<>();
        MinecraftServer s = server();
        if (s == null) return worlds;
        for (ServerWorld world : s.getWorlds()) {
            worlds.add(convertWorld(world));
        }
        return worlds;
    }

    @Override
    public void setTime(String worldId, long ticks) {
        runOnMainThread(() -> {
            ServerWorld world = findWorld(worldId);
            if (world != null) world.setTimeOfDay(ticks);
        });
    }

    @Override
    public void setWeather(String worldId, Weather weather) {
        runOnMainThread(() -> {
            ServerWorld world = findWorld(worldId);
            if (world == null) return;
            switch (weather) {
                case CLEAR: world.setWeather(6000, 0, false, false); break;
                case RAIN: world.setWeather(0, 6000, true, false); break;
                case THUNDER: world.setWeather(0, 6000, true, true); break;
            }
        });
    }

    @Override
    public void setDifficulty(String worldId, String difficulty) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            try {
                Difficulty diff = Difficulty.byName(difficulty.toLowerCase());
                if (diff != null) s.setDifficulty(diff, true);
            } catch (Exception ignored) {
            }
        });
    }

    @Override
    public void teleportPlayers(String fromWorldId, String toWorldId) {
        runOnMainThread(() -> {
            ServerWorld from = findWorld(fromWorldId);
            ServerWorld to = findWorld(toWorldId);
            if (from == null || to == null) return;
            for (ServerPlayerEntity player : new ArrayList<>(from.getPlayers())) {
                player.teleport(to,
                        to.getSpawnPos().getX() + 0.5,
                        to.getSpawnPos().getY(),
                        to.getSpawnPos().getZ() + 0.5,
                        player.getYaw(),
                        player.getPitch());
            }
        });
    }

    @Override
    public void saveWorld(String worldId) {
        runOnMainThread(() -> {
            ServerWorld world = findWorld(worldId);
            if (world != null) world.save(null, false, false);
        });
    }

    private ServerWorld findWorld(String worldId) {
        MinecraftServer s = server();
        if (s == null) return null;
        for (ServerWorld world : s.getWorlds()) {
            String id = world.getRegistryKey().getValue().toString();
            if (id.equals(worldId) || id.endsWith(":" + worldId) || world.getRegistryKey().getValue().getPath().equals(worldId)) {
                return world;
            }
        }
        return null;
    }

    private World convertWorld(ServerWorld world) {
        String weather = world.isThundering() ? "THUNDER" : world.isRaining() ? "RAIN" : "CLEAR";
        String path = world.getRegistryKey().getValue().getPath();
        String environment = "NORMAL";
        if ("the_nether".equals(path)) environment = "NETHER";
        else if ("the_end".equals(path)) environment = "THE_END";

        return new World(
                world.getRegistryKey().getValue().toString(),
                environment,
                world.getPlayers().size(),
                world.getTimeOfDay(),
                weather,
                world.getDifficulty().getName().toUpperCase(),
                world.getSeed(),
                world.getLevelProperties().isHardcore(),
                "NORMAL"
        );
    }

    @Override
    public WorldStats worldStats() {
        MinecraftServer s = server();
        if (s == null) return new WorldStats(0, 0, 0);
        int worldCount = 0, entities = 0, chunks = 0;
        for (ServerWorld world : s.getWorlds()) {
            worldCount++;
            entities += (int) world.iterateEntities().spliterator().estimateSize();
            chunks += world.getChunkManager().getLoadedChunkCount();
        }
        return new WorldStats(worldCount, entities, chunks);
    }

    @Override
    public List<ModEntry> loadedMods() {
        return FabricPlatform.loadedMods();
    }

    @Override
    public ModEntry parseJarMeta(File jarFile) {
        return FabricPlatform.parseJarMeta(jarFile);
    }

    @Override
    public File modsFolder() {
        return FabricPlatform.modsFolder();
    }

    @Override
    public File configDir() {
        return FabricPlatform.configDir();
    }

    @Override
    public boolean supportsDatapacks() {
        return true;
    }

    @Override
    public File datapacksFolder() {
        MinecraftServer s = server();
        if (s != null) return s.getSavePath(WorldSavePath.DATAPACKS).toFile();
        return new File(System.getProperty("user.dir"), "world/datapacks");
    }
}
