package de.gnm.loader.pipes;

import de.gnm.voxeldash.api.entities.OfflinePlayer;
import de.gnm.voxeldash.api.entities.players.PlayerProfile;
import de.gnm.voxeldash.api.helper.OfflinePlayerReader;
import de.gnm.voxeldash.api.pipes.players.ProfilePipe;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;

public class ProfilePipeImpl implements ProfilePipe {

    private final OfflinePlayerReader reader;

    public ProfilePipeImpl(File serverRoot) {
        this.reader = new OfflinePlayerReader(serverRoot);
    }

    @Override
    public PlayerProfile getProfile(UUID uuid, boolean online) {
        PlayerProfile profile = reader.readProfile(uuid, null);
        profile.online = online;
        return profile;
    }

    @Override
    public ArrayList<OfflinePlayer> listAllPlayers() {
        return reader.listAllPlayers();
    }
}
