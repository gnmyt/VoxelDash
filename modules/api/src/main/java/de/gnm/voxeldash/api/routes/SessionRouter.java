package de.gnm.voxeldash.api.routes;

import de.gnm.voxeldash.api.annotations.ApiDoc;
import de.gnm.voxeldash.api.annotations.ApiField;
import de.gnm.voxeldash.api.annotations.Method;
import de.gnm.voxeldash.api.annotations.Path;
import de.gnm.voxeldash.api.controller.AccountController;
import de.gnm.voxeldash.api.controller.SessionController;
import de.gnm.voxeldash.api.http.JSONRequest;
import de.gnm.voxeldash.api.http.JSONResponse;
import de.gnm.voxeldash.api.http.Response;

import static de.gnm.voxeldash.api.http.HTTPMethod.POST;

public class SessionRouter extends BaseRoute {

    @ApiDoc(summary = "Create a session", description = "Authenticates with username and password and returns a session token.", tag = "Session")
    @ApiField(name = "username", description = "The account username")
    @ApiField(name = "password", description = "The account password")
    @Path("/session/create")
    @Method(POST)
    public Response createSession(JSONRequest request) {
        request.checkFor("username", "password");
        AccountController accountController = getController(AccountController.class);
        SessionController sessionController = getController(SessionController.class);

        String username = request.get("username");
        String password = request.get("password");

        if (!accountController.isValidPassword(username, password))
            return new JSONResponse().error("Invalid username or password");

        int userId = accountController.getUserId(username);
        String sessionToken = sessionController.generateSessionToken(userId, request.getHeader("User-Agent"));

        return new JSONResponse().add("session", sessionToken);
    }

    @ApiDoc(summary = "Destroy a session", description = "Invalidates the given session token, logging the user out.", tag = "Session")
    @ApiField(name = "session", description = "The session token to destroy")
    @Path("/session/destroy")
    @Method(POST)
    public Response destroySession(JSONRequest request) {
        request.checkFor("session");
        SessionController sessionController = getController(SessionController.class);

        String sessionToken = request.get("session");

        if (!sessionController.isValidToken(sessionToken))
            return new JSONResponse().error("Invalid session token");

        sessionController.destroySession(sessionToken);

        return new JSONResponse().message("Session destroyed");
    }

}
