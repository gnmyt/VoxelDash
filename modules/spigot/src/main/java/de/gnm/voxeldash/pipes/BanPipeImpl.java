package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.entities.BannedPlayer;
import de.gnm.voxeldash.api.pipes.players.BanPipe;
import de.gnm.voxeldash.util.BanCompat;
import de.gnm.voxeldash.util.BukkitUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class BanPipeImpl implements BanPipe {

    @Override
    public ArrayList<BannedPlayer> getBannedPlayers() {
        return new ArrayList<>(BanCompat.listBans());
    }

    @Override
    public void banPlayer(String playerName, String reason) {
        BukkitUtil.runOnMainThread(() -> {
            BanCompat.ban(playerName, reason, "VoxelDash");

            Player player = Bukkit.getPlayerExact(playerName);
            if (player != null) {
                player.kickPlayer("You have been banned: " + reason);
            }
        });
    }

    @Override
    public void unbanPlayer(String playerName) {
        BukkitUtil.runOnMainThread(() -> BanCompat.unban(playerName));
    }

}
