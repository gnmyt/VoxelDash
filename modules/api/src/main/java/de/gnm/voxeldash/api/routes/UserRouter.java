package de.gnm.voxeldash.api.routes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import de.gnm.voxeldash.api.annotations.ApiDoc;
import de.gnm.voxeldash.api.annotations.ApiField;
import de.gnm.voxeldash.api.annotations.AuthenticatedRoute;
import de.gnm.voxeldash.api.annotations.FieldType;
import de.gnm.voxeldash.api.annotations.Method;
import de.gnm.voxeldash.api.annotations.ParamLocation;
import de.gnm.voxeldash.api.annotations.Path;
import de.gnm.voxeldash.api.controller.AccountController;
import de.gnm.voxeldash.api.controller.PermissionController;
import de.gnm.voxeldash.api.controller.SessionController;
import de.gnm.voxeldash.api.entities.Feature;
import de.gnm.voxeldash.api.entities.PermissionLevel;
import de.gnm.voxeldash.api.entities.UserPermissions;
import de.gnm.voxeldash.api.http.JSONRequest;
import de.gnm.voxeldash.api.http.JSONResponse;
import de.gnm.voxeldash.api.http.RawRequest;
import de.gnm.voxeldash.api.http.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.gnm.voxeldash.api.http.HTTPMethod.*;

public class UserRouter extends BaseRoute {

    /**
     * Checks if the requesting user is an admin
     */
    private boolean isRequestingUserAdmin(JSONRequest request) {
        return isAdmin(request.getUserId());
    }

    /**
     * Checks if the requesting user is an admin (for RawRequest)
     */
    private boolean isRequestingUserAdmin(RawRequest request) {
        return isAdmin(request.getUserId());
    }

    /**
     * Checks if a user is an admin
     */
    private boolean isAdmin(int userId) {
        PermissionController permissionController = getController(PermissionController.class);
        return permissionController.isAdmin(userId);
    }

    /**
     * Gets all users with their permissions
     */
    @ApiDoc(summary = "List all users", description = "Returns all users together with their permissions and admin status. Requires admin privileges.", tag = "Users")
    @AuthenticatedRoute
    @Path("/users")
    @Method(GET)
    public Response getUsers(JSONRequest request) {
        if (!isRequestingUserAdmin(request)) {
            return new JSONResponse().error("Access denied. Admin privileges required.");
        }

        AccountController accountController = getController(AccountController.class);
        PermissionController permissionController = getController(PermissionController.class);

        List<Map<String, Object>> users = accountController.getAllAccounts();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> user : users) {
            int userId = (int) user.get("id");
            String username = (String) user.get("username");
            
            UserPermissions permissions = permissionController.getPermissions(userId);
            boolean isAdmin = permissionController.isAdmin(userId);

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", userId);
            userInfo.put("username", username);
            userInfo.put("isAdmin", isAdmin);
            userInfo.put("permissions", permissions.toMap());

            result.add(userInfo);
        }

