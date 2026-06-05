package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.VoxelDashMod;
import de.gnm.voxeldash.api.entities.BannedPlayer;
import de.gnm.voxeldash.api.pipes.players.BanPipe;
import de.gnm.voxeldash.util.ForgeUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;

import java.io.File;
import java.util.ArrayList;
import java.util.Optional;

public class BanPipeImpl implements BanPipe {

    @Override
    public ArrayList<BannedPlayer> getBannedPlayers() {
        ArrayList<BannedPlayer> players = new ArrayList<>();
        MinecraftServer server = VoxelDashMod.getServer();

        if (server == null) {
            return players;
        }

        File bannedPlayersFile = new File(System.getProperty("user.dir"), "banned-players.json");
        if (!bannedPlayersFile.exists()) {
            return players;
        }

        try {
            UserBanList banList = server.getPlayerList().getBans();
            String[] bannedNames = banList.getUserList();

            for (String name : bannedNames) {
                Optional<NameAndId> profileOpt = server.services().nameToIdCache().get(name);
                if (profileOpt.isPresent()) {
                    NameAndId profile = profileOpt.get();
                    UserBanListEntry entry = banList.get(profile);

                    if (entry != null) {
                        players.add(new BannedPlayer(
                                name,
                                profile.id(),
                                entry.getReason(),
                                entry.getCreated(),
                                entry.getExpires(),
                                entry.getSource()
                        ));
                    }
                }
            }
        } catch (Exception e) {
            VoxelDashMod.getInstance().getLogger().warning("Failed to get banned players: " + e.getMessage());
        }

        return players;
    }

    @Override
    public void banPlayer(String playerName, String reason) {
        ForgeUtil.runOnMainThread(() -> {
            MinecraftServer server = VoxelDashMod.getServer();
            if (server == null) return;

            Optional<NameAndId> profileOpt = server.services().nameToIdCache().get(playerName);
            NameAndId profile = profileOpt.orElseGet(() -> NameAndId.createOffline(playerName));

            UserBanList banList = server.getPlayerList().getBans();
            UserBanListEntry entry = new UserBanListEntry(profile, null, "VoxelDash", null, reason);
            banList.add(entry);

            ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);
            if (player != null) {
                player.connection.disconnect(Component.literal("You have been banned: " + reason));
            }
        });
    }

    @Override
    public void unbanPlayer(String playerName) {
        ForgeUtil.runOnMainThread(() -> {
            MinecraftServer server = VoxelDashMod.getServer();
            if (server == null) return;

            Optional<NameAndId> profileOpt = server.services().nameToIdCache().get(playerName);
            profileOpt.ifPresent(profile -> server.getPlayerList().getBans().remove(profile));
        });
    }
}
