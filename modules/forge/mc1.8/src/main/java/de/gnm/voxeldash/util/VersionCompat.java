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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.server.management.UserListBans;
import net.minecraft.server.management.UserListBansEntry;
import net.minecraft.server.management.UserListWhitelistEntry;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

/**
 * {@link ForgeCompat} for the <b>1.8.9</b> line (Forge 11.15.x, Java 8, FG2.1,
 * MCP snapshot_20160312 — oldest supported: {@code IChatComponent},
 * {@code ServerConfigurationManager}, {@code util.BlockPos},
 * {@code WorldSettings.GameType}, {@code worldServers[]}). {@code getConfigurationManager()}
 * has no MCP name in this mapping and is referenced by its SRG name {@code func_71203_ab}.
 */
public class VersionCompat implements ForgeCompat {

    private static MinecraftServer server() {
        return VoxelDashMod.getServer();
    }

    private static ServerConfigurationManager scm() {
        MinecraftServer s = server();
        return s == null ? null : s.func_71203_ab();
    }

    private static GameProfile lookup(String name) {
        MinecraftServer s = server();
        return s == null ? null : s.getPlayerProfileCache().getGameProfileForUsername(name);
    }

    private static IChatComponent text(String message) {
        return new ChatComponentText(message);
    }

    private static String dimId(net.minecraft.world.World level) {
        String folder = level.provider.getSaveFolder();
        if (folder == null) return "overworld";
        if (folder.equals("DIM-1")) return "the_nether";
        if (folder.equals("DIM1")) return "the_end";
        return folder;
    }

