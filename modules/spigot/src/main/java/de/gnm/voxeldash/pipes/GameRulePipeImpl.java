package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.entities.gamerule.GameRule;
import de.gnm.voxeldash.api.entities.gamerule.GameRuleCapabilities;
import de.gnm.voxeldash.api.pipes.GameRulePipe;
import de.gnm.voxeldash.util.BukkitUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameRulePipeImpl implements GameRulePipe {

    private final boolean modernApi;

    public GameRulePipeImpl() {
        boolean modern;
        try {
            Class.forName("org.bukkit.GameRule");
            modern = true;
        } catch (ClassNotFoundException e) {
            modern = false;
        }
        this.modernApi = modern;
    }

    @Override
    public GameRuleCapabilities getCapabilities() {
        return new GameRuleCapabilities(true, true, false, modernApi);
    }

    @Override
    public ArrayList<GameRule> getGameRules() {
        ArrayList<GameRule> rules = new ArrayList<>();

        World world = mainWorld();
        if (world == null) return rules;

        if (modernApi) {
            collectModern(world, rules);
        } else {
            collectLegacy(world, rules);
        }

        return rules;
    }

    @Override
    public boolean setGameRule(String key, String value) {
        World world = mainWorld();
        if (world == null) return false;

        AtomicBoolean success = new AtomicBoolean(false);

        BukkitUtil.runOnMainThread(() -> {
            if (modernApi) {
                success.set(setModern(key, value));
            } else {
                success.set(setLegacy(key, value));
            }
        });

        return success.get();
    }

    /**
     * Collects rules using the typed {@code org.bukkit.GameRule} enum (1.13+).
     * Everything is done reflectively so the module still compiles against the
     * 1.8 API.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void collectModern(World world, ArrayList<GameRule> rules) {
        try {
            Class<?> gameRuleClass = Class.forName("org.bukkit.GameRule");
            Object[] values = (Object[]) gameRuleClass.getMethod("values").invoke(null);

            Method getName = gameRuleClass.getMethod("getName");
            Method getType = gameRuleClass.getMethod("getType");
            Method getValue = World.class.getMethod("getGameRuleValue", gameRuleClass);
            Method getDefault = World.class.getMethod("getGameRuleDefault", gameRuleClass);

            for (Object rule : values) {
                String key = (String) getName.invoke(rule);
                Class<?> typeClass = (Class<?>) getType.invoke(rule);
                String type = typeClass == Boolean.class ? "BOOLEAN"
                        : (typeClass == Integer.class ? "INTEGER" : "STRING");

                Object current = getValue.invoke(world, rule);
                Object def = getDefault.invoke(world, rule);

                rules.add(new GameRule(key, type,
                        current != null ? String.valueOf(current) : null,
                        def != null ? String.valueOf(def) : null));
            }
        } catch (Throwable ignored) {
            collectLegacy(world, rules);
        }
    }

    /**
     * Collects rules using the legacy string-based API present in every version.
     */
    private void collectLegacy(World world, ArrayList<GameRule> rules) {
        try {
            String[] keys = world.getGameRules();
            if (keys == null) return;
            for (String key : keys) {
                String value = world.getGameRuleValue(key);
                rules.add(new GameRule(key, inferType(value), value, null));
            }
        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean setModern(String key, String value) {
        try {
            Class<?> gameRuleClass = Class.forName("org.bukkit.GameRule");
            Object rule = gameRuleClass.getMethod("getByName", String.class).invoke(null, key);
            if (rule == null) return false;

            Class<?> typeClass = (Class<?>) gameRuleClass.getMethod("getType").invoke(rule);
            Object parsed = parse(typeClass, value);
            if (parsed == null) return false;

            Method setValue = World.class.getMethod("setGameRule", gameRuleClass, Object.class);

            boolean any = false;
            for (World world : Bukkit.getWorlds()) {
                Object ok = setValue.invoke(world, rule, parsed);
                any |= (ok instanceof Boolean) ? (Boolean) ok : true;
            }
            return any;
        } catch (Throwable e) {
            return setLegacy(key, value);
        }
    }

    private boolean setLegacy(String key, String value) {
        boolean any = false;
        for (World world : Bukkit.getWorlds()) {
            try {
                any |= world.setGameRuleValue(key, value);
            } catch (Throwable ignored) {
            }
        }
        return any;
    }

    private Object parse(Class<?> typeClass, String value) {
        if (typeClass == Boolean.class) {
            return Boolean.parseBoolean(value);
        }
        if (typeClass == Integer.class) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return value;
    }

    private String inferType(String value) {
        if (value == null) return "STRING";
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) return "BOOLEAN";
        try {
            Integer.parseInt(value.trim());
            return "INTEGER";
        } catch (NumberFormatException e) {
            return "STRING";
        }
    }

    private World mainWorld() {
        return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
    }
}
