package de.gnm.voxeldash.api.routes;

import de.gnm.voxeldash.api.annotations.ApiDoc;
import de.gnm.voxeldash.api.annotations.Path;
import de.gnm.voxeldash.api.http.Response;

public class PingRouter extends BaseRoute {

    @ApiDoc(summary = "Health check", description = "Returns `Pong!` to confirm the server is reachable. No authentication required.", tag = "General")
    @Path("/ping")
    public Response ping() {
        return new Response().raw("Pong!");
    }

}
