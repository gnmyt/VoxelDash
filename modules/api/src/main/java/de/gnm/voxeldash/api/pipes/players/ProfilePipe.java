package de.gnm.voxeldash.api.pipes.players;

import de.gnm.voxeldash.api.entities.OfflinePlayer;
import de.gnm.voxeldash.api.entities.players.PlayerProfile;
import de.gnm.voxeldash.api.pipes.BasePipe;

import java.util.ArrayList;
import java.util.UUID;

public interface ProfilePipe extends BasePipe {

    /**
     * Builds the server/NBT-derived part of a player's profile.
     *
     * @param uuid   the player's UUID
     * @param online whether the player is currently online
     * @return the profile, or null if nothing is known about the player
     */
    PlayerProfile getProfile(UUID uuid, boolean online);

    /**
     * Lists every player known to the server (union of online players, on-disk
     * player data and the user cache) for offline browsing. May be empty on
     * platforms with no persistent player data (e.g. a proxy).
     *
     * @return the players, with name and UUID populated
     */
    ArrayList<OfflinePlayer> listAllPlayers();
}
