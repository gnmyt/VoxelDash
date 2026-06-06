package de.gnm.voxeldash.api.routes;

import de.gnm.voxeldash.api.annotations.*;
import de.gnm.voxeldash.api.entities.Feature;
import de.gnm.voxeldash.api.entities.PermissionLevel;
import de.gnm.voxeldash.api.entities.gamerule.GameRule;
import de.gnm.voxeldash.api.entities.gamerule.GameRuleCapabilities;
import de.gnm.voxeldash.api.http.JSONRequest;
import de.gnm.voxeldash.api.http.JSONResponse;
import de.gnm.voxeldash.api.pipes.GameRulePipe;

import java.util.ArrayList;

import static de.gnm.voxeldash.api.http.HTTPMethod.GET;
import static de.gnm.voxeldash.api.http.HTTPMethod.POST;

public class GameRuleRouter extends BaseRoute {

    @ApiDoc(summary = "List game rules", description = "Returns every game rule the server currently exposes (extracted from the game itself, including modded rules) together with the platform's game-rule capabilities.", tag = "Game Rules")
    @AuthenticatedRoute
    @Path("/gamerules")
    @RequiresFeatures(Feature.GameRules)
    @Method(GET)
    public JSONResponse getGameRules() {
        GameRulePipe pipe = getPipe(GameRulePipe.class);

        GameRuleCapabilities capabilities = pipe != null ? pipe.getCapabilities() : new GameRuleCapabilities();
        ArrayList<GameRule> rules = pipe != null ? pipe.getGameRules() : new ArrayList<>();

        return new JSONResponse()
                .add("gamerules", rules)
                .add("capabilities", capabilities);
    }

    @ApiDoc(summary = "Set a game rule", description = "Updates a single game rule and applies it to the running server.", tag = "Game Rules")
    @ApiField(name = "key", description = "The rule key (e.g. keepInventory, randomTickSpeed)")
    @ApiField(name = "value", description = "The new value, as a string (e.g. true, false or a number)")
    @AuthenticatedRoute
    @Path("/gamerules")
    @RequiresFeatures(value = Feature.GameRules, level = PermissionLevel.FULL)
    @Method(POST)
    public JSONResponse setGameRule(JSONRequest request) {
        request.checkFor("key", "value");

        String key = request.get("key");
        String value = request.get("value");

        GameRulePipe pipe = getPipe(GameRulePipe.class);
        if (pipe == null) {
            return new JSONResponse().error("Game rules are not supported on this platform.");
        }

        boolean success = pipe.setGameRule(key, value);
        if (!success) {
            return new JSONResponse().error("Failed to set game rule '" + key + "'.");
        }

        return new JSONResponse().message("Game rule updated.");
    }
}
