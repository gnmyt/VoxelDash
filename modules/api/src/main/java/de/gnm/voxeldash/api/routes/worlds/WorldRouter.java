package de.gnm.voxeldash.api.routes.worlds;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.gnm.voxeldash.api.annotations.ApiDoc;
import de.gnm.voxeldash.api.annotations.ApiField;
import de.gnm.voxeldash.api.annotations.AuthenticatedRoute;
import de.gnm.voxeldash.api.annotations.Method;
import de.gnm.voxeldash.api.annotations.Path;
import de.gnm.voxeldash.api.annotations.RequiresFeatures;
import de.gnm.voxeldash.api.entities.Feature;
import de.gnm.voxeldash.api.entities.PermissionLevel;
import de.gnm.voxeldash.api.entities.World;
import de.gnm.voxeldash.api.http.JSONRequest;
import de.gnm.voxeldash.api.http.JSONResponse;
import de.gnm.voxeldash.api.pipes.worlds.WorldPipe;
import de.gnm.voxeldash.api.routes.BaseRoute;

import static de.gnm.voxeldash.api.http.HTTPMethod.*;

public class WorldRouter extends BaseRoute {

    @ApiDoc(summary = "List worlds", description = "Returns all loaded worlds with their name, environment, player count, time, weather, difficulty, seed, hardcore flag and world type.", tag = "Worlds")
    @AuthenticatedRoute
    @RequiresFeatures(Feature.Worlds)
    @Path("/worlds")
    @Method(GET)
    public JSONResponse getWorlds() {
        WorldPipe pipe = getPipe(WorldPipe.class);
        ArrayNode worlds = getMapper().createArrayNode();

        for (World world : pipe.getWorlds()) {
            ObjectNode worldNode = getMapper().createObjectNode();
            worldNode.put("name", world.getName());
            worldNode.put("environment", world.getEnvironment());
            worldNode.put("playerCount", world.getPlayerCount());
            worldNode.put("time", world.getTime());
            worldNode.put("weather", world.getWeather());
            worldNode.put("difficulty", world.getDifficulty());
            worldNode.put("seed", world.getSeed());
            worldNode.put("hardcore", world.isHardcore());
            worldNode.put("worldType", world.getWorldType());
            worlds.add(worldNode);
        }

        return new JSONResponse().add("worlds", worlds);
    }

