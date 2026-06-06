package de.gnm.voxeldash.util;

import de.gnm.voxeldash.VoxelDashMod;
import de.gnm.voxeldash.api.entities.BannedPlayer;
import de.gnm.voxeldash.api.entities.OfflinePlayer;
import de.gnm.voxeldash.api.entities.OnlinePlayer;
import de.gnm.voxeldash.api.entities.World;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.stats.Stats;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * {@link FabricCompat} for the 26.x+ year-major Fabric line. Because FabricMC no longer
 * publishes Yarn for the stable 26.x releases, this module is built against Mojang's
 * official mappings — so the {@code net.minecraft.*} names here are mojmap (identical to
 * the {@code forge/mc26} module) rather than Yarn. The MC pieces are plain vanilla calls;
 * only the mod-introspection bits differ from Forge and delegate to {@link FabricPlatform}
 * (FabricLoader API, mapping-stable across every version). Together with VoxelDashMod and
 * PasswordCommand this is the only file in the module touching net.minecraft.
 */
public class VersionCompat implements FabricCompat {

    private static MinecraftServer server() {
        return VoxelDashMod.getServer();
    }

    private static Optional<NameAndId> lookup(String name) {
        MinecraftServer s = server();
        if (s == null) return Optional.empty();
        return s.services().nameToIdCache().get(name);
    }

