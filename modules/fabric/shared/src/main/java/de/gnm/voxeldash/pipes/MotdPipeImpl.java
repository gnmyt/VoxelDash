package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.entities.motd.Motd;
import de.gnm.voxeldash.api.entities.motd.MotdCapabilities;
import de.gnm.voxeldash.api.helper.PropertyHelper;
import de.gnm.voxeldash.api.helper.motd.LegacyMotdSerializer;
import de.gnm.voxeldash.api.pipes.MotdPipe;

public class MotdPipeImpl implements MotdPipe {

    @Override
    public MotdCapabilities getCapabilities() {
        return new MotdCapabilities(2, false, true, true, false, true);
    }

    @Override
    public void apply(Motd motd) {
        PropertyHelper.setProperty("motd", LegacyMotdSerializer.toLegacy(motd, false));
    }

    @Override
    public void applyIcon(byte[] png) {

    }
}