        return new JSONResponse().add("users", result);
    }

    /**
     * Creates a new user
     */
    @ApiDoc(summary = "Create a user", description = "Creates a new user account with the given username and password and initializes default permissions. Requires admin privileges.", tag = "Users")
    @ApiField(name = "username", description = "Username for the new account (3-32 characters)")
    @ApiField(name = "password", description = "Password for the new account (at least 6 characters)")
    @AuthenticatedRoute
    @Path("/users")
    @Method(POST)
    public Response createUser(JSONRequest request) {
        if (!isRequestingUserAdmin(request)) {
            return new JSONResponse().error("Access denied. Admin privileges required.");
        }

        request.checkFor("username", "password");
        
        AccountController accountController = getController(AccountController.class);
        PermissionController permissionController = getController(PermissionController.class);

        String username = request.get("username");
        String password = request.get("password");

        if (accountController.accountExists(username)) {
            return new JSONResponse().error("User already exists");
        }

        if (username.length() < 3 || username.length() > 32) {
            return new JSONResponse().error("Username must be between 3 and 32 characters");
        }

        if (password.length() < 6) {
            return new JSONResponse().error("Password must be at least 6 characters");
        }

        boolean created = accountController.createAccount(username, password);
        
        if (!created) {
            return new JSONResponse().error("Failed to create user");
        }

        int userId = accountController.getUserId(username);
        permissionController.initializePermissions(userId);

        return new JSONResponse().message("User created successfully");
    }

    /**
     * Updates a user's password
     */
    @ApiDoc(summary = "Update a user's password", description = "Changes the password of the specified user. Requires admin privileges.", tag = "Users")
    @ApiField(name = "userId", type = FieldType.INTEGER, in = ParamLocation.PATH, description = "ID of the user to update")
    @ApiField(name = "password", description = "New password (at least 6 characters)")
    @AuthenticatedRoute
    @Path("/users/:userId/password")
    @Method(PUT)
    public Response updatePassword(JSONRequest request) {
        if (!isRequestingUserAdmin(request)) {
            return new JSONResponse().error("Access denied. Admin privileges required.");
        }

        request.checkFor("password");
        
        int userId = request.getIntParam("userId");
        String password = request.get("password");

        if (password.length() < 6) {
            return new JSONResponse().error("Password must be at least 6 characters");
        }

        AccountController accountController = getController(AccountController.class);
        String username = accountController.getUsernameById(userId);
        
        if (username == null) {
            return new JSONResponse().error("User not found");
        }

        boolean updated = accountController.changePassword(username, password);
        
        if (!updated) {
            return new JSONResponse().error("Failed to update password");
        }

        return new JSONResponse().message("Password updated successfully");
    }

    /**
     * Updates a user's username
     */
    @ApiDoc(summary = "Update a user's username", description = "Changes the username of the specified user. Requires admin privileges.", tag = "Users")
    @ApiField(name = "userId", type = FieldType.INTEGER, in = ParamLocation.PATH, description = "ID of the user to update")
    @ApiField(name = "username", description = "New username (3-32 characters)")
    @AuthenticatedRoute
    @Path("/users/:userId/username")
    @Method(PUT)
    public Response updateUsername(JSONRequest request) {
        if (!isRequestingUserAdmin(request)) {
            return new JSONResponse().error("Access denied. Admin privileges required.");
        }

        request.checkFor("username");
        
        int userId = request.getIntParam("userId");
        String newUsername = request.get("username");

        if (newUsername.length() < 3 || newUsername.length() > 32) {
            return new JSONResponse().error("Username must be between 3 and 32 characters");
        }

        AccountController accountController = getController(AccountController.class);
        String currentUsername = accountController.getUsernameById(userId);
        
        if (currentUsername == null) {
            return new JSONResponse().error("User not found");
        }

        if (accountController.accountExists(newUsername) && !currentUsername.equals(newUsername)) {
            return new JSONResponse().error("Username already taken");
        }

        boolean updated = accountController.changeUsername(currentUsername, newUsername);
        
        if (!updated) {
            return new JSONResponse().error("Failed to update username");
        }

        return new JSONResponse().message("Username updated successfully");
    }

    /**
     * Deletes a user
     */
    @ApiDoc(summary = "Delete a user", description = "Deletes the specified user, destroying all of their sessions and permissions. You cannot delete your own account. Requires admin privileges.", tag = "Users")
    @ApiField(name = "userId", type = FieldType.INTEGER, in = ParamLocation.PATH, description = "ID of the user to delete")
    @AuthenticatedRoute
    @Path("/users/:userId")
    @Method(DELETE)
    public Response deleteUser(RawRequest request) {
        if (!isRequestingUserAdmin(request)) {
            return new JSONResponse().error("Access denied. Admin privileges required.");
        }

        int userId;
        try {
            userId = Integer.parseInt(request.getParameter("userId"));
        } catch (NumberFormatException e) {
            return new JSONResponse().error("Invalid user ID");
        }

        if (userId == request.getUserId()) {
            return new JSONResponse().error("Cannot delete your own account");
        }

        AccountController accountController = getController(AccountController.class);
        PermissionController permissionController = getController(PermissionController.class);
        SessionController sessionController = getController(SessionController.class);

        String username = accountController.getUsernameById(userId);
        
        if (username == null) {
            return new JSONResponse().error("User not found");
        }

        sessionController.destroyAllSessionsForUser(userId);
        
        permissionController.deletePermissions(userId);
        
        boolean deleted = accountController.deleteAccount(username);
        
        if (!deleted) {
            return new JSONResponse().error("Failed to delete user");
        }

        return new JSONResponse().message("User deleted successfully");
    }

    /**
     * Gets the permissions for a user
     */
    @ApiDoc(summary = "Get a user's permissions", description = "Returns the permissions and admin status of the specified user. Requires admin privileges.", tag = "Users")
    @ApiField(name = "userId", type = FieldType.INTEGER, in = ParamLocation.PATH, description = "ID of the user")
    @AuthenticatedRoute
    @Path("/users/:userId/permissions")
    @Method(GET)
    public Response getPermissions(JSONRequest request) {
        if (!isRequestingUserAdmin(request)) {
            return new JSONResponse().error("Access denied. Admin privileges required.");
        }

        int userId = request.getIntParam("userId");
        
        PermissionController permissionController = getController(PermissionController.class);
        AccountController accountController = getController(AccountController.class);
        
        String username = accountController.getUsernameById(userId);
        if (username == null) {
            return new JSONResponse().error("User not found");
        }

        UserPermissions permissions = permissionController.getPermissions(userId);
        boolean isAdmin = permissionController.isAdmin(userId);

        return new JSONResponse()
                .add("userId", userId)
                .add("username", username)
                .add("isAdmin", isAdmin)
                .add("permissions", permissions.toMap());
    }

    /**
     * Updates the permissions for a user
     */
    @ApiDoc(summary = "Update a user's permissions", description = "Updates the per-feature permission levels of the specified user. Requires admin privileges.", tag = "Users")
    @ApiField(name = "userId", type = FieldType.INTEGER, in = ParamLocation.PATH, description = "ID of the user")
    @ApiField(name = "permissions", type = FieldType.OBJECT, required = false, description = "Map of feature name to permission level")
    @AuthenticatedRoute
    @Path("/users/:userId/permissions")
    @Method(PUT)
    public Response updatePermissions(JSONRequest request) {
        if (!isRequestingUserAdmin(request)) {
            return new JSONResponse().error("Access denied. Admin privileges required.");
        }

        int userId = request.getIntParam("userId");

        PermissionController permissionController = getController(PermissionController.class);
        AccountController accountController = getController(AccountController.class);
        
        String username = accountController.getUsernameById(userId);
        if (username == null) {
            return new JSONResponse().error("User not found");
        }

        if (request.has("permissions")) {
            JsonNode permissionsNode = request.getJson("permissions");
            UserPermissions permissions = permissionController.getPermissions(userId);
            
            for (Feature feature : Feature.values()) {
                String featureName = feature.name();
                if (permissionsNode.has(featureName)) {
                    int level = permissionsNode.get(featureName).asInt();
                    permissions.setPermission(feature, PermissionLevel.fromLevel(level));
                }
            }
            
            permissionController.savePermissions(permissions);
        }

        return new JSONResponse().message("Permissions updated successfully");
    }

    /**
     * Gets available features from the server
     */
    @ApiDoc(summary = "List available features", description = "Returns the names of all features available on the server. Requires admin privileges.", tag = "Users")
    @AuthenticatedRoute
    @Path("/users/features")
    @Method(GET)
    public Response getAvailableFeatures(JSONRequest request) {
        if (!isRequestingUserAdmin(request)) {
            return new JSONResponse().error("Access denied. Admin privileges required.");
        }

        List<String> features = new ArrayList<>();
        for (Feature feature : loader.getAvailableFeatures()) {
            features.add(feature.name());
        }

        return new JSONResponse().add("features", features);
    }

    /**
     * Gets the current user's info including permissions
     */
    @ApiDoc(summary = "Get the current user", description = "Returns the authenticated user's info including username, admin status and permissions.", tag = "Users")
    @AuthenticatedRoute
    @Path("/users/me")
    @Method(GET)
    public Response getCurrentUser(JSONRequest request) {
        int userId = request.getUserId();
        
        AccountController accountController = getController(AccountController.class);
        PermissionController permissionController = getController(PermissionController.class);
        
        String username = accountController.getUsernameById(userId);
        UserPermissions permissions = permissionController.getPermissions(userId);
        boolean isAdmin = permissionController.isAdmin(userId);

        return new JSONResponse()
                .add("id", userId)
                .add("username", username)
                .add("isAdmin", isAdmin)
                .add("permissions", permissions.toMap());
    }
}
