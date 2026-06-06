package de.gnm.voxeldash.pipes;

import de.gnm.voxeldash.api.entities.gamerule.GameRule;
import de.gnm.voxeldash.api.entities.gamerule.GameRuleCapabilities;
import de.gnm.voxeldash.api.pipes.GameRulePipe;
import de.gnm.voxeldash.util.ForgeCompat;
import de.gnm.voxeldash.util.ForgeUtil;

import java.util.ArrayList;

public class GameRulePipeImpl implements GameRulePipe {

    @Override
    public GameRuleCapabilities getCapabilities() {
        return new GameRuleCapabilities(true, true, false, false);
    }

    @Override
    public ArrayList<GameRule> getGameRules() {
        ArrayList<GameRule> rules = new ArrayList<>();
        for (ForgeCompat.GameRuleEntry entry : ForgeUtil.compat().gameRules()) {
            rules.add(new GameRule(entry.key, entry.type, entry.value, null));
        }
        return rules;
    }

    @Override
    public boolean setGameRule(String key, String value) {
        if (key == null || key.isEmpty() || value == null) return false;
        if (key.contains(" ") || key.contains("\n") || value.contains("\n")) return false;

        ForgeUtil.compat().runCommand("gamerule " + key + " " + value);
        return true;
    }
}
