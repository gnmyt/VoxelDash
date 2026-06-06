package de.gnm.loader.msmp;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.*;

public final class MsmpSupport {

    public static final String PLAYERS = "minecraft:players";
    public static final String PLAYERS_KICK = "minecraft:players/kick";
    public static final String OPERATORS = "minecraft:operators";
    public static final String OPERATORS_ADD = "minecraft:operators/add";
    public static final String OPERATORS_REMOVE = "minecraft:operators/remove";
    public static final String ALLOWLIST = "minecraft:allowlist";
    public static final String ALLOWLIST_ADD = "minecraft:allowlist/add";
    public static final String ALLOWLIST_REMOVE = "minecraft:allowlist/remove";
    public static final String BANS = "minecraft:bans";
    public static final String BANS_ADD = "minecraft:bans/add";
    public static final String BANS_REMOVE = "minecraft:bans/remove";
    public static final String SERVER_STOP = "minecraft:server/stop";
    public static final String USE_ALLOWLIST = "minecraft:serversettings/use_allowlist";
    public static final String USE_ALLOWLIST_SET = "minecraft:serversettings/use_allowlist/set";
    public static final String GAMERULES = "minecraft:gamerules";
    public static final String GAMERULES_UPDATE = "minecraft:gamerules/update";

    private MsmpSupport() {
    }

    public static Map<String, Object> nameRef(String name) {
        Map<String, Object> ref = new HashMap<>();
        ref.put("name", name);
        return ref;
    }

    public static Map<String, Object> params(String field, Object value) {
        return Collections.singletonMap(field, value);
    }

    public static List<JsonNode> elements(JsonNode result, String key) {
        List<JsonNode> elements = new ArrayList<>();
        if (result == null) {
            return elements;
        }
        JsonNode array = result.has(key) ? result.get(key) : result;
        if (array != null && array.isArray()) {
            for (JsonNode element : array) {
                elements.add(element);
            }
        }
        return elements;
    }

    public static JsonNode playerNode(JsonNode entry) {
        return entry != null && entry.has("player") ? entry.get("player") : entry;
    }

    public static String name(JsonNode playerNode) {
        return playerNode != null && playerNode.hasNonNull("name") ? playerNode.get("name").asText() : null;
    }

    public static UUID uuid(JsonNode playerNode, String name) {
        if (playerNode != null && playerNode.hasNonNull("id")) {
            try {
                return UUID.fromString(playerNode.get("id").asText());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes());
    }

    public static Date instant(JsonNode textNode) {
        if (textNode == null || !textNode.isTextual()) {
            return null;
        }
        String text = textNode.asText();
        if (text.isEmpty() || "forever".equalsIgnoreCase(text)) {
            return null;
        }
        try {
            return Date.from(Instant.parse(text));
        } catch (Exception e) {
            return null;
        }
    }
}
