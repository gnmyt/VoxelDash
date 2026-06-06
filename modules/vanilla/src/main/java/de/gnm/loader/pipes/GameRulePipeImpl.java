package de.gnm.loader.pipes;

import de.gnm.loader.helper.NBTHelper;
import de.gnm.voxeldash.api.entities.gamerule.GameRule;
import de.gnm.voxeldash.api.entities.gamerule.GameRuleCapabilities;
import de.gnm.voxeldash.api.helper.PropertyHelper;
import de.gnm.voxeldash.api.pipes.GameRulePipe;
import net.querz.nbt.tag.CompoundTag;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vanilla game-rule pipe for an out-of-process server.
 * <p>
 * The rule set is extracted from the world save itself: every entry in the
 * {@code GameRules} compound of {@code level.dat} is exposed (so modded and
 * future vanilla rules show up automatically). Changes are applied live through
 * the {@code /gamerule} console command. Because the running server only flushes
 * {@code level.dat} on save, recently changed values are kept in a small overlay
 * so the dashboard reflects them immediately.
 */
public class GameRulePipeImpl implements GameRulePipe {

    private static final Logger LOG = Logger.getLogger("VoxelDashVanilla");

    private final BufferedWriter consoleWriter;
    private final File serverRoot;
    private final Map<String, String> overlay = new ConcurrentHashMap<>();

    public GameRulePipeImpl(OutputStream console, File serverRoot) {
        this.consoleWriter = new BufferedWriter(new OutputStreamWriter(console));
        this.serverRoot = serverRoot;
    }

    @Override
    public GameRuleCapabilities getCapabilities() {
        return new GameRuleCapabilities(true, true, false, false);
    }

    @Override
    public ArrayList<GameRule> getGameRules() {
        ArrayList<GameRule> rules = new ArrayList<>();

        CompoundTag levelData = NBTHelper.readLevelData(mainWorldFolder());
        if (levelData != null && levelData.containsKey("GameRules")) {
            CompoundTag gameRules = levelData.getCompoundTag("GameRules");
            for (String key : gameRules.keySet()) {
                String value = overlay.getOrDefault(key, readString(gameRules, key));
                rules.add(new GameRule(key, inferType(value), value, null));
            }
        }

        for (Map.Entry<String, String> entry : overlay.entrySet()) {
            boolean present = false;
            for (GameRule rule : rules) {
                if (rule.key.equals(entry.getKey())) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                rules.add(new GameRule(entry.getKey(), inferType(entry.getValue()), entry.getValue(), null));
            }
        }

        return rules;
    }

    @Override
    public boolean setGameRule(String key, String value) {
        if (key == null || key.isEmpty() || value == null) return false;
        if (key.contains(" ") || key.contains("\n") || value.contains("\n")) return false;

        try {
            consoleWriter.write("gamerule " + key + " " + value + System.lineSeparator());
            consoleWriter.flush();
            overlay.put(key, value);
            return true;
        } catch (Exception e) {
            LOG.error("Failed to set game rule " + key, e);
            return false;
        }
    }

    private String readString(CompoundTag gameRules, String key) {
        try {
            return gameRules.getString(key);
        } catch (Exception e) {
            return "";
        }
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

    private File mainWorldFolder() {
        String levelName = PropertyHelper.getProperty("level-name");
        if (levelName == null) levelName = "world";
        return new File(serverRoot, levelName);
    }
}
