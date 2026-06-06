package de.gnm.voxeldash.api.routes.players;

import de.gnm.voxeldash.api.annotations.*;
import de.gnm.voxeldash.api.entities.Feature;
import de.gnm.voxeldash.api.entities.PermissionLevel;
import de.gnm.voxeldash.api.http.JSONRequest;
import de.gnm.voxeldash.api.http.JSONResponse;
import de.gnm.voxeldash.api.pipes.players.TeleportPipe;
import de.gnm.voxeldash.api.routes.BaseRoute;

import static de.gnm.voxeldash.api.http.HTTPMethod.POST;

public class TeleportRouter extends BaseRoute {

    @ApiDoc(summary = "Teleport a player to coordinates", description = "Teleports an online player to absolute coordinates, optionally in a specific world.", tag = "Players")
    @ApiField(name = "playerName", description = "Name of the player to teleport")
    @ApiField(name = "x", type = FieldType.NUMBER, description = "Target x coordinate")
    @ApiField(name = "y", type = FieldType.NUMBER, description = "Target y coordinate")
    @ApiField(name = "z", type = FieldType.NUMBER, description = "Target z coordinate")
    @ApiField(name = "world", required = false, description = "Target world name (defaults to the player's current world)")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Players, level = PermissionLevel.FULL)
    @Path("/players/teleport/coords")
    @Method(POST)
    public JSONResponse teleportToCoords(JSONRequest request) {
        request.checkFor("playerName", "x", "y", "z");
        TeleportPipe pipe = getPipeOrNull(TeleportPipe.class);
        if (pipe == null || !pipe.getCapabilities().coords) {
            return new JSONResponse().error("Coordinate teleport is not supported on this platform.");
        }
        String world = request.has("world") ? request.get("world") : null;
        pipe.teleportToCoords(request.get("playerName"), request.getDouble("x"), request.getDouble("y"), request.getDouble("z"), world);
        return new JSONResponse().message("Player teleported");
    }

    @ApiDoc(summary = "Teleport a player to another player", description = "Teleports an online player to another online player.", tag = "Players")
    @ApiField(name = "playerName", description = "Name of the player to teleport")
    @ApiField(name = "targetName", description = "Name of the player to teleport to")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Players, level = PermissionLevel.FULL)
    @Path("/players/teleport/player")
    @Method(POST)
    public JSONResponse teleportToPlayer(JSONRequest request) {
        request.checkFor("playerName", "targetName");
        TeleportPipe pipe = getPipeOrNull(TeleportPipe.class);
        if (pipe == null || !pipe.getCapabilities().toPlayer) {
            return new JSONResponse().error("Teleport to player is not supported on this platform.");
        }
        pipe.teleportToPlayer(request.get("playerName"), request.get("targetName"));
        return new JSONResponse().message("Player teleported");
    }

    @ApiDoc(summary = "Teleport a player to spawn", description = "Teleports an online player to the world spawn.", tag = "Players")
    @ApiField(name = "playerName", description = "Name of the player to teleport")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Players, level = PermissionLevel.FULL)
    @Path("/players/teleport/spawn")
    @Method(POST)
    public JSONResponse teleportToSpawn(JSONRequest request) {
        request.checkFor("playerName");
        TeleportPipe pipe = getPipeOrNull(TeleportPipe.class);
        if (pipe == null || !pipe.getCapabilities().toSpawn) {
            return new JSONResponse().error("Teleport to spawn is not supported on this platform.");
        }
        pipe.teleportToSpawn(request.get("playerName"));
        return new JSONResponse().message("Player teleported");
    }

    @ApiDoc(summary = "Send a player to another server", description = "Proxy only: sends a player to another backend server.", tag = "Players")
    @ApiField(name = "playerName", description = "Name of the player to move")
    @ApiField(name = "serverName", description = "Target backend server name")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Players, level = PermissionLevel.FULL)
    @Path("/players/teleport/server")
    @Method(POST)
    public JSONResponse teleportToServer(JSONRequest request) {
        request.checkFor("playerName", "serverName");
        TeleportPipe pipe = getPipeOrNull(TeleportPipe.class);
        if (pipe == null || !pipe.getCapabilities().toServer) {
            return new JSONResponse().error("Server switching is not supported on this platform.");
        }
        pipe.teleportToServer(request.get("playerName"), request.get("serverName"));
        return new JSONResponse().message("Player moved");
    }
}
