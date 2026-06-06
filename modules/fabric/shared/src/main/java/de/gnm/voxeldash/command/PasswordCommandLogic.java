package de.gnm.voxeldash.command;

import de.gnm.voxeldash.VoxelDashLoader;
import de.gnm.voxeldash.api.controller.AccountController;
import de.gnm.voxeldash.api.controller.PermissionController;

import java.util.function.Consumer;

public final class PasswordCommandLogic {

    public static final int MIN_PASSWORD_LENGTH = 6;

    private PasswordCommandLogic() {
    }

    public static void handle(VoxelDashLoader loader, String username, boolean isOp, String password, Consumer<String> reply) {
        if (!isOp) {
            reply.accept("You must be a server operator to manage your VoxelDash account.");
            return;
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            reply.accept("Your password must be at least " + MIN_PASSWORD_LENGTH + " characters long.");
            return;
        }
        if (loader == null) {
            reply.accept("VoxelDash is not ready yet. Please try again in a moment.");
            return;
        }

        AccountController accounts = loader.getController(AccountController.class);
        try {
            if (accounts.accountExists(username)) {
                accounts.changePassword(username, password);
                reply.accept("Your VoxelDash password has been updated. Log in with the username " + username + ".");
            } else {
                accounts.createAccount(username, password);
                int userId = accounts.getUserId(username);
                loader.getController(PermissionController.class).initializePermissions(userId);
                reply.accept("A VoxelDash account has been created for " + username + ". Use it to log in at the web interface.");
            }
        } catch (Exception e) {
            reply.accept("Something went wrong while updating your VoxelDash account.");
        }
    }
}