    @Override
    public void runOnMainThread(Runnable runnable) {
        MinecraftServer server = server();
        if (server == null || server.isSameThread()) {
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
        if (server == null || server.isSameThread()) {
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
        return s != null ? s.getPort() : 25565;
    }

    @Override
    public double currentTps() {
        MinecraftServer s = server();
        if (s == null) return 20.0;
        try {
            long[] times = s.getTickTimesNanos();
            if (times == null || times.length == 0) return 20.0;
            long sum = 0;
            for (long t : times) sum += t;
            double avg = sum / (double) times.length;
            return Math.min(20.0, 1_000_000_000.0 / avg);
        } catch (Throwable t) {
            return 20.0;
        }
    }

    @Override
    public int onlinePlayerCount() {
        MinecraftServer s = server();
        return s != null ? s.getPlayerCount() : 0;
    }

    @Override
    public int maxPlayers() {
        MinecraftServer s = server();
        return s != null ? s.getMaxPlayers() : 0;
    }

    @Override
    public void reloadServer() {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s != null) s.reloadResources(s.getPackRepository().getSelectedIds());
        });
    }

    @Override
    public void stopServer() {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s != null) s.halt(false);
        });
    }

    @Override
    public void runCommand(String command) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            String clean = command.startsWith("/") ? command.substring(1) : command;
            s.getCommands().performPrefixedCommand(s.createCommandSourceStack(), clean);
        });
    }

    @Override
    public void broadcast(String message) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s != null) s.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
        });
    }

    @Override
    public ArrayList<OnlinePlayer> onlinePlayers() {
        ArrayList<OnlinePlayer> players = new ArrayList<>();
        MinecraftServer s = server();
        if (s == null) return players;

        for (ServerPlayer player : s.getPlayerList().getPlayers()) {
            String ip = player.getIpAddress();
            if (ip != null) {
                if (ip.startsWith("/")) ip = ip.substring(1);
                if (ip.contains(":")) ip = ip.split(":")[0];
            } else {
                ip = "Unknown";
            }
            long playtime = player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) * 50L;
            players.add(new OnlinePlayer(
                    player.getName().getString(),
                    player.getUUID(),
                    player.level().dimension().identifier().toString(),
                    ip,
                    player.getHealth(),
                    player.getFoodData().getFoodLevel(),
                    s.getPlayerList().isOp(new NameAndId(player.getGameProfile())),
                    player.gameMode.getGameModeForPlayer().getName().toUpperCase(),
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
            ServerPlayer player = s.getPlayerList().getPlayerByName(playerName);
            if (player != null) player.connection.disconnect(Component.literal(reason));
        });
    }

    @Override
    public void kickAll(String reason) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            for (ServerPlayer player : new ArrayList<>(s.getPlayerList().getPlayers())) {
                player.connection.disconnect(Component.literal(reason));
            }
        });
    }

    @Override
    public void setGamemode(String playerName, String gamemode) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            ServerPlayer player = s.getPlayerList().getPlayerByName(playerName);
            if (player != null) {
                try {
                    GameType mode = GameType.byName(gamemode.toLowerCase(), null);
                    if (mode != null) player.setGameMode(mode);
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
            ServerPlayer player = s.getPlayerList().getPlayerByName(playerName);
            if (player == null) return;
            ServerLevel target = findLevel(worldId);
            if (target != null) {
                BlockPos spawn = target.getRespawnData().pos();
                player.teleportTo(target, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                        Set.of(), player.getYRot(), player.getXRot(), true);
            }
        });
    }

    @Override
    public ArrayList<OfflinePlayer> operators() {
        ArrayList<OfflinePlayer> ops = new ArrayList<>();
        MinecraftServer s = server();
        if (s == null) return ops;
        for (String name : s.getPlayerList().getOps().getUserList()) {
            lookup(name).ifPresent(p -> ops.add(new OfflinePlayer(p.name(), p.id())));
        }
        return ops;
    }

    @Override
    public void setOp(String playerName) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            lookup(playerName).ifPresent(p -> s.getPlayerList().op(p));
        });
    }

    @Override
    public void deOp(String playerName) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            lookup(playerName).ifPresent(p -> s.getPlayerList().deop(p));
        });
    }

    @Override
    public boolean isOperator(String playerName) {
        MinecraftServer s = server();
        if (s == null) return false;
        return lookup(playerName).map(p -> s.getPlayerList().isOp(p)).orElse(false);
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
            if (s != null) s.setEnforceWhitelist(enabled);
        });
    }

    @Override
    public ArrayList<OfflinePlayer> whitelistedPlayers() {
        ArrayList<OfflinePlayer> list = new ArrayList<>();
        MinecraftServer s = server();
        if (s == null) return list;
        for (String name : s.getPlayerList().getWhiteList().getUserList()) {
            lookup(name).ifPresent(p -> list.add(new OfflinePlayer(p.name(), p.id())));
        }
        return list;
    }

    @Override
    public void whitelistAdd(String playerName) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            lookup(playerName).ifPresent(p -> s.getPlayerList().getWhiteList().add(new UserWhiteListEntry(p)));
        });
    }

    @Override
    public void whitelistRemove(String playerName) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            lookup(playerName).ifPresent(p -> s.getPlayerList().getWhiteList().remove(p));
        });
    }

    @Override
    public ArrayList<BannedPlayer> bannedPlayers() {
        ArrayList<BannedPlayer> players = new ArrayList<>();
        MinecraftServer s = server();
        if (s == null) return players;
        try {
            UserBanList banList = s.getPlayerList().getBans();
            for (String name : banList.getUserList()) {
                Optional<NameAndId> profileOpt = lookup(name);
                if (!profileOpt.isPresent()) continue;
                NameAndId profile = profileOpt.get();
                UserBanListEntry entry = banList.get(profile);
                if (entry != null) {
                    players.add(new BannedPlayer(name, profile.id(), entry.getReason(),
                            entry.getCreated(), entry.getExpires(), entry.getSource()));
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
            NameAndId profile = lookup(playerName).orElseGet(() -> NameAndId.createOffline(playerName));
            s.getPlayerList().getBans().add(new UserBanListEntry(profile, null, "VoxelDash", null, reason));
            ServerPlayer player = s.getPlayerList().getPlayerByName(playerName);
            if (player != null) player.connection.disconnect(Component.literal("You have been banned: " + reason));
        });
    }

    @Override
    public void unbanPlayer(String playerName) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            lookup(playerName).ifPresent(p -> s.getPlayerList().getBans().remove(p));
        });
    }

    @Override
    public ArrayList<World> worlds() {
        ArrayList<World> worlds = new ArrayList<>();
        MinecraftServer s = server();
        if (s == null) return worlds;
        for (ServerLevel level : s.getAllLevels()) {
            worlds.add(convertWorld(level));
        }
        return worlds;
    }

    @Override
    public void setTime(String worldId, long ticks) {
        runCommand("time set " + ticks);
    }

    @Override
    public void setWeather(String worldId, Weather weather) {
        switch (weather) {
            case CLEAR: runCommand("weather clear"); break;
            case RAIN: runCommand("weather rain"); break;
            case THUNDER: runCommand("weather thunder"); break;
        }
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
            ServerLevel from = findLevel(fromWorldId);
            ServerLevel to = findLevel(toWorldId);
            if (from == null || to == null) return;
            BlockPos spawn = to.getRespawnData().pos();
            for (ServerPlayer player : new ArrayList<>(from.players())) {
                player.teleportTo(to, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                        Set.of(), player.getYRot(), player.getXRot(), true);
            }
        });
    }

    @Override
    public void saveWorld(String worldId) {
        runOnMainThread(() -> {
            ServerLevel level = findLevel(worldId);
            if (level != null) level.save(null, false, false);
        });
    }

    private ServerLevel findLevel(String worldId) {
        MinecraftServer s = server();
        if (s == null) return null;
        for (ServerLevel level : s.getAllLevels()) {
            String id = level.dimension().identifier().toString();
            if (id.equals(worldId) || id.endsWith(":" + worldId) || level.dimension().identifier().getPath().equals(worldId)) {
                return level;
            }
        }
        return null;
    }

    private World convertWorld(ServerLevel level) {
        String weather = level.isThundering() ? "THUNDER" : level.isRaining() ? "RAIN" : "CLEAR";
        String path = level.dimension().identifier().getPath();
        String environment = "NORMAL";
        if ("the_nether".equals(path)) environment = "NETHER";
        else if ("the_end".equals(path)) environment = "THE_END";

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

    @Override
    public WorldStats worldStats() {
        MinecraftServer s = server();
        if (s == null) return new WorldStats(0, 0, 0);
        int worldCount = 0, entities = 0, chunks = 0;
        for (ServerLevel level : s.getAllLevels()) {
            worldCount++;
            for (Object ignored : level.getAllEntities()) entities++;
            chunks += level.getChunkSource().getLoadedChunksCount();
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
        if (s != null) return s.getWorldPath(LevelResource.DATAPACK_DIR).toFile();
        return new File(System.getProperty("user.dir"), "world/datapacks");
    }
}
