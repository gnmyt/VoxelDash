package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.VoxelDashMod;
import de.gnm.voxeldash.api.entities.OfflinePlayer;
import de.gnm.voxeldash.api.pipes.players.OperatorPipe;
import de.gnm.voxeldash.util.ForgeUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;

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
            Optional<NameAndId> profileOpt = server.services().nameToIdCache().get(name);
            if (profileOpt.isPresent()) {
                NameAndId profile = profileOpt.get();
                operators.add(new OfflinePlayer(profile.name(), profile.id()));
            }
        }

        return operators;
    }

    @Override
    public void setOp(String playerName) {
        ForgeUtil.runOnMainThread(() -> {
            MinecraftServer server = VoxelDashMod.getServer();
            if (server == null) return;

            Optional<NameAndId> profileOpt = server.services().nameToIdCache().get(playerName);
            profileOpt.ifPresent(profile -> server.getPlayerList().op(profile));
        });
    }

    @Override
    public void deOp(String playerName) {
        ForgeUtil.runOnMainThread(() -> {
            MinecraftServer server = VoxelDashMod.getServer();
            if (server == null) return;

            Optional<NameAndId> profileOpt = server.services().nameToIdCache().get(playerName);
            profileOpt.ifPresent(profile -> server.getPlayerList().deop(profile));
        });
    }
}
