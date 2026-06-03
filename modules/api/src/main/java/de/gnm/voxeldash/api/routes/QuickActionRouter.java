package de.gnm.voxeldash.api.routes;

import de.gnm.voxeldash.api.annotations.ApiDoc;
import de.gnm.voxeldash.api.annotations.ApiField;
import de.gnm.voxeldash.api.annotations.AuthenticatedRoute;
import de.gnm.voxeldash.api.annotations.Method;
import de.gnm.voxeldash.api.annotations.Path;
import de.gnm.voxeldash.api.annotations.RequiresFeatures;
import de.gnm.voxeldash.api.entities.Feature;
import de.gnm.voxeldash.api.entities.PermissionLevel;
import de.gnm.voxeldash.api.http.JSONRequest;
import de.gnm.voxeldash.api.http.JSONResponse;
import de.gnm.voxeldash.api.pipes.QuickActionPipe;

import static de.gnm.voxeldash.api.http.HTTPMethod.POST;

public class QuickActionRouter extends BaseRoute {

    @ApiDoc(summary = "Execute a console command", description = "Sends the given command to the server console for execution.", tag = "Quick Actions")
    @ApiField(name = "command", description = "The console command to execute")
    @AuthenticatedRoute
    @RequiresFeatures(value = Feature.Console, level = PermissionLevel.FULL)
    @Path("/action/command")
    @Method(POST)
    public JSONResponse executeCommand(JSONRequest request) {
        request.checkFor("command");

        getPipe(QuickActionPipe.class).sendCommand(request.get("command"));

        return new JSONResponse().message("Command sent successfully");
    }

    @ApiDoc(summary = "Reload the server", description = "Triggers a server reload.", tag = "Quick Actions")
    @AuthenticatedRoute
    @Path("/action/reload")
    @Method(POST)
    public JSONResponse reload() {
        getPipe(QuickActionPipe.class).reloadServer();

        return new JSONResponse().message("Server reloaded successfully");
    }

    @ApiDoc(summary = "Shut down the server", description = "Stops the server.", tag = "Quick Actions")
    @AuthenticatedRoute
    @Path("/action/shutdown")
    @Method(POST)
    public JSONResponse shutdown() {
        getPipe(QuickActionPipe.class).stopServer();

        return new JSONResponse().message("Server stopped successfully");
    }

}
