package de.gnm.voxeldash.api.pipes.players;

import de.gnm.voxeldash.api.entities.players.MuteCapabilities;
import de.gnm.voxeldash.api.pipes.BasePipe;

public interface PunishmentPipe extends BasePipe {

    /**
     * Reports whether this platform can enforce mutes (has a chat hook).
     */
    MuteCapabilities getMuteCapabilities();

    /**
     * Temporarily bans a player until the given time.
     *
     * @param playerName   the player to ban
     * @param reason       the ban reason
     * @param expiryMillis when the ban expires (epoch millis)
     */
    void tempBan(String playerName, String reason, long expiryMillis);

    /**
     * Hook invoked after a mute is written, so the platform can notify the player
     * or refresh any cached state. Implementations may no-op.
     *
     * @param playerName   the muted player
     * @param reason       the mute reason
     * @param expiryMillis when the mute expires (epoch millis), or 0 for permanent
     */
    void onMuted(String playerName, String reason, long expiryMillis);

    /**
     * Hook invoked after a mute is lifted. Implementations may no-op.
     *
     * @param playerName the unmuted player
     */
    void onUnmuted(String playerName);
}