    @ApiDoc(summary = "Set world time", description = "Sets the in-game time of the given world.", tag = "Worlds")
    @ApiField(name = "worldName", description = "Name of the world to update")
    @ApiField(name = "time", description = "Time value to apply (e.g. day, night or a tick value)")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Worlds, level = PermissionLevel.FULL)
    @Path("/worlds/time")
    @Method(POST)
    public JSONResponse setTime(JSONRequest request) {
        request.checkFor("worldName", "time");
        String worldName = request.get("worldName");
        String time = request.get("time");

        WorldPipe pipe = getPipe(WorldPipe.class);
        pipe.setTime(worldName, time);

        return new JSONResponse().message("Time updated");
    }

    @ApiDoc(summary = "Set world weather", description = "Sets the weather of the given world.", tag = "Worlds")
    @ApiField(name = "worldName", description = "Name of the world to update")
    @ApiField(name = "weather", description = "Weather to apply (e.g. clear, rain or thunder)")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Worlds, level = PermissionLevel.FULL)
    @Path("/worlds/weather")
    @Method(POST)
    public JSONResponse setWeather(JSONRequest request) {
        request.checkFor("worldName", "weather");
        String worldName = request.get("worldName");
        String weather = request.get("weather");

        WorldPipe pipe = getPipe(WorldPipe.class);
        pipe.setWeather(worldName, weather);

        return new JSONResponse().message("Weather updated");
    }

    @ApiDoc(summary = "Set world difficulty", description = "Sets the difficulty of the given world.", tag = "Worlds")
    @ApiField(name = "worldName", description = "Name of the world to update")
    @ApiField(name = "difficulty", description = "Difficulty to apply (e.g. peaceful, easy, normal or hard)")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Worlds, level = PermissionLevel.FULL)
    @Path("/worlds/difficulty")
    @Method(POST)
    public JSONResponse setDifficulty(JSONRequest request) {
        request.checkFor("worldName", "difficulty");
        String worldName = request.get("worldName");
        String difficulty = request.get("difficulty");

        WorldPipe pipe = getPipe(WorldPipe.class);
        pipe.setDifficulty(worldName, difficulty);

        return new JSONResponse().message("Difficulty updated");
    }

    @ApiDoc(summary = "Create a world", description = "Creates a new world with the given name and environment. Optionally a world type and seed can be supplied.", tag = "Worlds")
    @ApiField(name = "worldName", description = "Name of the world to create")
    @ApiField(name = "environment", description = "World environment (e.g. NORMAL, NETHER or THE_END)")
    @ApiField(name = "worldType", required = false, description = "World type, defaults to NORMAL")
    @ApiField(name = "seed", required = false, description = "Seed used to generate the world")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Worlds, level = PermissionLevel.FULL)
    @Path("/worlds/create")
    @Method(POST)
    public JSONResponse createWorld(JSONRequest request) {
        request.checkFor("worldName", "environment");
        String worldName = request.get("worldName");
        String environment = request.get("environment");
        String worldType = request.has("worldType") ? request.get("worldType") : "NORMAL";
        String seed = request.has("seed") ? request.get("seed") : null;

        WorldPipe pipe = getPipe(WorldPipe.class);
        boolean success = pipe.createWorld(worldName, environment, worldType, seed);

        if (success) {
            return new JSONResponse().message("World created");
        } else {
            return new JSONResponse().error("Failed to create world");
        }
    }

    @ApiDoc(summary = "Delete a world", description = "Deletes the given world from the server.", tag = "Worlds")
    @ApiField(name = "worldName", description = "Name of the world to delete")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Worlds, level = PermissionLevel.FULL)
    @Path("/worlds/delete")
    @Method(POST)
    public JSONResponse deleteWorld(JSONRequest request) {
        request.checkFor("worldName");
        String worldName = request.get("worldName");

        WorldPipe pipe = getPipe(WorldPipe.class);
        boolean success = pipe.deleteWorld(worldName);

        if (success) {
            return new JSONResponse().message("World deleted");
        } else {
            return new JSONResponse().error("Failed to delete world");
        }
    }

    @ApiDoc(summary = "Save a world", description = "Saves the current state of the given world to disk.", tag = "Worlds")
    @ApiField(name = "worldName", description = "Name of the world to save")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Worlds, level = PermissionLevel.FULL)
    @Path("/worlds/save")
    @Method(POST)
    public JSONResponse saveWorld(JSONRequest request) {
        request.checkFor("worldName");
        String worldName = request.get("worldName");

        WorldPipe pipe = getPipe(WorldPipe.class);
        pipe.saveWorld(worldName);

        return new JSONResponse().message("World saved");
    }

    @ApiDoc(summary = "Teleport players between worlds", description = "Teleports all players from one world to another.", tag = "Worlds")
    @ApiField(name = "fromWorld", description = "Name of the world to teleport players from")
    @ApiField(name = "toWorld", description = "Name of the world to teleport players to")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Worlds, level = PermissionLevel.FULL)
    @Path("/worlds/teleport")
    @Method(POST)
    public JSONResponse teleportPlayers(JSONRequest request) {
        request.checkFor("fromWorld", "toWorld");
        String fromWorld = request.get("fromWorld");
        String toWorld = request.get("toWorld");

        WorldPipe pipe = getPipe(WorldPipe.class);
        pipe.teleportPlayers(fromWorld, toWorld);

        return new JSONResponse().message("Players teleported");
    }
}
