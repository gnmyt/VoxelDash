package de.gnm.voxeldash.util;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.mojang.authlib.GameProfile;
import de.gnm.voxeldash.VoxelDashMod;
import de.gnm.voxeldash.api.entities.BannedPlayer;
import de.gnm.voxeldash.api.entities.OfflinePlayer;
import de.gnm.voxeldash.api.entities.OnlinePlayer;
import de.gnm.voxeldash.api.entities.World;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import de.gnm.voxeldash.api.entities.players.InventoryItem;
import de.gnm.voxeldash.api.entities.players.InventoryView;
import net.minecraft.stats.Stats;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.IModInfo;

import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.Date;

public class VersionCompat implements ForgeCompat {

    private static MinecraftServer server() {
        return VoxelDashMod.getServer();
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
                    player.level().dimension().location().toString(),
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
                BlockPos spawn = target.getSharedSpawnPos();
                player.teleportTo(target, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                        player.getYRot(), player.getXRot());
            }
        });
    }

    @Override
    public ArrayList<OfflinePlayer> operators() {
        ArrayList<OfflinePlayer> ops = new ArrayList<>();
        MinecraftServer s = server();
        if (s == null) return ops;
        for (String name : s.getPlayerList().getOps().getUserList()) {
            s.getProfileCache().get(name).ifPresent(p -> ops.add(new OfflinePlayer(p.getName(), p.getId())));
        }
        return ops;
    }

    @Override
    public void setOp(String playerName) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            s.getProfileCache().get(playerName).ifPresent(p -> s.getPlayerList().op(p));
        });
    }

    @Override
    public void deOp(String playerName) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            s.getProfileCache().get(playerName).ifPresent(p -> s.getPlayerList().deop(p));
        });
    }

    @Override
    public boolean isOperator(String playerName) {
        MinecraftServer s = server();
        if (s == null) return false;
        return s.getProfileCache().get(playerName).map(p -> s.getPlayerList().isOp(p)).orElse(false);
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
            s.getProfileCache().get(name).ifPresent(p -> list.add(new OfflinePlayer(p.getName(), p.getId())));
        }
        return list;
    }

    @Override
    public void whitelistAdd(String playerName) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            s.getProfileCache().get(playerName).ifPresent(p ->
                    s.getPlayerList().getWhiteList().add(new UserWhiteListEntry(p)));
        });
    }

    @Override
    public void whitelistRemove(String playerName) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            s.getProfileCache().get(playerName).ifPresent(p -> s.getPlayerList().getWhiteList().remove(p));
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
                Optional<GameProfile> profileOpt = s.getProfileCache().get(name);
                if (!profileOpt.isPresent()) continue;
                GameProfile profile = profileOpt.get();
                UserBanListEntry entry = banList.get(profile);
                if (entry != null) {
                    players.add(new BannedPlayer(name, profile.getId(), entry.getReason(),
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
            GameProfile profile = s.getProfileCache().get(playerName).orElseGet(() -> new GameProfile(null, playerName));
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
            s.getProfileCache().get(playerName).ifPresent(p -> s.getPlayerList().getBans().remove(p));
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
        runOnMainThread(() -> {
            ServerLevel level = findLevel(worldId);
            if (level != null) level.setDayTime(ticks);
        });
    }

    @Override
    public void setWeather(String worldId, Weather weather) {
        runOnMainThread(() -> {
            ServerLevel level = findLevel(worldId);
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
            ServerLevel from = findLevel(fromWorldId);
            ServerLevel to = findLevel(toWorldId);
            if (from == null || to == null) return;
            BlockPos spawn = to.getSharedSpawnPos();
            for (ServerPlayer player : new ArrayList<>(from.players())) {
                player.teleportTo(to, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                        player.getYRot(), player.getXRot());
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
            String id = level.dimension().location().toString();
            if (id.equals(worldId) || id.endsWith(":" + worldId) || level.dimension().location().getPath().equals(worldId)) {
                return level;
            }
        }
        return null;
    }

    private World convertWorld(ServerLevel level) {
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
                level.getDifficulty().getKey().toUpperCase(),
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
            GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
                @Override
                public void visitBoolean(GameRules.Key<GameRules.BooleanValue> key, GameRules.Type<GameRules.BooleanValue> type) {
                    out.add(new GameRuleEntry(key.getId(), "BOOLEAN", String.valueOf(rules.getBoolean(key))));
                }

                @Override
                public void visitInteger(GameRules.Key<GameRules.IntegerValue> key, GameRules.Type<GameRules.IntegerValue> type) {
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
        for (ServerLevel level : s.getAllLevels()) {
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
            String fileName = null;
            try {
                fileName = info.getOwningFile().getFile().getFileName();
            } catch (Exception ignored) {
            }
            result.add(new ModEntry(info.getModId(), info.getDisplayName(),
                    info.getVersion().toString(), info.getDescription(), null, fileName));
        }
        return result;
    }

    @Override
    public ModEntry parseJarMeta(File jarFile) {
        if (jarFile == null || !jarFile.exists()) return null;
        try (JarFile jar = new JarFile(jarFile)) {
            java.util.jar.JarEntry entry = jar.getJarEntry("META-INF/neoforge.mods.toml");
            if (entry == null) entry = jar.getJarEntry("META-INF/mods.toml");
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
        if (s != null) return s.getWorldPath(LevelResource.DATAPACK_DIR).toFile();
        return new File(System.getProperty("user.dir"), "world/datapacks");
    }

    @Override
    public boolean inventorySupported() {
        return true;
    }

    @Override
    public boolean muteSupported() {
        return true;
    }

    @Override
    public void registerMuteCheck(Predicate<UUID> mutedCheck) {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.ServerChatEvent event) -> {
                    if (event.getPlayer() != null && mutedCheck.test(event.getPlayer().getUUID())) {
                        event.setCanceled(true);
                    }
                });
    }

    @Override
    public InventoryView readInventory(UUID uuid) {
        InventoryView view = new InventoryView("inventory", true);
        runOnMainThread(() -> {
            ServerPlayer p = playerByUuid(uuid);
            if (p == null) return;
            Inventory inv = p.getInventory();
            for (int i = 0; i < inv.items.size(); i++) addItem(view, inv.items.get(i), i);
            for (int i = 0; i < inv.armor.size(); i++) addItem(view, inv.armor.get(i), 100 + i);
            for (int i = 0; i < inv.offhand.size(); i++) addItem(view, inv.offhand.get(i), -106);
        });
        return view;
    }

    @Override
    public InventoryView readEnderChest(UUID uuid) {
        InventoryView view = new InventoryView("enderchest", true);
        runOnMainThread(() -> {
            ServerPlayer p = playerByUuid(uuid);
            if (p == null) return;
            PlayerEnderChestContainer ender = p.getEnderChestInventory();
            for (int i = 0; i < ender.getContainerSize(); i++) addItem(view, ender.getItem(i), i);
        });
        return view;
    }

    @Override
    public void setSlot(UUID uuid, boolean enderChest, int slot, InventoryItem item) {
        runOnMainThread(() -> {
            ServerPlayer p = playerByUuid(uuid);
            if (p == null) return;
            ItemStack stack = fromDto(item);
            if (enderChest) {
                PlayerEnderChestContainer ender = p.getEnderChestInventory();
                if (slot >= 0 && slot < ender.getContainerSize()) ender.setItem(slot, stack);
            } else {
                Inventory inv = p.getInventory();
                if (slot >= 0 && slot < inv.items.size()) inv.items.set(slot, stack);
                else if (slot >= 100 && slot <= 103) inv.armor.set(slot - 100, stack);
                else if (slot == -106) inv.offhand.set(0, stack);
            }
            p.inventoryMenu.broadcastChanges();
        });
    }

    @Override
    public void giveItem(UUID uuid, String id, int count) {
        runOnMainThread(() -> {
            ServerPlayer p = playerByUuid(uuid);
            if (p == null) return;
            ItemStack stack = fromDto(makeDto(id, count));
            if (!stack.isEmpty()) {
                p.getInventory().add(stack);
                p.inventoryMenu.broadcastChanges();
            }
        });
    }

    @Override
    public void clearInventory(UUID uuid, boolean enderChest) {
        runOnMainThread(() -> {
            ServerPlayer p = playerByUuid(uuid);
            if (p == null) return;
            if (enderChest) p.getEnderChestInventory().clearContent();
            else p.getInventory().clearContent();
            p.inventoryMenu.broadcastChanges();
        });
    }

    @Override
    public void moveSlot(UUID uuid, boolean enderChest, int fromSlot, int toSlot) {
        if (fromSlot == toSlot) return;
        runOnMainThread(() -> {
            ServerPlayer p = playerByUuid(uuid);
            if (p == null) return;
            if (enderChest) {
                PlayerEnderChestContainer ender = p.getEnderChestInventory();
                if (fromSlot < 0 || fromSlot >= ender.getContainerSize() || toSlot < 0 || toSlot >= ender.getContainerSize())
                    return;
                ItemStack from = ender.getItem(fromSlot).copy();
                ItemStack to = ender.getItem(toSlot).copy();
                ender.setItem(toSlot, from);
                ender.setItem(fromSlot, to);
            } else {
                ItemStack from = readMainStack(p, fromSlot);
                ItemStack to = readMainStack(p, toSlot);
                writeMainStack(p, toSlot, from);
                writeMainStack(p, fromSlot, to);
            }
            p.inventoryMenu.broadcastChanges();
        });
    }

    private ItemStack readMainStack(ServerPlayer p, int slot) {
        Inventory inv = p.getInventory();
        if (slot >= 0 && slot < inv.items.size()) return inv.items.get(slot).copy();
        if (slot >= 100 && slot <= 103) return inv.armor.get(slot - 100).copy();
        if (slot == -106) return inv.offhand.get(0).copy();
        return ItemStack.EMPTY;
    }

    private void writeMainStack(ServerPlayer p, int slot, ItemStack stack) {
        Inventory inv = p.getInventory();
        if (slot >= 0 && slot < inv.items.size()) inv.items.set(slot, stack);
        else if (slot >= 100 && slot <= 103) inv.armor.set(slot - 100, stack);
        else if (slot == -106) inv.offhand.set(0, stack);
    }

    @Override
    public void tempBan(String playerName, String reason, long expiryMillis) {
        runOnMainThread(() -> {
            MinecraftServer s = server();
            if (s == null) return;
            GameProfile profile = s.getProfileCache().get(playerName).orElseGet(() -> new GameProfile(null, playerName));
            s.getPlayerList().getBans().add(
                    new UserBanListEntry(profile, null, "VoxelDash", new Date(expiryMillis), reason));
            ServerPlayer player = s.getPlayerList().getPlayerByName(playerName);
            if (player != null) player.connection.disconnect(Component.literal("You have been banned: " + reason));
        });
    }

    private ServerPlayer playerByUuid(UUID uuid) {
        MinecraftServer s = server();
        return s != null ? s.getPlayerList().getPlayer(uuid) : null;
    }

    private void addItem(InventoryView view, ItemStack stack, int slot) {
        InventoryItem item = toItem(stack, slot);
        if (item != null) view.items.add(item);
    }

    private InventoryItem toItem(ItemStack stack, int slot) {
        if (stack == null || stack.isEmpty()) return null;
        InventoryItem item = new InventoryItem();
        item.slot = slot;
        item.id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        item.count = stack.getCount();
        item.damage = stack.getDamageValue();
        item.maxDamage = stack.getMaxDamage();
        item.enchanted = stack.isEnchanted();
        return item;
    }

    private InventoryItem makeDto(String id, int count) {
        InventoryItem dto = new InventoryItem();
        dto.id = id;
        dto.count = count;
        return dto;
    }

    private ItemStack fromDto(InventoryItem dto) {
        if (dto == null || dto.id == null) return ItemStack.EMPTY;
        ResourceLocation rl = ResourceLocation.tryParse(dto.id);
        if (rl == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(rl);
        if (item == null) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item, Math.max(1, dto.count));
        if (dto.damage > 0) stack.setDamageValue(dto.damage);
        return stack;
    }
}
