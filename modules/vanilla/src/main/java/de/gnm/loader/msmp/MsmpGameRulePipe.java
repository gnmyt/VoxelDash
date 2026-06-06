package de.gnm.loader.msmp;

import com.fasterxml.jackson.databind.JsonNode;
import de.gnm.voxeldash.api.entities.gamerule.GameRule;
import de.gnm.voxeldash.api.entities.gamerule.GameRuleCapabilities;
import de.gnm.voxeldash.api.pipes.GameRulePipe;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MsmpGameRulePipe implements GameRulePipe {

    private static final Logger LOG = Logger.getLogger("VoxelDashVanilla");

    private final MsmpClient client;
    private final GameRulePipe fallback;

    public MsmpGameRulePipe(MsmpClient client, GameRulePipe fallback) {
        this.client = client;
        this.fallback = fallback;
    }

    @Override
    public GameRuleCapabilities getCapabilities() {
        return new GameRuleCapabilities(true, true, false, false);
    }

    @Override
    public ArrayList<GameRule> getGameRules() {
        if (client.isConnected()) {
            try {
                JsonNode result = client.call(MsmpSupport.GAMERULES, null);
                ArrayList<GameRule> rules = new ArrayList<>();
                for (JsonNode node : MsmpSupport.elements(result, "gamerules")) {
                    if (!node.hasNonNull("key")) continue;
                    String key = node.get("key").asText();
                    String type = node.hasNonNull("type") ? node.get("type").asText() : "";
                    String value = node.hasNonNull("value") ? node.get("value").asText() : "";
                    rules.add(new GameRule(key, mapType(type), value, null));
                }
                return rules;
            } catch (Exception e) {
                LOG.warn("MSMP getGameRules failed, falling back", e);
            }
        }
        return fallback.getGameRules();
    }

    @Override
    public boolean setGameRule(String key, String value) {
        if (client.isConnected()) {
            try {
                Map<String, Object> rule = new HashMap<>();
                rule.put("key", key);
                rule.put("value", toNative(value));
                client.call(MsmpSupport.GAMERULES_UPDATE, MsmpSupport.params("gamerule", rule));
                return true;
            } catch (Exception e) {
                LOG.warn("MSMP setGameRule failed, falling back", e);
            }
        }
        return fallback.setGameRule(key, value);
    }

    private String mapType(String msmpType) {
        if ("boolean".equalsIgnoreCase(msmpType)) return "BOOLEAN";
        if ("integer".equalsIgnoreCase(msmpType)) return "INTEGER";
        return "STRING";
    }

    /**
     * The protocol requires the value as its native JSON type (a boolean or a
     * number), not a string.
     */
    private Object toNative(String value) {
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return value;
        }
    }
}
