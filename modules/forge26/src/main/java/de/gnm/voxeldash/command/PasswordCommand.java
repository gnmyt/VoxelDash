package de.gnm.voxeldash.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import de.gnm.voxeldash.VoxelDashLoader;
import de.gnm.voxeldash.VoxelDashMod;
import de.gnm.voxeldash.api.controller.AccountController;
import de.gnm.voxeldash.api.controller.PermissionController;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;

public class PasswordCommand {

    private static final int MIN_PASSWORD_LENGTH = 6;

    private PasswordCommand() {
    }

    /**
     * Registers the {@code /voxeldash password <password>} command
     *
     * @param dispatcher the command dispatcher from {@code RegisterCommandsEvent}
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("voxeldash")
                .then(Commands.literal("password")
                        .then(Commands.argument("password", greedyString())
                                .executes(PasswordCommand::execute))));
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }

        if (!VoxelDashMod.getServer().getPlayerList().isOp(new NameAndId(player.getGameProfile()))) {
            source.sendFailure(Component.literal("You must be a server operator to manage your VoxelDash account."));
            return 0;
        }

        String password = getString(context, "password");
        if (password.length() < MIN_PASSWORD_LENGTH) {
            source.sendFailure(Component.literal("Your password must be at least " + MIN_PASSWORD_LENGTH + " characters long."));
            return 0;
        }

        VoxelDashLoader loader = VoxelDashMod.getInstance().getLoader();
        if (loader == null) {
            source.sendFailure(Component.literal("VoxelDash is not ready yet. Please try again in a moment."));
            return 0;
        }

        String username = player.getName().getString();
        AccountController accounts = loader.getController(AccountController.class);

        try {
            if (accounts.accountExists(username)) {
                accounts.changePassword(username, password);
                source.sendSuccess(() -> Component.literal("Your VoxelDash password has been updated. Log in with the username " + username + "."), false);
            } else {
                accounts.createAccount(username, password);
                int userId = accounts.getUserId(username);
                loader.getController(PermissionController.class).initializePermissions(userId);
                source.sendSuccess(() -> Component.literal("A VoxelDash account has been created for " + username + ". Use it to log in at the web interface."), false);
            }
        } catch (Exception e) {
            source.sendFailure(Component.literal("Something went wrong while updating your VoxelDash account."));
            return 0;
        }

        return 1;
    }
}
