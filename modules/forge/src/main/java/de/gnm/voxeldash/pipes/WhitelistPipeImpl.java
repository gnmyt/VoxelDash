package de.gnm.voxeldash.pipes;

import com.mojang.authlib.GameProfile;
import de.gnm.voxeldash.VoxelDashMod;
import de.gnm.voxeldash.api.entities.OfflinePlayer;
import de.gnm.voxeldash.api.pipes.players.WhitelistPipe;
import de.gnm.voxeldash.util.ForgeUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.server.players.UserWhiteListEntry;

import java.util.ArrayList;
import java.util.Optional;

public class WhitelistPipeImpl implements WhitelistPipe {

    @Override
    public void setStatus(boolean status) {
        ForgeUtil.runOnMainThread(() -> {
            MinecraftServer server = VoxelDashMod.getServer();
            if (server != null) {
                server.setEnforceWhitelist(status);
                server.getPlayerList().setUsingWhiteList(status);
            }
        });
    }

    @Override
    public boolean getStatus() {
        MinecraftServer server = VoxelDashMod.getServer();
        return server != null && server.isEnforceWhitelist();
    }

    @Override
    public ArrayList<OfflinePlayer> getWhitelistedPlayers() {
        ArrayList<OfflinePlayer> whitelist = new ArrayList<>();
        MinecraftServer server = VoxelDashMod.getServer();

        if (server == null) {
            return whitelist;
        }

        UserWhiteList whitelistObj = server.getPlayerList().getWhiteList();
        String[] whitelistNames = whitelistObj.getUserList();

        for (String name : whitelistNames) {
            Optional<GameProfile> profileOpt = server.getProfileCache().get(name);
            if (profileOpt.isPresent()) {
                GameProfile profile = profileOpt.get();
                whitelist.add(new OfflinePlayer(profile.getName(), profile.getId()));
            }
        }

        return whitelist;
    }

    @Override
    public void addPlayer(String playerName) {
        ForgeUtil.runOnMainThread(() -> {
            MinecraftServer server = VoxelDashMod.getServer();
            if (server == null) return;

            Optional<GameProfile> profileOpt = server.getProfileCache().get(playerName);
            profileOpt.ifPresent(profile ->
                    server.getPlayerList().getWhiteList().add(new UserWhiteListEntry(profile)));
        });
    }

    @Override
    public void removePlayer(String playerName) {
        ForgeUtil.runOnMainThread(() -> {
            MinecraftServer server = VoxelDashMod.getServer();
            if (server == null) return;

            Optional<GameProfile> profileOpt = server.getProfileCache().get(playerName);
            profileOpt.ifPresent(profile -> server.getPlayerList().getWhiteList().remove(profile));
        });
    }
}
