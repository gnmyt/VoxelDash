package de.gnm.voxeldash.api.entities.gamerule;

public class GameRule {

    /**
     * The rule key as the game knows it (e.g. {@code doDaylightCycle},
     * {@code randomTickSpeed}).
     */
    public String key;

    /**
     * The value type: {@code BOOLEAN} or {@code INTEGER}. The UI renders a
     * toggle for boolean rules and a number input for integer rules; an unknown
     * type falls back to a plain text input.
     */
    public String type;

    /**
     * The current value, serialized as a string (e.g. {@code "true"} or
     * {@code "3"}).
     */
    public String value;

    /**
     * The game's default value for this rule, serialized as a string, or
     * {@code null} when the platform cannot report it.
     */
    public String defaultValue;

    public GameRule() {
    }

    public GameRule(String key, String type, String value, String defaultValue) {
        this.key = key;
        this.type = type;
        this.value = value;
        this.defaultValue = defaultValue;
    }
}
