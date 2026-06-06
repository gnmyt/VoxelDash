package de.gnm.voxeldash.api.entities.gamerule;

public class GameRuleCapabilities {

    /**
     * Whether the platform is able to enumerate the available rules and their
     * current values. When {@code false} the UI hides the list (the platform
     * could not read them).
     */
    public boolean canList = true;

    /**
     * Whether a changed rule takes effect on the running server without a
     * restart. Every supported platform applies game rules live.
     */
    public boolean liveApply = true;

    /**
     * Whether a server restart is required for changes to take effect.
     */
    public boolean requiresRestart;

    /**
     * Whether {@link GameRule#defaultValue} is populated (enables a
     * "reset to default" affordance in the UI).
     */
    public boolean knownDefaults;

    public GameRuleCapabilities() {
    }

    public GameRuleCapabilities(boolean canList, boolean liveApply, boolean requiresRestart, boolean knownDefaults) {
        this.canList = canList;
        this.liveApply = liveApply;
        this.requiresRestart = requiresRestart;
        this.knownDefaults = knownDefaults;
    }
}
