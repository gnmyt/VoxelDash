package de.gnm.voxeldash.command;

import com.mojang.brigadier.context.CommandContext;
import de.gnm.voxeldash.VoxelDashLoader;
import de.gnm.voxeldash.VoxelDashMod;
import de.gnm.voxeldash.api.controller.AccountController;
import de.gnm.voxeldash.api.controller.PermissionController;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class PasswordCommand {

    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int OPERATOR_LEVEL = 4;

    private PasswordCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(literal("voxeldash")
                        .then(literal("password")
                                .then(argument("password", greedyString())
                                        .executes(PasswordCommand::execute)))));
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("This command can only be used by a player."));
            return 0;
        }

        if (!source.hasPermissionLevel(OPERATOR_LEVEL)) {
            source.sendError(Text.literal("You must be a server operator to manage your VoxelDash account."));
            return 0;
        }

        String password = getString(context, "password");
        if (password.length() < MIN_PASSWORD_LENGTH) {
            source.sendError(Text.literal("Your password must be at least " + MIN_PASSWORD_LENGTH + " characters long."));
            return 0;
        }

        VoxelDashLoader loader = VoxelDashMod.getInstance().getLoader();
        if (loader == null) {
            source.sendError(Text.literal("VoxelDash is not ready yet. Please try again in a moment."));
            return 0;
        }

        String username = player.getName().getString();
        AccountController accounts = loader.getController(AccountController.class);

        try {
            if (accounts.accountExists(username)) {
                accounts.changePassword(username, password);
                source.sendFeedback(() -> Text.literal("Your VoxelDash password has been updated. Log in with the username " + username + "."), false);
            } else {
                accounts.createAccount(username, password);
                int userId = accounts.getUserId(username);
                loader.getController(PermissionController.class).initializePermissions(userId);
                source.sendFeedback(() -> Text.literal("A VoxelDash account has been created for " + username + ". Use it to log in at the web interface."), false);
            }
        } catch (Exception e) {
            source.sendError(Text.literal("Something went wrong while updating your VoxelDash account."));
            return 0;
        }

        return 1;
    }
}
