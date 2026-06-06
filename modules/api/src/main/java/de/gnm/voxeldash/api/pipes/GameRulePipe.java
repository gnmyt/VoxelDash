package de.gnm.voxeldash.api.pipes;

import de.gnm.voxeldash.api.entities.gamerule.GameRule;
import de.gnm.voxeldash.api.entities.gamerule.GameRuleCapabilities;

import java.util.ArrayList;

public interface GameRulePipe extends BasePipe {

    /**
     * @return what this platform can do with game rules (listing, live apply,
     * known defaults).
     */
    GameRuleCapabilities getCapabilities();

    /**
     * Lists every game rule the server currently knows about, with its type and
     * current value.
     *
     * @return the current game rules (possibly empty if the platform cannot
     * enumerate them)
     */
    ArrayList<GameRule> getGameRules();

    /**
     * Sets a game rule to the given value.
     *
     * @param key   the rule key (e.g. {@code keepInventory})
     * @param value the new value, serialized as a string ({@code "true"},
     *              {@code "10"}, ...)
     * @return {@code true} if the change was accepted
     */
    boolean setGameRule(String key, String value);
}
