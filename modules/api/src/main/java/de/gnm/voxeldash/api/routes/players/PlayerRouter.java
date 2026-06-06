package de.gnm.voxeldash.api.routes.players;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.gnm.voxeldash.api.annotations.*;
import de.gnm.voxeldash.api.controller.PlayerDataController;
import de.gnm.voxeldash.api.entities.*;
import de.gnm.voxeldash.api.http.JSONRequest;
import de.gnm.voxeldash.api.http.JSONResponse;
import de.gnm.voxeldash.api.pipes.players.*;
import de.gnm.voxeldash.api.routes.BaseRoute;

import static de.gnm.voxeldash.api.http.HTTPMethod.GET;
import static de.gnm.voxeldash.api.http.HTTPMethod.POST;

public class PlayerRouter extends BaseRoute {

    @ApiDoc(summary = "List online players", description = "Returns all currently online players including their name, UUID, world, IP address, health, hunger, operator status, gamemode and playtime.", tag = "Players")
    @AuthenticatedRoute
    @RequiresFeatures(Feature.Players)
    @Path("/players/online")
    @Method(GET)
    public JSONResponse getOnlinePlayers() {
        OnlinePlayerPipe pipe = getPipe(OnlinePlayerPipe.class);
        ArrayNode players = getMapper().createArrayNode();

        PlayerDataController playerData = getController(PlayerDataController.class);
        long now = System.currentTimeMillis();

        for (OnlinePlayer player : pipe.getOnlinePlayers()) {
            ObjectNode playerNode = getMapper().createObjectNode();
            playerNode.put("name", player.getName());
            playerNode.put("uuid", player.getUuid().toString());
            playerNode.put("world", player.getWorld());
            playerNode.put("ipAddress", player.getIpAddress());
            playerNode.put("health", player.getHealth());
            playerNode.put("hunger", player.getHunger());
            playerNode.put("op", player.isOp());
            playerNode.put("gamemode", player.getGamemode());
            playerNode.put("playtime", player.getPlaytime());
            players.add(playerNode);

            try {
                playerData.recordIp(player.getUuid(), normalizeIp(player.getIpAddress()), now);
            } catch (Exception ignored) {
            }
        }

        return new JSONResponse().add("players", players);
    }

    /**
     * Strips a leading slash and any trailing {@code :port} so the stored IP is
     * just the address (platforms report it in different shapes).
     */
    private String normalizeIp(String ip) {
        if (ip == null) {
            return null;
        }
        String value = ip.startsWith("/") ? ip.substring(1) : ip;
        int colon = value.lastIndexOf(':');
        if (colon > 0 && value.indexOf(':') == colon) {
            value = value.substring(0, colon);
        }
        return value;
    }

