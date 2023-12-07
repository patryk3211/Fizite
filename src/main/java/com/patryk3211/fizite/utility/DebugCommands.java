package com.patryk3211.fizite.utility;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.patryk3211.fizite.simulation.physics.PhysicsStorage;
import com.patryk3211.fizite.simulation.physics.ServerPhysicsStorage;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.*;

public class DebugCommands {
    private static int fizite(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(() -> Text.literal("""
                An argument is required:
                  /fizite record-physics <count> - Records the specified amount of physics simulation frames
                  /fizite times - Print all solver times
                All of the above subcommands can be executed with
                  /fizite-client <subcommand>
                to get the client side solver status.
                """
        ), false);
        return 1;
    }

    private static int fizite_recordPhysics(CommandContext<ServerCommandSource> context) {
        int frameCount = context.getArgument("count", int.class);
        ServerPhysicsStorage.recordFrames(context.getSource().getWorld().getRegistryKey(), frameCount, () -> context.getSource().sendFeedback(() -> Text.literal("Finished physics recording"), false));
        context.getSource().sendFeedback(() -> Text.literal("Started recording of " + frameCount + " physics frames, estimated time " + (frameCount / 20.0f) + "s (assuming 20 physics frames per second)"), false);
        return 1;
    }

    private static int fizite_times(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(() -> PhysicsStorage.get(context.getSource().getWorld()).timingReport(), false);
        return 1;
    }

    @SuppressWarnings("unused")
    public static void registerDebugCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(
                literal("fizite")
                        .requires(source -> source.hasPermissionLevel(4))
                        .executes(DebugCommands::fizite)
                        .then(literal("record-physics")
                            .then(argument("count", IntegerArgumentType.integer(0))
                                    .executes(DebugCommands::fizite_recordPhysics)))
                        .then(literal("times")
                                .executes(DebugCommands::fizite_times)));
    }
}
