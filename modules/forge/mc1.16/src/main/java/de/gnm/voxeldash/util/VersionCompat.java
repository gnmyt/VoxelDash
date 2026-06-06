package de.gnm.voxeldash.util;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.mojang.authlib.GameProfile;
import de.gnm.voxeldash.VoxelDashMod;
import de.gnm.voxeldash.api.entities.BannedPlayer;
import de.gnm.voxeldash.api.entities.OfflinePlayer;
import de.gnm.voxeldash.api.entities.OnlinePlayer;
import de.gnm.voxeldash.api.entities.World;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameRules;
import net.minecraft.server.management.BanList;
import net.minecraft.server.management.ProfileBanEntry;
import net.minecraft.server.management.WhitelistEntry;
import net.minecraft.stats.Stats;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameType;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.IModInfo;

import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;

/**
 * {@link ForgeCompat} for the 1.16.x line (Java 8, FG5). MCP-style class names,
 * Mojang-named methods. The only file here (besides VoxelDashMod / the command
 * shell) that touches net.minecraft.
 */
public class VersionCompat implements ForgeCompat {

    private static MinecraftServer server() {
        return VoxelDashMod.getServer();
    }

    private static GameProfile lookup(String name) {
        MinecraftServer s = server();
        return s == null ? null : s.getProfileCache().get(name);
    }

