package de.gnm.voxeldash.api.pipes;

import de.gnm.voxeldash.api.entities.motd.Motd;
import de.gnm.voxeldash.api.entities.motd.MotdCapabilities;

public interface MotdPipe extends BasePipe {

    /**
     * @return what this platform can do with a MOTD (lines, hex, favicon, live
     * apply, restart requirement).
     */
    MotdCapabilities getCapabilities();

    /**
     * Applies the MOTD to the server. Platforms that intercept the server-list
     * ping update it live; others write the relevant config and rely on a
     * restart (signalled via {@link MotdCapabilities#requiresRestart}).
     *
     * @param motd the canonical MOTD model
     */
    void apply(Motd motd);

    /**
     * Applies (or clears) the server-list icon.
     *
     * @param png the raw PNG bytes of a 64x64 icon, or {@code null} to clear it.
     *            The bytes have already been written to {@code server-icon.png}
     *            by the caller.
     */
    void applyIcon(byte[] png);
}
