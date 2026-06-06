package de.gnm.voxeldash.api.routes.players;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.gnm.voxeldash.api.annotations.*;
import de.gnm.voxeldash.api.controller.PlayerDataController;
import de.gnm.voxeldash.api.entities.Feature;
import de.gnm.voxeldash.api.entities.OfflinePlayer;
import de.gnm.voxeldash.api.entities.players.InventoryCapabilities;
import de.gnm.voxeldash.api.entities.players.MuteCapabilities;
import de.gnm.voxeldash.api.entities.players.PlayerProfile;
import de.gnm.voxeldash.api.entities.players.TeleportCapabilities;
import de.gnm.voxeldash.api.http.JSONRequest;
import de.gnm.voxeldash.api.http.JSONResponse;
import de.gnm.voxeldash.api.pipes.players.InventoryPipe;
import de.gnm.voxeldash.api.pipes.players.ProfilePipe;
import de.gnm.voxeldash.api.pipes.players.PunishmentPipe;
import de.gnm.voxeldash.api.pipes.players.TeleportPipe;
import de.gnm.voxeldash.api.routes.BaseRoute;

import java.util.UUID;

import static de.gnm.voxeldash.api.http.HTTPMethod.GET;

public class ProfileRouter extends BaseRoute {

    @ApiDoc(summary = "List all known players", description = "Returns every player that has ever joined (union of online players, on-disk player data and the user cache) for offline browsing. Empty on platforms without persistent player data.", tag = "Players")
    @AuthenticatedRoute
    @RequiresFeatures(Feature.Players)
    @Path("/players/all")
    @Method(GET)
    public JSONResponse getAllPlayers() {
        ProfilePipe pipe = getPipeOrNull(ProfilePipe.class);
        ArrayNode players = getMapper().createArrayNode();
        if (pipe != null) {
            for (OfflinePlayer player : pipe.listAllPlayers()) {
                ObjectNode node = getMapper().createObjectNode();
                node.put("name", player.getName());
                node.put("uuid", player.getUuid().toString());
                players.add(node);
            }
        }
        return new JSONResponse().add("players", players);
    }

    @ApiDoc(summary = "Get player-management capabilities", description = "Returns the combined inventory, teleport and mute capability flags for this platform so the UI can show only the supported controls.", tag = "Players")
    @AuthenticatedRoute
    @RequiresFeatures(Feature.Players)
    @Path("/players/capabilities")
    @Method(GET)
    public JSONResponse getCapabilities() {
        InventoryPipe inventory = getPipeOrNull(InventoryPipe.class);
        TeleportPipe teleport = getPipeOrNull(TeleportPipe.class);
        PunishmentPipe punishment = getPipeOrNull(PunishmentPipe.class);

        return new JSONResponse()
                .add("inventory", inventory != null ? inventory.getCapabilities() : new InventoryCapabilities())
                .add("teleport", teleport != null ? teleport.getCapabilities() : new TeleportCapabilities())
                .add("mute", punishment != null ? punishment.getMuteCapabilities() : new MuteCapabilities());
    }

    @ApiDoc(summary = "Get a player's profile", description = "Returns a player's profile - identity, playtime, health, gamemode, ban status, first join / last seen - merged with VoxelDash-tracked IP history, moderation history, notes and mute state.", tag = "Players")
    @ApiField(name = "uuid", in = ParamLocation.QUERY, description = "UUID of the player")
    @ApiField(name = "online", in = ParamLocation.QUERY, type = FieldType.BOOLEAN, required = false, description = "Whether the player is currently online")
    @AuthenticatedRoute
    @RequiresFeatures(Feature.Players)
    @Path("/players/profile")
    @Method(GET)
    public JSONResponse getProfile(JSONRequest request) {
        if (!request.has("uuid")) {
            return new JSONResponse().error("A valid 'uuid' query parameter is required.");
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(request.get("uuid"));
        } catch (Exception e) {
            return new JSONResponse().error("A valid 'uuid' query parameter is required.");
        }
        boolean online = request.has("online") && request.getBoolean("online");

        ProfilePipe pipe = getPipeOrNull(ProfilePipe.class);
        PlayerProfile profile = pipe != null ? pipe.getProfile(uuid, online) : null;
        if (profile == null) {
            profile = new PlayerProfile();
            profile.uuid = uuid.toString();
            profile.name = uuid.toString();
            profile.online = online;
        }

        PlayerDataController data = getController(PlayerDataController.class);
        profile.ipHistory = data.getIps(uuid);
        long muteExpiry = data.getMuteExpiry(uuid);
        profile.muted = muteExpiry != -1;
        profile.muteExpiry = profile.muted ? muteExpiry : 0;

        return new JSONResponse().add("profile", profile);
    }
}
