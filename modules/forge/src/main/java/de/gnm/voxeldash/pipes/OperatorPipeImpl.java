package de.gnm.voxeldash.pipes;

import com.mojang.authlib.GameProfile;
import de.gnm.voxeldash.VoxelDashMod;
import de.gnm.voxeldash.api.entities.OfflinePlayer;
import de.gnm.voxeldash.api.pipes.players.OperatorPipe;
import de.gnm.voxeldash.util.ForgeUtil;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Optional;

public class OperatorPipeImpl implements OperatorPipe {

    @Override
    public ArrayList<OfflinePlayer> getOperators() {
        ArrayList<OfflinePlayer> operators = new ArrayList<>();
        MinecraftServer server = VoxelDashMod.getServer();

        if (server == null) {
            return operators;
        }

        String[] opNames = server.getPlayerList().getOps().getUserList();

        for (String name : opNames) {
            Optional<GameProfile> profileOpt = server.getProfileCache().get(name);
            if (profileOpt.isPresent()) {
                GameProfile profile = profileOpt.get();
                operators.add(new OfflinePlayer(profile.getName(), profile.getId()));
            }
        }

        return operators;
    }

    @Override
    public void setOp(String playerName) {
        ForgeUtil.runOnMainThread(() -> {
            MinecraftServer server = VoxelDashMod.getServer();
            if (server == null) return;

            Optional<GameProfile> profileOpt = server.getProfileCache().get(playerName);
            profileOpt.ifPresent(profile -> server.getPlayerList().op(profile));
        });
    }

    @Override
    public void deOp(String playerName) {
        ForgeUtil.runOnMainThread(() -> {
            MinecraftServer server = VoxelDashMod.getServer();
            if (server == null) return;

            Optional<GameProfile> profileOpt = server.getProfileCache().get(playerName);
            profileOpt.ifPresent(profile -> server.getPlayerList().deop(profile));
        });
    }
}
