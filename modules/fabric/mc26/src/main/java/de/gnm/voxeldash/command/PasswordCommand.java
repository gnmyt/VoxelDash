package de.gnm.voxeldash.command;

import com.mojang.brigadier.context.CommandContext;
import de.gnm.voxeldash.VoxelDashMod;
import de.gnm.voxeldash.util.FabricUtil;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;

/**
 * Brigadier shell of {@code /voxeldash password <password>} for the 26.x year-major
 * Fabric line. Unlike the 1.x Fabric modules (Yarn {@code ServerCommandSource}) this
 * uses Mojang's {@code CommandSourceStack}, since the 26.x line is mapped with Mojang
 * mappings. Hands off to the shared {@link PasswordCommandLogic}.
 */
public class PasswordCommand {

    private PasswordCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("voxeldash")
                        .then(Commands.literal("password")
                                .then(Commands.argument("password", greedyString())
                                        .executes(PasswordCommand::execute)))));
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }

        String username = player.getName().getString();
        PasswordCommandLogic.handle(
                VoxelDashMod.getInstance().getLoader(),
                username,
                FabricUtil.compat().isOperator(username),
                getString(context, "password"),
                msg -> source.sendSuccess(() -> Component.literal(msg), false)
        );
        return 1;
    }
}
