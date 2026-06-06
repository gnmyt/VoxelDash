package de.gnm.voxeldash.api.routes.players;

import com.fasterxml.jackson.databind.JsonNode;
import de.gnm.voxeldash.api.annotations.*;
import de.gnm.voxeldash.api.entities.Feature;
import de.gnm.voxeldash.api.entities.PermissionLevel;
import de.gnm.voxeldash.api.entities.players.InventoryCapabilities;
import de.gnm.voxeldash.api.entities.players.InventoryItem;
import de.gnm.voxeldash.api.http.JSONRequest;
import de.gnm.voxeldash.api.http.JSONResponse;
import de.gnm.voxeldash.api.pipes.players.InventoryPipe;
import de.gnm.voxeldash.api.routes.BaseRoute;

import java.util.UUID;

import static de.gnm.voxeldash.api.http.HTTPMethod.GET;
import static de.gnm.voxeldash.api.http.HTTPMethod.POST;

public class InventoryRouter extends BaseRoute {

    @ApiDoc(summary = "Get a player's inventory", description = "Returns a player's main inventory (including armor and offhand) and ender chest, for online players via the live server and offline players from NBT on disk, along with the platform's inventory capabilities.", tag = "Players")
    @ApiField(name = "uuid", in = ParamLocation.QUERY, description = "UUID of the player")
    @ApiField(name = "online", in = ParamLocation.QUERY, type = FieldType.BOOLEAN, required = false, description = "Whether the player is currently online (read live vs. from disk)")
    @AuthenticatedRoute
    @RequiresFeatures(Feature.Players)
    @Path("/players/inventory")
    @Method(GET)
    public JSONResponse getInventory(JSONRequest request) {
        InventoryPipe pipe = getPipeOrNull(InventoryPipe.class);
        if (pipe == null) {
            return new JSONResponse().add("capabilities", new InventoryCapabilities());
        }

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

        InventoryCapabilities caps = pipe.getCapabilities();
        boolean canView = online ? caps.viewOnline : caps.viewOffline;

        JSONResponse response = new JSONResponse().add("capabilities", caps);
        if (canView) {
            response.add("inventory", pipe.getInventory(uuid, online));
            if (caps.viewEnderChest) {
                response.add("enderchest", pipe.getEnderChest(uuid, online));
            }
        }
        return response;
    }

    @ApiDoc(summary = "Set an inventory slot", description = "Sets or clears a single slot of an online player's inventory or ender chest. Online players only.", tag = "Players")
    @ApiField(name = "uuid", description = "UUID of the (online) player")
    @ApiField(name = "enderChest", type = FieldType.BOOLEAN, required = false, description = "True to target the ender chest, false for the main inventory")
    @ApiField(name = "slot", type = FieldType.INTEGER, description = "Slot index (vanilla NBT convention: 0-8 hotbar, 9-35 main, 100-103 armor, -106 offhand)")
    @ApiField(name = "item", type = FieldType.OBJECT, required = false, description = "The item to place (id, count, ...), or omitted/null to clear the slot")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Players, level = PermissionLevel.FULL)
    @Path("/players/inventory/set")
    @Method(POST)
    public JSONResponse setSlot(JSONRequest request) {
        request.checkFor("uuid", "slot");
        InventoryPipe pipe = requireEditable();
        if (pipe == null) {
            return new JSONResponse().error("Inventory editing is not supported on this platform.");
        }

        UUID uuid = UUID.fromString(request.get("uuid"));
        boolean enderChest = request.has("enderChest") && request.getBoolean("enderChest");
        int slot = request.getInt("slot");

        InventoryItem item = null;
        if (request.has("item") && request.getJson("item") != null && !request.getJson("item").isNull()) {
            try {
                JsonNode node = request.getJson("item");
                item = getMapper().treeToValue(node, InventoryItem.class);
            } catch (Exception e) {
                return new JSONResponse().error("Invalid 'item' payload: " + e.getMessage());
            }
        }

        pipe.setSlot(uuid, enderChest, slot, item);
        return new JSONResponse().message("Slot updated");
    }

    @ApiDoc(summary = "Move/swap an inventory slot", description = "Swaps the contents of two slots of an online player's inventory or ender chest, preserving full item data. Online players only.", tag = "Players")
    @ApiField(name = "uuid", description = "UUID of the (online) player")
    @ApiField(name = "enderChest", type = FieldType.BOOLEAN, required = false, description = "True to target the ender chest, false for the main inventory")
    @ApiField(name = "fromSlot", type = FieldType.INTEGER, description = "Source slot index")
    @ApiField(name = "toSlot", type = FieldType.INTEGER, description = "Destination slot index")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Players, level = PermissionLevel.FULL)
    @Path("/players/inventory/move")
    @Method(POST)
    public JSONResponse moveSlot(JSONRequest request) {
        request.checkFor("uuid", "fromSlot", "toSlot");
        InventoryPipe pipe = requireEditable();
        if (pipe == null) {
            return new JSONResponse().error("Inventory editing is not supported on this platform.");
        }
        UUID uuid = UUID.fromString(request.get("uuid"));
        boolean enderChest = request.has("enderChest") && request.getBoolean("enderChest");
        pipe.moveSlot(uuid, enderChest, request.getInt("fromSlot"), request.getInt("toSlot"));
        return new JSONResponse().message("Slot moved");
    }

    @ApiDoc(summary = "Give an item to a player", description = "Gives an item to an online player (first free inventory slot). Online players only.", tag = "Players")
    @ApiField(name = "uuid", description = "UUID of the (online) player")
    @ApiField(name = "id", description = "Namespaced item id (e.g. minecraft:diamond)")
    @ApiField(name = "count", type = FieldType.INTEGER, required = false, description = "Stack size (default 1)")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Players, level = PermissionLevel.FULL)
    @Path("/players/inventory/give")
    @Method(POST)
    public JSONResponse giveItem(JSONRequest request) {
        request.checkFor("uuid", "id");
        InventoryPipe pipe = requireEditable();
        if (pipe == null) {
            return new JSONResponse().error("Inventory editing is not supported on this platform.");
        }

        UUID uuid = UUID.fromString(request.get("uuid"));
        String id = request.get("id");
        int count = request.has("count") ? request.getInt("count") : 1;

        pipe.giveItem(uuid, id, count);
        return new JSONResponse().message("Item given");
    }

    @ApiDoc(summary = "Clear a player's inventory", description = "Clears an online player's main inventory or ender chest. Online players only.", tag = "Players")
    @ApiField(name = "uuid", description = "UUID of the (online) player")
    @ApiField(name = "enderChest", type = FieldType.BOOLEAN, required = false, description = "True to clear the ender chest, false for the main inventory")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Players, level = PermissionLevel.FULL)
    @Path("/players/inventory/clear")
    @Method(POST)
    public JSONResponse clear(JSONRequest request) {
        request.checkFor("uuid");
        InventoryPipe pipe = requireEditable();
        if (pipe == null) {
            return new JSONResponse().error("Inventory editing is not supported on this platform.");
        }

        UUID uuid = UUID.fromString(request.get("uuid"));
        boolean enderChest = request.has("enderChest") && request.getBoolean("enderChest");

        pipe.clear(uuid, enderChest);
        return new JSONResponse().message("Inventory cleared");
    }

    /**
     * Returns the inventory pipe only if online editing is supported, else null.
     */
    private InventoryPipe requireEditable() {
        InventoryPipe pipe = getPipeOrNull(InventoryPipe.class);
        if (pipe == null || !pipe.getCapabilities().editOnline) {
            return null;
        }
        return pipe;
    }
}