    private static ITextComponent text(String message) {
        return new StringTextComponent(message);
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
        MinecraftServer s = server();
        return s != null ? s.getServerVersion() : "Unknown";
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
            float avgMs = s.getAverageTickTime();
            if (avgMs <= 0) return 20.0;
            return Math.min(20.0, 1000.0 / avgMs);
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
            s.getCommands().performCommand(s.createCommandSourceStack(), clean);
        });
    }

    @Override
    public void broadcast(String message) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s != null) s.getPlayerList().broadcastMessage(text(message), ChatType.SYSTEM, Util.NIL_UUID);
        });
    }

    @Override
    public ArrayList<OnlinePlayer> onlinePlayers() {
        ArrayList<OnlinePlayer> players = new ArrayList<>();
        MinecraftServer s = server();
        if (s == null) return players;

        for (ServerPlayerEntity player : s.getPlayerList().getPlayers()) {
            String ip = player.getIpAddress();
            if (ip != null) {
                if (ip.startsWith("/")) ip = ip.substring(1);
                if (ip.contains(":")) ip = ip.split(":")[0];
            } else {
                ip = "Unknown";
            }
            long playtime = player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_ONE_MINUTE)) * 50L;
            players.add(new OnlinePlayer(
                    player.getName().getString(),
                    player.getUUID(),
                    player.level.dimension().location().toString(),
                    ip,
                    player.getHealth(),
                    player.getFoodData().getFoodLevel(),
                    s.getPlayerList().isOp(player.getGameProfile()),
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
            ServerPlayerEntity player = s.getPlayerList().getPlayerByName(playerName);
            if (player != null) player.connection.disconnect(text(reason));
        });
    }

    @Override
    public void kickAll(String reason) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            for (ServerPlayerEntity player : new ArrayList<>(s.getPlayerList().getPlayers())) {
                player.connection.disconnect(text(reason));
            }
        });
    }

    @Override
    public void setGamemode(String playerName, String gamemode) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            ServerPlayerEntity player = s.getPlayerList().getPlayerByName(playerName);
            if (player != null) {
                try {
                    GameType mode = GameType.byName(gamemode.toLowerCase());
                    if (mode != GameType.NOT_SET) player.setGameMode(mode);
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
            ServerPlayerEntity player = s.getPlayerList().getPlayerByName(playerName);
            if (player == null) return;
            ServerWorld target = findLevel(worldId);
            if (target != null) {
                BlockPos spawn = target.getSharedSpawnPos();
                player.teleportTo(target, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                        player.yRot, player.xRot);
            }
        });
    }

    @Override
    public ArrayList<OfflinePlayer> operators() {
        ArrayList<OfflinePlayer> ops = new ArrayList<>();
        MinecraftServer s = server();
        if (s == null) return ops;
        for (String name : s.getPlayerList().getOps().getUserList()) {
            GameProfile profile = s.getProfileCache().get(name);
            if (profile != null) ops.add(new OfflinePlayer(profile.getName(), profile.getId()));
        }
        return ops;
    }

    @Override
    public void setOp(String playerName) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            GameProfile profile = lookup(playerName);
            if (s != null && profile != null) s.getPlayerList().op(profile);
        });
    }

    @Override
    public void deOp(String playerName) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            GameProfile profile = lookup(playerName);
            if (s != null && profile != null) s.getPlayerList().deop(profile);
        });
    }

    @Override
    public boolean isOperator(String playerName) {
        MinecraftServer s = server();
        GameProfile profile = lookup(playerName);
        return s != null && profile != null && s.getPlayerList().isOp(profile);
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
            s.getPlayerList().setUsingWhiteList(enabled);
        });
    }

    @Override
    public ArrayList<OfflinePlayer> whitelistedPlayers() {
        ArrayList<OfflinePlayer> list = new ArrayList<>();
        MinecraftServer s = server();
        if (s == null) return list;
        for (String name : s.getPlayerList().getWhiteList().getUserList()) {
            GameProfile profile = s.getProfileCache().get(name);
            if (profile != null) list.add(new OfflinePlayer(profile.getName(), profile.getId()));
        }
        return list;
    }

    @Override
    public void whitelistAdd(String playerName) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            GameProfile profile = lookup(playerName);
            if (s != null && profile != null) s.getPlayerList().getWhiteList().add(new WhitelistEntry(profile));
        });
    }

    @Override
    public void whitelistRemove(String playerName) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            GameProfile profile = lookup(playerName);
            if (s != null && profile != null) s.getPlayerList().getWhiteList().remove(profile);
        });
    }

    @Override
    public ArrayList<BannedPlayer> bannedPlayers() {
        ArrayList<BannedPlayer> players = new ArrayList<>();
        MinecraftServer s = server();
        if (s == null) return players;
        BanList banList = s.getPlayerList().getBans();
        for (String name : banList.getUserList()) {
            GameProfile profile = s.getProfileCache().get(name);
            if (profile == null) continue;
            ProfileBanEntry entry = banList.get(profile);
            if (entry != null) {
                players.add(new BannedPlayer(name, profile.getId(), entry.getReason(),
                        null, entry.getExpires(), entry.getSource()));
            }
        }
        return players;
    }

    @Override
    public void banPlayer(String playerName, String reason) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            GameProfile profile = lookup(playerName);
            if (profile == null) profile = new GameProfile(null, playerName);
            s.getPlayerList().getBans().add(new ProfileBanEntry(profile, null, "VoxelDash", null, reason));
            ServerPlayerEntity player = s.getPlayerList().getPlayerByName(playerName);
            if (player != null) player.connection.disconnect(text("You have been banned: " + reason));
        });
    }

    @Override
    public void unbanPlayer(String playerName) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            GameProfile profile = lookup(playerName);
            if (s != null && profile != null) s.getPlayerList().getBans().remove(profile);
        });
    }

    @Override
    public ArrayList<World> worlds() {
        ArrayList<World> worlds = new ArrayList<>();
        MinecraftServer s = server();
        if (s == null) return worlds;
        for (ServerWorld level : s.getAllLevels()) {
            worlds.add(convertWorld(level));
        }
        return worlds;
    }

    @Override
    public void setTime(String worldId, long ticks) {
        runOnMainThread(() -> {
            ServerWorld level = findLevel(worldId);
            if (level != null) level.setDayTime(ticks);
        });
    }

    @Override
    public void setWeather(String worldId, Weather weather) {
        runOnMainThread(() -> {
            ServerWorld level = findLevel(worldId);
            if (level == null) return;
            switch (weather) {
                case CLEAR: level.setWeatherParameters(6000, 0, false, false); break;
                case RAIN: level.setWeatherParameters(0, 6000, true, false); break;
                case THUNDER: level.setWeatherParameters(0, 6000, true, true); break;
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
            ServerWorld from = findLevel(fromWorldId);
            ServerWorld to = findLevel(toWorldId);
            if (from == null || to == null) return;
            BlockPos spawn = to.getSharedSpawnPos();
            for (ServerPlayerEntity player : new ArrayList<>(from.players())) {
                player.teleportTo(to, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                        player.yRot, player.xRot);
            }
        });
    }

    @Override
    public void saveWorld(String worldId) {
        runOnMainThread(() -> {
            ServerWorld level = findLevel(worldId);
            if (level == null) return;
            try {
                level.save(null, false, false);
            } catch (Exception ignored) {
            }
        });
    }

    private ServerWorld findLevel(String worldId) {
        MinecraftServer s = server();
        if (s == null) return null;
        for (ServerWorld level : s.getAllLevels()) {
            String id = level.dimension().location().toString();
            if (id.equals(worldId) || id.endsWith(":" + worldId) || level.dimension().location().getPath().equals(worldId)) {
                return level;
            }
        }
        return null;
    }

    private World convertWorld(ServerWorld level) {
        String weather = level.isThundering() ? "THUNDER" : level.isRaining() ? "RAIN" : "CLEAR";
        String path = level.dimension().location().getPath();
        String environment = "NORMAL";
        if ("the_nether".equals(path)) environment = "NETHER";
        else if ("the_end".equals(path)) environment = "THE_END";

        return new World(
                level.dimension().location().toString(),
                environment,
                level.players().size(),
                level.getDayTime(),
                weather,
                level.getLevelData().getDifficulty().getKey().toUpperCase(),
                level.getSeed(),
                level.getLevelData().isHardcore(),
                "NORMAL"
        );
    }

    @Override
    public List<GameRuleEntry> gameRules() {
        List<GameRuleEntry> out = new ArrayList<>();
        MinecraftServer s = server();
        if (s == null) return out;
        try {
            final GameRules rules = s.getGameRules();
            GameRules.visitGameRuleTypes(new GameRules.IRuleEntryVisitor() {
                @Override
                public void visitBoolean(GameRules.RuleKey<GameRules.BooleanValue> key, GameRules.RuleType<GameRules.BooleanValue> type) {
                    out.add(new GameRuleEntry(key.getId(), "BOOLEAN", String.valueOf(rules.getBoolean(key))));
                }

                @Override
                public void visitInteger(GameRules.RuleKey<GameRules.IntegerValue> key, GameRules.RuleType<GameRules.IntegerValue> type) {
                    out.add(new GameRuleEntry(key.getId(), "INTEGER", String.valueOf(rules.getInt(key))));
                }
            });
        } catch (Throwable ignored) {
        }
        return out;
    }

    @Override
    public WorldStats worldStats() {
        MinecraftServer s = server();
        if (s == null) return new WorldStats(0, 0, 0);
        int worldCount = 0, entities = 0, chunks = 0;
        for (ServerWorld level : s.getAllLevels()) {
            worldCount++;
            for (Object ignored : level.getAllEntities()) entities++;
            chunks += level.getChunkSource().getLoadedChunksCount();
        }
        return new WorldStats(worldCount, entities, chunks);
    }

    @Override
    public List<ModEntry> loadedMods() {
        List<ModEntry> result = new ArrayList<>();
        for (IModInfo info : ModList.get().getMods()) {
            result.add(new ModEntry(info.getModId(), info.getDisplayName(),
                    info.getVersion().toString(), info.getDescription(), null, null));
        }
        return result;
    }

    @Override
    public ModEntry parseJarMeta(File jarFile) {
        if (jarFile == null || !jarFile.exists()) return null;
        try (JarFile jar = new JarFile(jarFile)) {
            java.util.jar.JarEntry entry = jar.getJarEntry("META-INF/mods.toml");
            if (entry == null) return null;
            try (InputStreamReader reader = new InputStreamReader(jar.getInputStream(entry))) {
                UnmodifiableConfig config = new TomlParser().parse(reader);
                Object modsObj = config.get("mods");
                if (!(modsObj instanceof List)) return null;
                List<?> modsList = (List<?>) modsObj;
                if (modsList.isEmpty() || !(modsList.get(0) instanceof UnmodifiableConfig)) return null;
                UnmodifiableConfig mod = (UnmodifiableConfig) modsList.get(0);

                String id = mod.get("modId");
                String name = mod.get("displayName");
                Object version = mod.get("version");
                String description = mod.get("description");
                String authors = config.getOrElse("authors", mod.get("authors"));
                return new ModEntry(id, name, version != null ? String.valueOf(version) : null,
                        description, authors != null ? new String[]{authors} : null, null);
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public File modsFolder() {
        return FMLPaths.MODSDIR.get().toFile();
    }

    @Override
    public File configDir() {
        return FMLPaths.CONFIGDIR.get().toFile();
    }

    @Override
    public boolean supportsDatapacks() {
        return true;
    }

    @Override
    public File datapacksFolder() {
        MinecraftServer s = server();
        if (s != null) return s.getWorldPath(FolderName.DATAPACK_DIR).toFile();
        return new File(System.getProperty("user.dir"), "world/datapacks");
    }
}
