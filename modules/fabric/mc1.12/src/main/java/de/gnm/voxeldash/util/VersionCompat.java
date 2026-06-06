package de.gnm.voxeldash.util;

import com.mojang.authlib.GameProfile;
import de.gnm.voxeldash.VoxelDashMod;
import de.gnm.voxeldash.api.entities.BannedPlayer;
import de.gnm.voxeldash.api.entities.OfflinePlayer;
import de.gnm.voxeldash.api.entities.OnlinePlayer;
import de.gnm.voxeldash.api.entities.World;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.BannedPlayerEntry;
import net.minecraft.server.BannedPlayerList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.OperatorEntry;
import net.minecraft.server.Whitelist;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.level.LevelProperties;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class VersionCompat implements FabricCompat {

    private static final int[] DIMENSION_IDS = {0, -1, 1};

    private static MinecraftServer server() {
        return VoxelDashMod.getServer();
    }

    private static boolean onServerThread(MinecraftServer s) {
        return s == null || Thread.currentThread() == s.getThread();
    }

    @Override
    public void runOnMainThread(Runnable runnable) {
        MinecraftServer s = server();
        if (onServerThread(s)) {
            runnable.run();
            return;
        }
        try {
            s.method_10815(Executors.callable(runnable)).get(5, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void runOnMainThreadAsync(Runnable runnable) {
        MinecraftServer s = server();
        if (onServerThread(s)) {
            runnable.run();
        } else {
            s.method_10815(Executors.callable(runnable));
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
            long[] times = s.lastTickLengths;
            if (times == null || times.length == 0) return 20.0;
            long sum = 0;
            for (long t : times) sum += t;
            double avg = sum / (double) times.length;
            if (avg <= 0) return 20.0;
            return Math.min(20.0, 1_000_000_000.0 / avg);
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
    }

    @Override
    public void stopServer() {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s != null) s.stopRunning();
        });
    }

    @Override
    public void runCommand(String command) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            String clean = command.startsWith("/") ? command.substring(1) : command;
            s.getCommandManager().execute((CommandSource) s, clean);
        });
    }

    @Override
    public void broadcast(String message) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s != null) s.getPlayerManager().broadcastChatMessage(new LiteralText(message), false);
        });
    }

    @Override
    public ArrayList<OnlinePlayer> onlinePlayers() {
        ArrayList<OnlinePlayer> players = new ArrayList<>();
        MinecraftServer s = server();
        if (s == null) return players;

        for (ServerPlayerEntity player : s.getPlayerManager().getPlayers()) {
            String ip = "Unknown";
            if (player.networkHandler != null && player.networkHandler.connection != null
                    && player.networkHandler.connection.getAddress() != null) {
                ip = player.networkHandler.connection.getAddress().toString();
                if (ip.startsWith("/")) ip = ip.substring(1);
                if (ip.contains(":")) ip = ip.split(":")[0];
            }
            players.add(new OnlinePlayer(
                    player.getGameProfile().getName(),
                    player.getUuid(),
                    worldId(s, player.getServerWorld()),
                    ip,
                    player.getHealth(),
                    player.getHungerManager().getFoodLevel(),
                    s.getPlayerManager().isOperator(player.getGameProfile()),
                    player.interactionManager.getGameMode().name(),
                    0L
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
            if (player != null) player.networkHandler.method_14977(new LiteralText(reason));
        });
    }

    @Override
    public void kickAll(String reason) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            for (ServerPlayerEntity player : new ArrayList<>(s.getPlayerManager().getPlayers())) {
                player.networkHandler.method_14977(new LiteralText(reason));
            }
        });
    }

    @Override
    public void setGamemode(String playerName, String gamemode) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            ServerPlayerEntity player = s.getPlayerManager().getPlayer(playerName);
            if (player == null) return;
            for (GameMode mode : GameMode.values()) {
                if (mode.name().equalsIgnoreCase(gamemode)) {
                    player.interactionManager.setGameMode(mode);
                    return;
                }
            }
        });
    }

    @Override
    public void teleportToWorld(String playerName, String worldId) {
    }

    @Override
    public ArrayList<OfflinePlayer> operators() {
        ArrayList<OfflinePlayer> ops = new ArrayList<>();
        MinecraftServer s = server();
        if (s == null) return ops;
        for (String name : s.getPlayerManager().getOpList().getNames()) {
            GameProfile p = s.getUserCache().findByName(name);
            if (p != null) ops.add(new OfflinePlayer(p.getName(), p.getId()));
        }
        return ops;
    }

    @Override
    public void setOp(String playerName) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            GameProfile p = s.getUserCache().findByName(playerName);
            if (p != null) s.getPlayerManager().getOpList().add(new OperatorEntry(p, s.getOpPermissionLevel(), false));
        });
    }

    @Override
    public void deOp(String playerName) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            GameProfile p = s.getUserCache().findByName(playerName);
            if (p != null) s.getPlayerManager().getOpList().remove(p);
        });
    }

    @Override
    public boolean isOperator(String playerName) {
        MinecraftServer s = server();
        if (s == null) return false;
        GameProfile p = s.getUserCache().findByName(playerName);
        return p != null && s.getPlayerManager().isOperator(p);
    }

    @Override
    public boolean whitelistEnabled() {
        MinecraftServer s = server();
        return s != null && s.getPlayerManager().isWhitelistEnabled();
    }

    @Override
    public void setWhitelistEnabled(boolean enabled) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s != null) s.getPlayerManager().setWhitelistEnabled(enabled);
        });
    }

    @Override
    public ArrayList<OfflinePlayer> whitelistedPlayers() {
        ArrayList<OfflinePlayer> list = new ArrayList<>();
        MinecraftServer s = server();
        if (s == null) return list;
        for (String name : s.getPlayerManager().getWhitelist().getNames()) {
            GameProfile p = s.getUserCache().findByName(name);
            if (p != null) list.add(new OfflinePlayer(p.getName(), p.getId()));
        }
        return list;
    }

    @Override
    public void whitelistAdd(String playerName) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            GameProfile p = s.getUserCache().findByName(playerName);
            if (p != null) s.getPlayerManager().getWhitelist().add(new WhitelistEntry(p));
        });
    }

    @Override
    public void whitelistRemove(String playerName) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            GameProfile p = s.getUserCache().findByName(playerName);
            if (p != null) s.getPlayerManager().getWhitelist().remove(p);
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
                GameProfile profile = s.getUserCache().findByName(name);
                if (profile == null) continue;
                BannedPlayerEntry entry = (BannedPlayerEntry) banList.get(profile);
                if (entry != null) {
                    players.add(new BannedPlayer(name, profile.getId(), entry.getReason(),
                            null, entry.getExpiryDate(), null));
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
            GameProfile profile = s.getUserCache().findByName(playerName);
            if (profile == null) profile = new GameProfile(null, playerName);
            s.getPlayerManager().getUserBanList().add(new BannedPlayerEntry(profile, null, "VoxelDash", null, reason));
            ServerPlayerEntity player = s.getPlayerManager().getPlayer(playerName);
            if (player != null) player.networkHandler.method_14977(new LiteralText("You have been banned: " + reason));
        });
    }

    @Override
    public void unbanPlayer(String playerName) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            GameProfile p = s.getUserCache().findByName(playerName);
            if (p != null) s.getPlayerManager().getUserBanList().remove(p);
        });
    }

    @Override
    public ArrayList<World> worlds() {
        ArrayList<World> worlds = new ArrayList<>();
        MinecraftServer s = server();
        if (s == null) return worlds;
        for (int id : DIMENSION_IDS) {
            ServerWorld world = s.getWorld(id);
            if (world != null) worlds.add(convertWorld(world, id));
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
            LevelProperties props = world.getLevelProperties();
            switch (weather) {
                case CLEAR:
                    props.setClearWeatherTime(6000);
                    props.setRaining(false);
                    props.setRainTime(0);
                    props.setThundering(false);
                    props.setThunderTime(0);
                    break;
                case RAIN:
                    props.setClearWeatherTime(0);
                    props.setRaining(true);
                    props.setRainTime(6000);
                    props.setThundering(false);
                    props.setThunderTime(0);
                    break;
                case THUNDER:
                    props.setClearWeatherTime(0);
                    props.setRaining(true);
                    props.setRainTime(6000);
                    props.setThundering(true);
                    props.setThunderTime(6000);
                    break;
            }
        });
    }

    @Override
    public void setDifficulty(String worldId, String difficulty) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            for (Difficulty diff : Difficulty.values()) {
                if (diff.getName().equalsIgnoreCase(difficulty)) {
                    s.setDifficulty(diff);
                    return;
                }
            }
        });
    }

    @Override
    public void teleportPlayers(String fromWorldId, String toWorldId) {
    }

    @Override
    public void saveWorld(String worldId) {
        runOnMainThread(() -> {
            ServerWorld world = findWorld(worldId);
            if (world == null) return;
            try {
                world.save(false, null);
            } catch (Exception ignored) {
            }
        });
    }

    private static String idToName(int dimId) {
        switch (dimId) {
            case -1: return "minecraft:the_nether";
            case 1: return "minecraft:the_end";
            case 0: return "minecraft:overworld";
            default: return "minecraft:dim_" + dimId;
        }
    }

    private String worldId(MinecraftServer s, ServerWorld world) {
        for (int id : DIMENSION_IDS) {
            if (s.getWorld(id) == world) return idToName(id);
        }
        return "minecraft:overworld";
    }

    private ServerWorld findWorld(String worldId) {
        MinecraftServer s = server();
        if (s == null) return null;
        for (int id : DIMENSION_IDS) {
            String name = idToName(id);
            String path = name.substring(name.indexOf(':') + 1);
            if (name.equals(worldId) || name.endsWith(":" + worldId) || path.equals(worldId)) {
                return s.getWorld(id);
            }
        }
        return null;
    }

    private World convertWorld(ServerWorld world, int dimId) {
        String weather = world.isThundering() ? "THUNDER" : world.isRaining() ? "RAIN" : "CLEAR";
        String environment = dimId == -1 ? "NETHER" : dimId == 1 ? "THE_END" : "NORMAL";
        LevelProperties props = world.getLevelProperties();
        int playerCount = 0;
        MinecraftServer s = server();
        if (s != null) {
            for (ServerPlayerEntity p : s.getPlayerManager().getPlayers()) {
                if (p.getServerWorld() == world) playerCount++;
            }
        }
        return new World(
                idToName(dimId),
                environment,
                playerCount,
                world.getTimeOfDay(),
                weather,
                props.getDifficulty().getName().toUpperCase(),
                world.getSeed(),
                props.isHardcore(),
                "NORMAL"
        );
    }

    @Override
    public WorldStats worldStats() {
        MinecraftServer s = server();
        if (s == null) return new WorldStats(0, 0, 0);
        int worldCount = 0;
        for (int id : DIMENSION_IDS) {
            if (s.getWorld(id) != null) worldCount++;
        }
        return new WorldStats(worldCount, 0, 0);
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
        return false;
    }

    @Override
    public File datapacksFolder() {
        return null;
    }
}
