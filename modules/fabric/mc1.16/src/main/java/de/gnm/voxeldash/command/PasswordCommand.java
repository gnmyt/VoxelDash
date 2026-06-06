package de.gnm.voxeldash.command;

import com.mojang.brigadier.context.CommandContext;
import de.gnm.voxeldash.VoxelDashMod;
import de.gnm.voxeldash.util.FabricUtil;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class PasswordCommand {

    private PasswordCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) ->
                dispatcher.register(literal("voxeldash")
                        .then(literal("password")
                                .then(argument("password", greedyString())
                                        .executes(PasswordCommand::execute)))));
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayerEntity)) {
            source.sendError(new LiteralText("This command can only be used by a player."));
            return 0;
        }
        ServerPlayerEntity player = (ServerPlayerEntity) source.getEntity();

        String username = player.getName().getString();
        PasswordCommandLogic.handle(
                VoxelDashMod.getInstance().getLoader(),
                username,
                FabricUtil.compat().isOperator(username),
                getString(context, "password"),
                msg -> source.sendFeedback(new LiteralText(msg), false)
        );
        return 1;
    }
}
