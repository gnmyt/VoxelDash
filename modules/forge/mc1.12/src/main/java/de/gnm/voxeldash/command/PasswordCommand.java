package de.gnm.voxeldash.command;

import de.gnm.voxeldash.VoxelDashMod;
import de.gnm.voxeldash.util.ForgeUtil;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

import java.util.Arrays;

/**
 * {@code /voxeldash password <password>} as a 1.12.2 {@link CommandBase}. Hands
 * off to the shared {@link PasswordCommandLogic}; op-gating is via the default
 * CommandBase permission level plus an explicit operator check.
 */
public class PasswordCommand extends CommandBase {

    @Override
    public String getName() {
        return "voxeldash";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/voxeldash password <password>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayerMP)) {
            sender.sendMessage(new TextComponentString("This command can only be used by a player."));
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) sender;

        if (args.length < 2 || !args[0].equalsIgnoreCase("password")) {
            sender.sendMessage(new TextComponentString("Usage: " + getUsage(sender)));
            return;
        }

        String password = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        String username = player.getName();
        PasswordCommandLogic.handle(
                VoxelDashMod.getInstance().getLoader(),
                username,
                ForgeUtil.compat().isOperator(username),
                password,
                msg -> sender.sendMessage(new TextComponentString(msg))
        );
    }
}
