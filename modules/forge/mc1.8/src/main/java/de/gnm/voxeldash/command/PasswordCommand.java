package de.gnm.voxeldash.command;

import de.gnm.voxeldash.VoxelDashMod;
import de.gnm.voxeldash.util.ForgeUtil;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;

import java.util.Arrays;

/**
 * {@code /voxeldash password <password>} as a 1.8.9 {@link CommandBase}. The
 * execute method has no MCP name in this mapping (SRG {@code func_71515_b}).
 * Hands off to the shared {@link PasswordCommandLogic}.
 */
public class PasswordCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "voxeldash";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/voxeldash password <password>";
    }

    @Override
    public void func_71515_b(ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayerMP)) {
            sender.addChatMessage(new ChatComponentText("This command can only be used by a player."));
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) sender;

        if (args.length < 2 || !args[0].equalsIgnoreCase("password")) {
            sender.addChatMessage(new ChatComponentText("Usage: " + getCommandUsage(sender)));
            return;
        }

        String password = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        String username = player.getName();
        PasswordCommandLogic.handle(
                VoxelDashMod.getInstance().getLoader(),
                username,
                ForgeUtil.compat().isOperator(username),
                password,
                msg -> sender.addChatMessage(new ChatComponentText(msg))
        );
    }
}
