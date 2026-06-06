package de.gnm.voxeldash.api.entities.motd;

public class MotdCapabilities {

    /**
     * Maximum number of lines the platform renders (always 2 for the vanilla
     * server list).
     */
    public int maxLines = 2;

    /**
     * Whether arbitrary {@code #RRGGBB} hex colors are rendered. When
     * {@code false} the editor downgrades hex to the nearest named color.
     */
    public boolean hex;

    /**
     * Whether bold/italic/underline/strikethrough/obfuscated are supported.
     */
    public boolean styles = true;

    /**
     * Whether a custom server-list icon (favicon) can be applied.
     */
    public boolean favicon = true;

    /**
     * Whether changes take effect on the running server without a restart.
     */
    public boolean liveApply;

    /**
     * Whether a server restart is required for changes to show up.
     */
    public boolean requiresRestart = true;

    public MotdCapabilities() {
    }

    public MotdCapabilities(int maxLines, boolean hex, boolean styles, boolean favicon,
                            boolean liveApply, boolean requiresRestart) {
        this.maxLines = maxLines;
        this.hex = hex;
        this.styles = styles;
        this.favicon = favicon;
        this.liveApply = liveApply;
        this.requiresRestart = requiresRestart;
    }
}
