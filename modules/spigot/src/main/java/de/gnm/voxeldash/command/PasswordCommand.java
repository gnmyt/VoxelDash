package de.gnm.voxeldash.command;

import de.gnm.voxeldash.VoxelDashSpigot;
import de.gnm.voxeldash.api.controller.AccountController;
import de.gnm.voxeldash.api.controller.PermissionController;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PasswordCommand implements CommandExecutor {

    private static final int MIN_PASSWORD_LENGTH = 6;

    private final VoxelDashSpigot plugin;

    public PasswordCommand(VoxelDashSpigot plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }

        if (!player.isOp() && !player.hasPermission("voxeldash.password")) {
            player.sendMessage("§cYou don't have permission to manage your VoxelDash account.");
            return true;
        }

        if (args.length != 2 || !args[0].equalsIgnoreCase("password")) {
            player.sendMessage("§cUsage: /voxeldash password <new-password>");
            return true;
        }

        String password = args[1];
        if (password.length() < MIN_PASSWORD_LENGTH) {
            player.sendMessage("§cYour password must be at least " + MIN_PASSWORD_LENGTH + " characters long.");
            return true;
        }

        String username = player.getName();
        AccountController accounts = plugin.getLoader().getController(AccountController.class);

        try {
            if (accounts.accountExists(username)) {
                accounts.changePassword(username, password);
                player.sendMessage("§aYour VoxelDash password has been updated. Log in with the username §f" + username + "§a.");
            } else {
                accounts.createAccount(username, password);
                int userId = accounts.getUserId(username);
                plugin.getLoader().getController(PermissionController.class).initializePermissions(userId);
                player.sendMessage("§aA VoxelDash account has been created for §f" + username + "§a. Use it to log in at the web interface.");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update VoxelDash account for " + username + ": " + e.getMessage());
            player.sendMessage("§cSomething went wrong while updating your VoxelDash account.");
        }

        return true;
    }
}
