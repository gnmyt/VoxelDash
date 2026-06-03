package de.gnm.voxeldash.command;

import de.gnm.voxeldash.VoxelDashBungee;
import de.gnm.voxeldash.api.controller.AccountController;
import de.gnm.voxeldash.api.controller.PermissionController;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class PasswordCommand extends Command {

    private static final int MIN_PASSWORD_LENGTH = 6;

    private final VoxelDashBungee plugin;

    public PasswordCommand(VoxelDashBungee plugin) {
        super("voxeldash");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer player)) {
            sender.sendMessage(new TextComponent("This command can only be used by a player."));
            return;
        }

        if (!player.hasPermission("voxeldash.password")) {
            player.sendMessage(new TextComponent(ChatColor.RED + "You don't have permission to manage your VoxelDash account."));
            return;
        }

        if (args.length != 2 || !args[0].equalsIgnoreCase("password")) {
            player.sendMessage(new TextComponent(ChatColor.RED + "Usage: /voxeldash password <new-password>"));
            return;
        }

        String password = args[1];
        if (password.length() < MIN_PASSWORD_LENGTH) {
            player.sendMessage(new TextComponent(ChatColor.RED + "Your password must be at least " + MIN_PASSWORD_LENGTH + " characters long."));
            return;
        }

        String username = player.getName();
        AccountController accounts = plugin.getLoader().getController(AccountController.class);

        try {
            if (accounts.accountExists(username)) {
                accounts.changePassword(username, password);
                player.sendMessage(new TextComponent(ChatColor.GREEN + "Your VoxelDash password has been updated. Log in with the username " + username + "."));
            } else {
                accounts.createAccount(username, password);
                int userId = accounts.getUserId(username);
                plugin.getLoader().getController(PermissionController.class).initializePermissions(userId);
                player.sendMessage(new TextComponent(ChatColor.GREEN + "A VoxelDash account has been created for " + username + ". Use it to log in at the web interface."));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update VoxelDash account for " + username + ": " + e.getMessage());
            player.sendMessage(new TextComponent(ChatColor.RED + "Something went wrong while updating your VoxelDash account."));
        }
    }
}
