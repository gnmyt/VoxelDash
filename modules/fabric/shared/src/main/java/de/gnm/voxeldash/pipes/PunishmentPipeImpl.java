package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.entities.players.MuteCapabilities;
import de.gnm.voxeldash.api.pipes.players.PunishmentPipe;
import de.gnm.voxeldash.util.FabricUtil;

public class PunishmentPipeImpl implements PunishmentPipe {

    @Override
    public MuteCapabilities getMuteCapabilities() {
        return new MuteCapabilities(FabricUtil.compat().muteSupported());
    }

    @Override
    public void tempBan(String playerName, String reason, long expiryMillis) {
        FabricUtil.compat().tempBan(playerName, reason, expiryMillis);
    }

    @Override
    public void onMuted(String playerName, String reason, long expiryMillis) {
        if (playerName != null) {
            FabricUtil.compat().whisper(playerName, "You have been muted: " + reason);
        }
    }

    @Override
    public void onUnmuted(String playerName) {
        if (playerName != null) {
            FabricUtil.compat().whisper(playerName, "You have been unmuted.");
        }
    }
}
