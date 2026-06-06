package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.entities.OfflinePlayer;
import de.gnm.voxeldash.api.entities.OnlinePlayer;
import de.gnm.voxeldash.api.entities.players.PlayerProfile;
import de.gnm.voxeldash.api.helper.OfflinePlayerReader;
import de.gnm.voxeldash.api.pipes.players.ProfilePipe;
import de.gnm.voxeldash.util.FabricUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;

public class ProfilePipeImpl implements ProfilePipe {

    private OfflinePlayerReader reader;

    private OfflinePlayerReader reader() {
        if (reader == null) {
            reader = new OfflinePlayerReader(new File(System.getProperty("user.dir")));
        }
        return reader;
    }

    @Override
    public PlayerProfile getProfile(UUID uuid, boolean online) {
        if (online) {
            for (OnlinePlayer p : FabricUtil.compat().onlinePlayers()) {
                if (uuid.equals(p.getUuid())) {
                    PlayerProfile profile = new PlayerProfile();
                    profile.uuid = uuid.toString();
                    profile.name = p.getName();
                    profile.online = true;
                    profile.health = p.getHealth();
                    profile.foodLevel = p.getHunger();
                    profile.gamemode = p.getGamemode();
                    profile.dimension = p.getWorld();
                    profile.op = p.isOp();
                    profile.playtimeMillis = p.getPlaytime();
                    return profile;
                }
            }
        }
        return reader().readProfile(uuid, null);
    }

    @Override
    public ArrayList<OfflinePlayer> listAllPlayers() {
        return reader().listAllPlayers();
    }
}