    @Override
    public void runOnMainThread(Runnable runnable) {
        MinecraftServer s = server();
        if (s == null || s.isCallingFromMinecraftThread()) {
            runnable.run();
        } else {
            s.addScheduledTask(runnable);
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
        ServerConfigurationManager m = scm();
        return m != null ? m.getCurrentPlayerCount() : 0;
    }

    @Override
    public int maxPlayers() {
        ServerConfigurationManager m = scm();
        return m != null ? m.getMaxPlayers() : 0;
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
            ServerConfigurationManager m = scm();
            if (m != null) m.sendChatMsg(text(message));
        });
    }

    @Override
    public ArrayList<OnlinePlayer> onlinePlayers() {
        ArrayList<OnlinePlayer> players = new ArrayList<>();
        ServerConfigurationManager m = scm();
        if (m == null) return players;

        for (EntityPlayerMP player : new ArrayList<EntityPlayerMP>(m.getPlayerList())) {
            String ip = player.getPlayerIP();
            if (ip != null) {
                if (ip.startsWith("/")) ip = ip.substring(1);
                if (ip.contains(":")) ip = ip.split(":")[0];
            } else {
                ip = "Unknown";
            }
            players.add(new OnlinePlayer(
                    player.getName(),
                    player.getUniqueID(),
                    dimId(player.worldObj),
                    ip,
                    player.getHealth(),
                    player.getFoodStats().getFoodLevel(),
                    m.canSendCommands(player.getGameProfile()),
                    player.interactionManager.getGameType().getName().toUpperCase(),
                    0L
            ));
        }
        return players;
    }

    @Override
    public void kickPlayer(String playerName, String reason) {
        runOnMainThread(() -> {
            ServerConfigurationManager m = scm();
            if (m == null) return;
            EntityPlayerMP player = m.getPlayerByUsername(playerName);
            if (player != null) player.playerNetServerHandler.kickPlayerFromServer(reason);
        });
    }

    @Override
    public void kickAll(String reason) {
        runOnMainThread(() -> {
            ServerConfigurationManager m = scm();
            if (m == null) return;
            for (EntityPlayerMP player : new ArrayList<EntityPlayerMP>(m.getPlayerList())) {
                player.playerNetServerHandler.kickPlayerFromServer(reason);
            }
        });
    }

    @Override
    public void setGamemode(String playerName, String gamemode) {
        runOnMainThread(() -> {
            ServerConfigurationManager m = scm();
            if (m == null) return;
            EntityPlayerMP player = m.getPlayerByUsername(playerName);
            if (player != null) {
                try {
                    WorldSettings.GameType mode = WorldSettings.GameType.getByName(gamemode.toLowerCase());
                    if (mode != WorldSettings.GameType.NOT_SET) player.setGameType(mode);
                } catch (Exception ignored) {
                }
            }
        });
    }

    @Override
    public void teleportToWorld(String playerName, String worldId) {
        runOnMainThread(() -> {
            ServerConfigurationManager m = scm();
            if (m == null) return;
            EntityPlayerMP player = m.getPlayerByUsername(playerName);
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
        ServerConfigurationManager m = scm();
        if (m == null) return ops;
        for (String name : m.getOppedPlayers().getKeys()) {
            GameProfile profile = lookup(name);
            if (profile != null) ops.add(new OfflinePlayer(profile.getName(), profile.getId()));
        }
        return ops;
    }

    @Override
    public void setOp(String playerName) {
        runOnMainThread(() -> {
            ServerConfigurationManager m = scm();
            GameProfile profile = lookup(playerName);
            if (m != null && profile != null) m.addOp(profile);
        });
    }

    @Override
    public void deOp(String playerName) {
        runOnMainThread(() -> {
            ServerConfigurationManager m = scm();
            GameProfile profile = lookup(playerName);
            if (m != null && profile != null) m.removeOp(profile);
        });
    }

    @Override
    public boolean isOperator(String playerName) {
        ServerConfigurationManager m = scm();
        GameProfile profile = lookup(playerName);
        return m != null && profile != null && m.canSendCommands(profile);
    }

    @Override
    public boolean whitelistEnabled() {
        ServerConfigurationManager m = scm();
        return m != null && m.isWhiteListEnabled();
    }

    @Override
    public void setWhitelistEnabled(boolean enabled) {
        runOnMainThread(() -> {
            ServerConfigurationManager m = scm();
            if (m != null) m.setWhiteListEnabled(enabled);
        });
    }

    @Override
    public ArrayList<OfflinePlayer> whitelistedPlayers() {
        ArrayList<OfflinePlayer> list = new ArrayList<>();
        ServerConfigurationManager m = scm();
        if (m == null) return list;
        for (String name : m.getWhitelistedPlayers().getKeys()) {
            GameProfile profile = lookup(name);
            if (profile != null) list.add(new OfflinePlayer(profile.getName(), profile.getId()));
        }
        return list;
    }

    @Override
    public void whitelistAdd(String playerName) {
        runOnMainThread(() -> {
            ServerConfigurationManager m = scm();
            GameProfile profile = lookup(playerName);
            if (m != null && profile != null) m.getWhitelistedPlayers().addEntry(new UserListWhitelistEntry(profile));
        });
    }

    @Override
    public void whitelistRemove(String playerName) {
        runOnMainThread(() -> {
            ServerConfigurationManager m = scm();
            GameProfile profile = lookup(playerName);
            if (m != null && profile != null) m.getWhitelistedPlayers().removeEntry(profile);
        });
    }

    @Override
    public ArrayList<BannedPlayer> bannedPlayers() {
        ArrayList<BannedPlayer> players = new ArrayList<>();
        ServerConfigurationManager m = scm();
        if (m == null) return players;
        UserListBans banList = m.getBannedPlayers();
        for (String name : banList.getKeys()) {
            GameProfile profile = lookup(name);
            players.add(new BannedPlayer(name, profile != null ? profile.getId() : null, null, null, null, null));
        }
        return players;
    }

    @Override
    public void banPlayer(String playerName, String reason) {
        runOnMainThread(() -> {
            ServerConfigurationManager m = scm();
            if (m == null) return;
            GameProfile profile = lookup(playerName);
            if (profile == null) profile = new GameProfile(null, playerName);
            m.getBannedPlayers().addEntry(new UserListBansEntry(profile, null, "VoxelDash", null, reason));
            EntityPlayerMP player = m.getPlayerByUsername(playerName);
            if (player != null) player.playerNetServerHandler.kickPlayerFromServer("You have been banned: " + reason);
        });
    }

    @Override
    public void unbanPlayer(String playerName) {
        runOnMainThread(() -> {
            ServerConfigurationManager m = scm();
            GameProfile profile = lookup(playerName);
            if (m != null && profile != null) m.getBannedPlayers().removeEntry(profile);
        });
    }

    @Override
    public ArrayList<World> worlds() {
        ArrayList<World> worlds = new ArrayList<>();
        MinecraftServer s = server();
        if (s == null) return worlds;
        for (WorldServer level : s.worldServers) {
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
            for (EntityPlayer player : new ArrayList<EntityPlayer>(from.playerEntities)) {
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
        for (WorldServer level : s.worldServers) {
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
        for (WorldServer level : s.worldServers) {
            worldCount++;
            entities += level.loadedEntityList.size();
            if (level.getChunkProvider() instanceof ChunkProviderServer) {
                chunks += ((ChunkProviderServer) level.getChunkProvider()).getLoadedChunkCount();
            }
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
                return new ModEntry(id, name, version, description, null, null);
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
