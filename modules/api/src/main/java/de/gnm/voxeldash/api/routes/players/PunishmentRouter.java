package de.gnm.voxeldash.api.routes.players;

import de.gnm.voxeldash.api.annotations.*;
import de.gnm.voxeldash.api.controller.AccountController;
import de.gnm.voxeldash.api.controller.PlayerDataController;
import de.gnm.voxeldash.api.entities.Feature;
import de.gnm.voxeldash.api.entities.PermissionLevel;
import de.gnm.voxeldash.api.http.JSONRequest;
import de.gnm.voxeldash.api.http.JSONResponse;
import de.gnm.voxeldash.api.pipes.players.PunishmentPipe;
import de.gnm.voxeldash.api.routes.BaseRoute;

import java.util.UUID;

import static de.gnm.voxeldash.api.http.HTTPMethod.GET;
import static de.gnm.voxeldash.api.http.HTTPMethod.POST;

public class PunishmentRouter extends BaseRoute {

    @ApiDoc(summary = "Get a player's mute status", description = "Returns whether the player is currently muted (a cheap database lookup), so the UI can show mute or unmute appropriately.", tag = "Players")
    @ApiField(name = "uuid", in = ParamLocation.QUERY, description = "UUID of the player")
    @AuthenticatedRoute
    @RequiresFeatures(Feature.Players)
    @Path("/players/mute/status")
    @Method(GET)
    public JSONResponse muteStatus(JSONRequest request) {
        if (!request.has("uuid")) {
            return new JSONResponse().add("muted", (Object) Boolean.FALSE);
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(request.get("uuid"));
        } catch (Exception e) {
            return new JSONResponse().add("muted", (Object) Boolean.FALSE);
        }
        long expiry = getController(PlayerDataController.class).getMuteExpiry(uuid);
        return new JSONResponse().add("muted", (Object) (expiry != -1));
    }

    @ApiDoc(summary = "Temporarily ban a player", description = "Bans a player until a given time. Uses the platform's native ban list where available.", tag = "Players")
    @ApiField(name = "playerName", description = "Name of the player to ban")
    @ApiField(name = "uuid", required = false, description = "UUID of the player (used to record the action in the player's history)")
    @ApiField(name = "expiry", type = FieldType.NUMBER, description = "When the ban expires (epoch milliseconds, must be in the future)")
    @ApiField(name = "reason", required = false, description = "Ban reason")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Players, level = PermissionLevel.FULL)
    @Path("/players/tempban")
    @Method(POST)
    public JSONResponse tempBan(JSONRequest request) {
        request.checkFor("playerName", "expiry");
        PunishmentPipe pipe = getPipeOrNull(PunishmentPipe.class);
        if (pipe == null) {
            return new JSONResponse().error("Temporary bans are not supported on this platform.");
        }

        String playerName = request.get("playerName");
        long expiry = request.getJson("expiry").asLong();
        if (expiry <= System.currentTimeMillis()) {
            return new JSONResponse().error("The ban expiry must be in the future.");
        }
        String reason = request.has("reason") ? request.get("reason") : "Banned by administrator";

        pipe.tempBan(playerName, reason, expiry);
        return new JSONResponse().message("Player temporarily banned");
    }

    @ApiDoc(summary = "Mute a player", description = "Mutes a player so their chat is suppressed, optionally until a given time. Mute is enforced by the platform's chat listener and only available where the platform supports it.", tag = "Players")
    @ApiField(name = "uuid", description = "UUID of the player")
    @ApiField(name = "playerName", required = false, description = "Name of the player (used to notify them if online)")
    @ApiField(name = "expiry", type = FieldType.NUMBER, required = false, description = "When the mute expires (epoch milliseconds), or 0 / omitted for permanent")
    @ApiField(name = "reason", required = false, description = "Mute reason")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Players, level = PermissionLevel.FULL)
    @Path("/players/mute")
    @Method(POST)
    public JSONResponse mute(JSONRequest request) {
        request.checkFor("uuid");
        PunishmentPipe pipe = getPipeOrNull(PunishmentPipe.class);
        if (pipe == null || !pipe.getMuteCapabilities().supported) {
            return new JSONResponse().error("Muting is not supported on this platform.");
        }

        UUID uuid = UUID.fromString(request.get("uuid"));
        String playerName = request.has("playerName") ? request.get("playerName") : null;
        long expiry = request.has("expiry") ? request.getJson("expiry").asLong() : 0;
        String reason = request.has("reason") ? request.get("reason") : "Muted by administrator";
        long now = System.currentTimeMillis();

        PlayerDataController data = getController(PlayerDataController.class);
        data.mute(uuid, reason, expiry, resolveUsername(request.getUserId()), now);
        pipe.onMuted(playerName, reason, expiry);

        return new JSONResponse().message("Player muted");
    }

    @ApiDoc(summary = "Unmute a player", description = "Lifts a player's mute.", tag = "Players")
    @ApiField(name = "uuid", description = "UUID of the player")
    @ApiField(name = "playerName", required = false, description = "Name of the player (used to notify them if online)")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Players, level = PermissionLevel.FULL)
    @Path("/players/unmute")
    @Method(POST)
    public JSONResponse unmute(JSONRequest request) {
        request.checkFor("uuid");
        UUID uuid = UUID.fromString(request.get("uuid"));
        String playerName = request.has("playerName") ? request.get("playerName") : null;

        PlayerDataController data = getController(PlayerDataController.class);
        data.unmute(uuid);

        PunishmentPipe pipe = getPipeOrNull(PunishmentPipe.class);
        if (pipe != null) {
            pipe.onUnmuted(playerName);
        }
        return new JSONResponse().message("Player unmuted");
    }

    private String resolveUsername(int userId) {
        if (userId == -1) {
            return "console";
        }
        try {
            String name = getController(AccountController.class).getUsernameById(userId);
            return name != null ? name : "dashboard";
        } catch (Exception e) {
            return "dashboard";
        }
    }
}
