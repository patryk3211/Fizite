package com.patryk3211.fizite.utility;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.patryk3211.fizite.simulation.physics.PhysicsStorage;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.*;


public class DebugCommands {
    private static int fizite(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(() -> Text.literal("An argument is required:\n" +
                "/fizite recordphysics <count> - Records the specified amount of physics simulation frames\n" +
                "/fizite-client recordphysics <count> - Records the specified amount of physics simulation frames of client side simulation"
        ), false);
        return 1;
    }

    private static int fizite_recordphysics(CommandContext<ServerCommandSource> context) {
        int frameCount = context.getArgument("count", int.class);
        PhysicsStorage.recordFrames(context.getSource().getWorld().getRegistryKey(), frameCount, () -> context.getSource().sendFeedback(() -> Text.literal("Finished physics recording"), false));
        context.getSource().sendFeedback(() -> Text.literal("Started recording of " + frameCount + " physics frames, estimated time " + (frameCount / 20.0f) + "s (assuming 20 physics frames per second)"), false);
        return 1;
    }

    private static int fizite_times(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(() -> PhysicsStorage.get(context.getSource().getWorld()).timingReport(), false);
        return 1;
    }

    public static void registerDebugCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registry, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(
                literal("fizite")
                        .requires(source -> source.hasPermissionLevel(4))
                        .executes(DebugCommands::fizite)
                        .then(literal("recordphysics")
                            .then(argument("count", IntegerArgumentType.integer(0))
                                    .executes(DebugCommands::fizite_recordphysics)))
                        .then(literal("times")
                                .executes(DebugCommands::fizite_times)));
    }
}
