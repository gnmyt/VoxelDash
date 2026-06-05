package de.gnm.voxeldash.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import de.gnm.voxeldash.VoxelDashMod;
import de.gnm.voxeldash.api.entities.BannedPlayer;
import de.gnm.voxeldash.api.entities.OfflinePlayer;
import de.gnm.voxeldash.api.entities.OnlinePlayer;
import de.gnm.voxeldash.api.entities.World;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.UserListBans;
import net.minecraft.server.management.UserListBansEntry;
import net.minecraft.server.management.UserListWhitelistEntry;
import net.minecraft.stats.StatList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

/**
 * {@link ForgeCompat} for the 1.12.2 line (Java 8, FG3, MCP snapshot). The only
 * file here (besides VoxelDashMod / the command shell) that touches net.minecraft.
 */
public class VersionCompat implements ForgeCompat {

    private static MinecraftServer server() {
        return VoxelDashMod.getServer();
    }

    private static GameProfile lookup(String name) {
        MinecraftServer s = server();
        return s == null ? null : s.getPlayerProfileCache().getGameProfileForUsername(name);
    }

    private static ITextComponent text(String message) {
        return new TextComponentString(message);
    }

    private static String dimId(net.minecraft.world.World level) {
        return level.provider.getDimensionType().getName();
    }

    @Override
    public void runOnMainThread(Runnable runnable) {
        MinecraftServer server = server();
        if (server == null || server.isCallingFromMinecraftThread()) {
            runnable.run();
        } else {
            server.addScheduledTask(runnable);
        }
    }

    @Override
    public void runOnMainThreadAsync(Runnable runnable) {
        runOnMainThread(runnable);
    }

    @Override
    public String serverVersion() {
        MinecraftServer s = server();
        return s != null ? s.getMinecraftVersion() : "Unknown";
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
            long[] times = s.tickTimeArray;
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
        return s != null ? s.getPlayerList().getCurrentPlayerCount() : 0;
    }

    @Override
    public int maxPlayers() {
        MinecraftServer s = server();
        return s != null ? s.getPlayerList().getMaxPlayers() : 0;
    }

    @Override
    public void reloadServer() {
    }

