package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.entities.players.MuteCapabilities;
import de.gnm.voxeldash.api.pipes.players.PunishmentPipe;
import de.gnm.voxeldash.util.ForgeUtil;

public class PunishmentPipeImpl implements PunishmentPipe {

    @Override
    public MuteCapabilities getMuteCapabilities() {
        return new MuteCapabilities(ForgeUtil.compat().muteSupported());
    }

    @Override
    public void tempBan(String playerName, String reason, long expiryMillis) {
        ForgeUtil.compat().tempBan(playerName, reason, expiryMillis);
    }

    @Override
    public void onMuted(String playerName, String reason, long expiryMillis) {
        if (playerName != null) {
            ForgeUtil.compat().whisper(playerName, "You have been muted: " + reason);
        }
    }

    @Override
    public void onUnmuted(String playerName) {
        if (playerName != null) {
            ForgeUtil.compat().whisper(playerName, "You have been unmuted.");
        }
    }
}
