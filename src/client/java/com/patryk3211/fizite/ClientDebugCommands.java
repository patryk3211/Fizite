package com.patryk3211.fizite;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.patryk3211.fizite.simulation.ClientPhysicsStorage;
import com.patryk3211.fizite.simulation.physics.PhysicsStorage;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class ClientDebugCommands {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess access) {
        dispatcher.register(literal("fizite-client")
                .then(literal("recordphysics")
                        .then(argument("count", IntegerArgumentType.integer(0))
                                .executes(ClientDebugCommands::fizite_client_recordphysics)))
                .then(literal("times")
                        .executes(ClientDebugCommands::fizite_client_times)));
    }

    private static int fizite_client_recordphysics(CommandContext<FabricClientCommandSource> context) {
        int frameCount = context.getArgument("count", int.class);
        ClientPhysicsStorage.recordClientFrames(frameCount, () -> context.getSource().sendFeedback(Text.literal("Finished physics recording")));
        context.getSource().sendFeedback(Text.literal("Started recording of " + frameCount + " physics frames, estimated time " + (frameCount / 20.0f) + "s (assuming 20 physics frames per second)"));
        return 1;
    }

    private static int fizite_client_times(CommandContext<FabricClientCommandSource> context) {
        final var report = ClientPhysicsStorage.getInstance().timingReport();
        context.getSource().sendFeedback(Text.literal(report));
        return 1;
    }
}