    @Override
    public void stopServer() {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s != null) s.initiateShutdown();
        });
    }

    @Override
    public void runCommand(String command) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            String clean = command.startsWith("/") ? command.substring(1) : command;
            s.getCommandManager().executeCommand(s, clean);
        });
    }

    @Override
    public void broadcast(String message) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s != null) s.getPlayerList().sendMessage(text(message));
        });
    }

    @Override
    public ArrayList<OnlinePlayer> onlinePlayers() {
        ArrayList<OnlinePlayer> players = new ArrayList<>();
        MinecraftServer s = server();
        if (s == null) return players;

        for (EntityPlayerMP player : s.getPlayerList().getPlayers()) {
            String ip = player.getPlayerIP();
            if (ip != null) {
                if (ip.startsWith("/")) ip = ip.substring(1);
                if (ip.contains(":")) ip = ip.split(":")[0];
            } else {
                ip = "Unknown";
            }
            long playtime;
            try {
                playtime = (long) player.getStatFile().readStat(StatList.PLAY_ONE_MINUTE) * 50L;
            } catch (Throwable t) {
                playtime = 0L;
            }
            players.add(new OnlinePlayer(
                    player.getName(),
                    player.getUniqueID(),
                    dimId(player.world),
                    ip,
                    player.getHealth(),
                    player.getFoodStats().getFoodLevel(),
                    s.getPlayerList().canSendCommands(player.getGameProfile()),
                    player.interactionManager.getGameType().getName().toUpperCase(),
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
            EntityPlayerMP player = s.getPlayerList().getPlayerByUsername(playerName);
            if (player != null) player.connection.disconnect(text(reason));
        });
    }

    @Override
    public void kickAll(String reason) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            for (EntityPlayerMP player : new ArrayList<>(s.getPlayerList().getPlayers())) {
                player.connection.disconnect(text(reason));
            }
        });
    }

    @Override
    public void setGamemode(String playerName, String gamemode) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            EntityPlayerMP player = s.getPlayerList().getPlayerByUsername(playerName);
            if (player != null) {
                try {
                    net.minecraft.world.GameType mode = net.minecraft.world.GameType.getByName(gamemode.toLowerCase());
                    if (mode != net.minecraft.world.GameType.NOT_SET) player.setGameType(mode);
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
            EntityPlayerMP player = s.getPlayerList().getPlayerByUsername(playerName);
            if (player == null) return;
            WorldServer target = findLevel(worldId);
            if (target != null) {
                BlockPos spawn = target.getSpawnPoint();
                player.setPositionAndUpdate(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
            }
        });
    }

    @Override
    public ArrayList<OfflinePlayer> operators() {
        ArrayList<OfflinePlayer> ops = new ArrayList<>();
        MinecraftServer s = server();
        if (s == null) return ops;
        for (String name : s.getPlayerList().getOppedPlayers().getKeys()) {
            GameProfile profile = lookup(name);
            if (profile != null) ops.add(new OfflinePlayer(profile.getName(), profile.getId()));
        }
        return ops;
    }

    @Override
    public void setOp(String playerName) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            GameProfile profile = lookup(playerName);
            if (s != null && profile != null) s.getPlayerList().addOp(profile);
        });
    }

    @Override
    public void deOp(String playerName) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            GameProfile profile = lookup(playerName);
            if (s != null && profile != null) s.getPlayerList().removeOp(profile);
        });
    }

    @Override
    public boolean isOperator(String playerName) {
        MinecraftServer s = server();
        GameProfile profile = lookup(playerName);
        return s != null && profile != null && s.getPlayerList().canSendCommands(profile);
    }

    @Override
    public boolean whitelistEnabled() {
        MinecraftServer s = server();
        return s != null && s.getPlayerList().isWhiteListEnabled();
    }

    @Override
    public void setWhitelistEnabled(boolean enabled) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s != null) s.getPlayerList().setWhiteListEnabled(enabled);
        });
    }

    @Override
    public ArrayList<OfflinePlayer> whitelistedPlayers() {
        ArrayList<OfflinePlayer> list = new ArrayList<>();
        MinecraftServer s = server();
        if (s == null) return list;
        for (String name : s.getPlayerList().getWhitelistedPlayers().getKeys()) {
            GameProfile profile = lookup(name);
            if (profile != null) list.add(new OfflinePlayer(profile.getName(), profile.getId()));
        }
        return list;
    }

    @Override
    public void whitelistAdd(String playerName) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            GameProfile profile = lookup(playerName);
            if (s != null && profile != null)
                s.getPlayerList().getWhitelistedPlayers().addEntry(new UserListWhitelistEntry(profile));
        });
    }

    @Override
    public void whitelistRemove(String playerName) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            GameProfile profile = lookup(playerName);
            if (s != null && profile != null) s.getPlayerList().getWhitelistedPlayers().removeEntry(profile);
        });
    }

    @Override
    public ArrayList<BannedPlayer> bannedPlayers() {
        ArrayList<BannedPlayer> players = new ArrayList<>();
        MinecraftServer s = server();
        if (s == null) return players;
        UserListBans banList = s.getPlayerList().getBannedPlayers();
        for (String name : banList.getKeys()) {
            GameProfile profile = lookup(name);
            players.add(new BannedPlayer(name, profile != null ? profile.getId() : null, null, null, null, null));
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
            s.getPlayerList().getBannedPlayers().addEntry(new UserListBansEntry(profile, null, "VoxelDash", null, reason));
            EntityPlayerMP player = s.getPlayerList().getPlayerByUsername(playerName);
            if (player != null) player.connection.disconnect(text("You have been banned: " + reason));
        });
    }

    @Override
    public void unbanPlayer(String playerName) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            GameProfile profile = lookup(playerName);
            if (s != null && profile != null) s.getPlayerList().getBannedPlayers().removeEntry(profile);
        });
    }

    @Override
    public ArrayList<World> worlds() {
        ArrayList<World> worlds = new ArrayList<>();
        MinecraftServer s = server();
        if (s == null) return worlds;
        for (WorldServer level : s.worlds) {
            worlds.add(convertWorld(level));
        }
        return worlds;
    }

    @Override
    public void setTime(String worldId, long ticks) {
        runOnMainThread(() -> {
            WorldServer level = findLevel(worldId);
            if (level != null) level.setWorldTime(ticks);
        });
    }

    @Override
    public void setWeather(String worldId, Weather weather) {
        runOnMainThread(() -> {
            WorldServer level = findLevel(worldId);
            if (level == null) return;
            switch (weather) {
                case CLEAR:
                    level.getWorldInfo().setRaining(false);
                    level.getWorldInfo().setThundering(false);
                    level.getWorldInfo().setCleanWeatherTime(6000);
                    break;
                case RAIN:
                    level.getWorldInfo().setRaining(true);
                    level.getWorldInfo().setThundering(false);
                    level.getWorldInfo().setRainTime(6000);
                    break;
                case THUNDER:
                    level.getWorldInfo().setRaining(true);
                    level.getWorldInfo().setThundering(true);
                    level.getWorldInfo().setThunderTime(6000);
                    break;
            }
        });
    }

    @Override
    public void setDifficulty(String worldId, String difficulty) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            try {
                EnumDifficulty diff = EnumDifficulty.valueOf(difficulty.toUpperCase());
                s.setDifficultyForAllWorlds(diff);
            } catch (Exception ignored) {
            }
        });
    }

    @Override
    public void teleportPlayers(String fromWorldId, String toWorldId) {
        runOnMainThread(() -> {
            WorldServer from = findLevel(fromWorldId);
            WorldServer to = findLevel(toWorldId);
            if (from == null || to == null) return;
            BlockPos spawn = to.getSpawnPoint();
            for (net.minecraft.entity.player.EntityPlayer player : new ArrayList<>(from.playerEntities)) {
                if (player instanceof EntityPlayerMP) {
                    player.setPositionAndUpdate(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
                }
            }
        });
    }

    @Override
    public void saveWorld(String worldId) {
        runOnMainThread(() -> {
            WorldServer level = findLevel(worldId);
            if (level == null) return;
            try {
                level.saveAllChunks(true, null);
            } catch (Exception ignored) {
            }
        });
    }

    private WorldServer findLevel(String worldId) {
        MinecraftServer s = server();
        if (s == null) return null;
        for (WorldServer level : s.worlds) {
            String id = dimId(level);
            if (id.equals(worldId) || id.endsWith(":" + worldId)) return level;
        }
        return null;
    }

    private World convertWorld(WorldServer level) {
        String weather = level.isThundering() ? "THUNDER" : level.isRaining() ? "RAIN" : "CLEAR";
        String path = dimId(level).toLowerCase();
        String environment = "NORMAL";
        if ("the_nether".equals(path) || "nether".equals(path)) environment = "NETHER";
        else if ("the_end".equals(path) || "end".equals(path)) environment = "THE_END";

        return new World(
                dimId(level),
                environment,
                level.playerEntities.size(),
                level.getWorldTime(),
                weather,
                level.getWorldInfo().getDifficulty().name(),
                level.getSeed(),
                level.getWorldInfo().isHardcoreModeEnabled(),
                "NORMAL"
        );
    }

    @Override
    public WorldStats worldStats() {
        MinecraftServer s = server();
        if (s == null) return new WorldStats(0, 0, 0);
        int worldCount = 0, entities = 0, chunks = 0;
        for (WorldServer level : s.worlds) {
            worldCount++;
            entities += level.loadedEntityList.size();
            chunks += level.getChunkProvider().getLoadedChunkCount();
        }
        return new WorldStats(worldCount, entities, chunks);
    }

    @Override
    public List<ModEntry> loadedMods() {
        List<ModEntry> result = new ArrayList<>();
        for (ModContainer mod : Loader.instance().getActiveModList()) {
            String fileName = mod.getSource() != null ? mod.getSource().getName() : null;
            result.add(new ModEntry(mod.getModId(), mod.getName(), mod.getVersion(), null, null, fileName));
        }
        return result;
    }

    @Override
    public ModEntry parseJarMeta(File jarFile) {
        if (jarFile == null || !jarFile.exists()) return null;
        try (JarFile jar = new JarFile(jarFile)) {
            java.util.jar.JarEntry entry = jar.getJarEntry("mcmod.info");
            if (entry == null) return null;
            try (InputStreamReader reader = new InputStreamReader(jar.getInputStream(entry))) {
                JsonElement root = new JsonParser().parse(reader);
                JsonObject mod = null;
                if (root.isJsonArray()) {
                    JsonArray arr = root.getAsJsonArray();
                    if (arr.size() > 0 && arr.get(0).isJsonObject()) mod = arr.get(0).getAsJsonObject();
                } else if (root.isJsonObject() && root.getAsJsonObject().has("modList")) {
                    JsonArray arr = root.getAsJsonObject().getAsJsonArray("modList");
                    if (arr.size() > 0 && arr.get(0).isJsonObject()) mod = arr.get(0).getAsJsonObject();
                }
                if (mod == null) return null;
                String id = mod.has("modid") ? mod.get("modid").getAsString() : null;
                String name = mod.has("name") ? mod.get("name").getAsString() : null;
                String version = mod.has("version") ? mod.get("version").getAsString() : null;
                String description = mod.has("description") ? mod.get("description").getAsString() : null;
                String[] authors = null;
                if (mod.has("authorList") && mod.get("authorList").isJsonArray()) {
                    JsonArray a = mod.getAsJsonArray("authorList");
                    List<String> list = new ArrayList<>();
                    for (JsonElement e : a) list.add(e.getAsString());
                    authors = list.toArray(new String[0]);
                }
                return new ModEntry(id, name, version, description, authors, null);
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public File modsFolder() {
        return new File(System.getProperty("user.dir"), "mods");
    }

    @Override
    public File configDir() {
        return Loader.instance().getConfigDir();
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