    @ApiDoc(summary = "Kick a player", description = "Kicks the given online player from the server, optionally with a custom reason.", tag = "Players")
    @ApiField(name = "playerName", description = "Name of the player to kick")
    @ApiField(name = "reason", required = false, description = "Optional kick reason shown to the player (defaults to \"Kicked by administrator\")")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Players, level = PermissionLevel.FULL)
    @Path("/players/kick")
    @Method(POST)
    public JSONResponse kickPlayer(JSONRequest request) {
        request.checkFor("playerName");
        String playerName = request.get("playerName");
        String reason = request.has("reason") ? request.get("reason") : "Kicked by administrator";

        OnlinePlayerPipe pipe = getPipe(OnlinePlayerPipe.class);
        pipe.kickPlayer(playerName, reason);

        return new JSONResponse().message("Player kicked");
    }

    @ApiDoc(summary = "Set a player's gamemode", description = "Changes the gamemode of the given online player.", tag = "Players")
    @ApiField(name = "playerName", description = "Name of the player whose gamemode should be changed")
    @ApiField(name = "gamemode", description = "Target gamemode to apply")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Players, level = PermissionLevel.FULL)
    @Path("/players/gamemode")
    @Method(POST)
    public JSONResponse setGamemode(JSONRequest request) {
        request.checkFor("playerName", "gamemode");
        String playerName = request.get("playerName");
        String gamemode = request.get("gamemode");

        OnlinePlayerPipe pipe = getPipe(OnlinePlayerPipe.class);
        pipe.setGamemode(playerName, gamemode);

        return new JSONResponse().message("Gamemode changed");
    }

    @ApiDoc(summary = "Teleport a player to a world", description = "Teleports the given online player to the specified world.", tag = "Players")
    @ApiField(name = "playerName", description = "Name of the player to teleport")
    @ApiField(name = "worldName", description = "Name of the target world")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Players, level = PermissionLevel.FULL)
    @Path("/players/teleport")
    @Method(POST)
    public JSONResponse teleportToWorld(JSONRequest request) {
        request.checkFor("playerName", "worldName");
        String playerName = request.get("playerName");
        String worldName = request.get("worldName");

        OnlinePlayerPipe pipe = getPipe(OnlinePlayerPipe.class);
        pipe.teleportToWorld(playerName, worldName);

        return new JSONResponse().message("Player teleported");
    }


    @ApiDoc(summary = "List whitelisted players", description = "Returns all whitelisted players (name and UUID) along with whether the whitelist is currently enabled.", tag = "Players")
    @AuthenticatedRoute
    @RequiresFeatures(Feature.Players)
    @Path("/players/whitelist")
    @Method(GET)
    public JSONResponse getWhitelistedPlayers() {
        WhitelistPipe pipe = getPipe(WhitelistPipe.class);
        ArrayNode players = getMapper().createArrayNode();

        for (OfflinePlayer player : pipe.getWhitelistedPlayers()) {
            ObjectNode playerNode = getMapper().createObjectNode();
            playerNode.put("name", player.getName());
            playerNode.put("uuid", player.getUuid().toString());
            players.add(playerNode);
        }

        return new JSONResponse()
                .add("players", players)
                .add("enabled", pipe.getStatus());
    }

    @ApiDoc(summary = "Enable or disable the whitelist", description = "Turns the server whitelist on or off.", tag = "Players")
    @ApiField(name = "enabled", type = FieldType.BOOLEAN, description = "Whether the whitelist should be enabled")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Players, level = PermissionLevel.FULL)
    @Path("/players/whitelist/status")
    @Method(POST)
    public JSONResponse setWhitelistStatus(JSONRequest request) {
        request.checkFor("enabled");
        boolean enabled = request.getBoolean("enabled");

        WhitelistPipe pipe = getPipe(WhitelistPipe.class);
        pipe.setStatus(enabled);

        return new JSONResponse().message("Whitelist status updated");
    }

    @ApiDoc(summary = "Add a player to the whitelist", description = "Adds the given player to the server whitelist.", tag = "Players")
    @ApiField(name = "playerName", description = "Name of the player to add to the whitelist")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Players, level = PermissionLevel.FULL)
    @Path("/players/whitelist/add")
    @Method(POST)
    public JSONResponse addToWhitelist(JSONRequest request) {
        request.checkFor("playerName");
        String playerName = request.get("playerName");

        WhitelistPipe pipe = getPipe(WhitelistPipe.class);
        pipe.addPlayer(playerName);

        return new JSONResponse().message("Player added to whitelist");
    }

    @ApiDoc(summary = "Remove a player from the whitelist", description = "Removes the given player from the server whitelist.", tag = "Players")
    @ApiField(name = "playerName", description = "Name of the player to remove from the whitelist")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Players, level = PermissionLevel.FULL)
    @Path("/players/whitelist/remove")
    @Method(POST)
    public JSONResponse removeFromWhitelist(JSONRequest request) {
        request.checkFor("playerName");
        String playerName = request.get("playerName");

        WhitelistPipe pipe = getPipe(WhitelistPipe.class);
        pipe.removePlayer(playerName);

        return new JSONResponse().message("Player removed from whitelist");
    }


    @ApiDoc(summary = "List banned players", description = "Returns all banned players including name, UUID, ban reason, ban date, expiry and source.", tag = "Players")
    @AuthenticatedRoute
    @RequiresFeatures(Feature.Players)
    @Path("/players/banned")
    @Method(GET)
    public JSONResponse getBannedPlayers() {
        BanPipe pipe = getPipe(BanPipe.class);
        ArrayNode players = getMapper().createArrayNode();

        for (BannedPlayer player : pipe.getBannedPlayers()) {
            ObjectNode playerNode = getMapper().createObjectNode();
            playerNode.put("name", player.getName());
            playerNode.put("uuid", player.getUuid().toString());
            playerNode.put("reason", player.getReason());
            playerNode.put("banDate", player.getBanDate() != null ? player.getBanDate().getTime() : null);
            playerNode.put("expiry", player.getExpiry() != null ? player.getExpiry().getTime() : null);
            playerNode.put("source", player.getSource());
            players.add(playerNode);
        }

        return new JSONResponse().add("players", players);
    }

    @ApiDoc(summary = "Ban a player", description = "Bans the given player from the server, optionally with a custom reason.", tag = "Players")
    @ApiField(name = "playerName", description = "Name of the player to ban")
    @ApiField(name = "reason", required = false, description = "Optional ban reason (defaults to \"Banned by administrator\")")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Players, level = PermissionLevel.FULL)
    @Path("/players/ban")
    @Method(POST)
    public JSONResponse banPlayer(JSONRequest request) {
        request.checkFor("playerName");
        String playerName = request.get("playerName");
        String reason = request.has("reason") ? request.get("reason") : "Banned by administrator";

        BanPipe pipe = getPipe(BanPipe.class);
        pipe.banPlayer(playerName, reason);

        return new JSONResponse().message("Player banned");
    }

    @ApiDoc(summary = "Unban a player", description = "Removes the ban for the given player.", tag = "Players")
    @ApiField(name = "playerName", description = "Name of the player to unban")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Players, level = PermissionLevel.FULL)
    @Path("/players/unban")
    @Method(POST)
    public JSONResponse unbanPlayer(JSONRequest request) {
        request.checkFor("playerName");
        String playerName = request.get("playerName");

        BanPipe pipe = getPipe(BanPipe.class);
        pipe.unbanPlayer(playerName);

        return new JSONResponse().message("Player unbanned");
    }

    @ApiDoc(summary = "Send a private message to a player", description = "Sends a private (whisper) message to an online player, shown in their chat.", tag = "Players")
    @ApiField(name = "playerName", description = "Name of the recipient")
    @ApiField(name = "message", description = "The message text")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Players, level = PermissionLevel.FULL)
    @Path("/players/whisper")
    @Method(POST)
    public JSONResponse whisper(JSONRequest request) {
        request.checkFor("playerName", "message");
        MessagePipe pipe = getPipeOrNull(MessagePipe.class);
        if (pipe == null) {
            return new JSONResponse().error("Messaging is not supported on this platform.");
        }
        pipe.whisper(request.get("playerName"), request.get("message"));
        return new JSONResponse().message("Message sent");
    }


    @ApiDoc(summary = "Set operator status", description = "Grants or revokes operator (op) permissions for the given player.", tag = "Players")
    @ApiField(name = "playerName", description = "Name of the player whose operator status should be changed")
    @ApiField(name = "op", type = FieldType.BOOLEAN, description = "True to grant operator status, false to revoke it")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Players, level = PermissionLevel.FULL)
    @Path("/players/op")
    @Method(POST)
    public JSONResponse setOperator(JSONRequest request) {
        request.checkFor("playerName", "op");
        String playerName = request.get("playerName");
        boolean op = request.getBoolean("op");

        OperatorPipe pipe = getPipe(OperatorPipe.class);
        if (op) {
            pipe.setOp(playerName);
        } else {
            pipe.deOp(playerName);
        }

        return new JSONResponse().message("Operator status updated");
    }

}
