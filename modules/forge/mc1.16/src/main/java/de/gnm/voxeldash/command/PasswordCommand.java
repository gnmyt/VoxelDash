package de.gnm.voxeldash.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import de.gnm.voxeldash.VoxelDashMod;
import de.gnm.voxeldash.util.ForgeUtil;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;

/**
 * Brigadier shell of {@code /voxeldash password <password>} for 1.16.5
 * ({@code CommandSource}, direct {@code sendSuccess}). Hands off to the shared
 * {@link PasswordCommandLogic}.
 */
public class PasswordCommand {

    private PasswordCommand() {
    }

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("voxeldash")
                .then(Commands.literal("password")
                        .then(Commands.argument("password", greedyString())
                                .executes(PasswordCommand::execute))));
    }

    private static int execute(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();

        ServerPlayerEntity player = source.getEntity() instanceof ServerPlayerEntity
                ? (ServerPlayerEntity) source.getEntity() : null;
        if (player == null) {
            source.sendFailure(new StringTextComponent("This command can only be used by a player."));
            return 0;
        }

        String username = player.getName().getString();
        PasswordCommandLogic.handle(
                VoxelDashMod.getInstance().getLoader(),
                username,
                ForgeUtil.compat().isOperator(username),
                getString(context, "password"),
                msg -> source.sendSuccess(new StringTextComponent(msg), false)
        );
        return 1;
    }
}
