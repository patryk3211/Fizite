package com.patryk3211.fizite;

import com.patryk3211.fizite.blockentity.AllBlockEntities;
import com.patryk3211.fizite.capability.OneTimeTicker;
import com.patryk3211.fizite.renderer.ConnectingRodRenderer;
import com.patryk3211.fizite.renderer.CrankShaftRenderer;
import com.patryk3211.fizite.renderer.CylinderRenderer;
import com.patryk3211.fizite.renderer.HandCrankRenderer;
import com.patryk3211.fizite.simulation.ClientGasStorage;
import com.patryk3211.fizite.simulation.ClientNetworking;
import com.patryk3211.fizite.simulation.ClientPhysicsStorage;
import com.patryk3211.fizite.simulation.Simulator;
import com.patryk3211.fizite.simulation.physics.SimulationTuner;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public class FiziteClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BlockEntityRendererFactories.register(AllBlockEntities.CYLINDER_ENTITY, CylinderRenderer::new);
		BlockEntityRendererFactories.register(AllBlockEntities.CONNECTING_ROD_ENTITY, ConnectingRodRenderer::new);
		BlockEntityRendererFactories.register(AllBlockEntities.CRANK_SHAFT_ENTITY, CrankShaftRenderer::new);
		BlockEntityRendererFactories.register(AllBlockEntities.HAND_CRANK_ENTITY, HandCrankRenderer::new);

		// Create client storage objects (Their instances are stored statically)
		final var physics = new ClientPhysicsStorage();
		final var gas = new ClientGasStorage();
		physics.physicsSimulation().tuner = new SimulationTuner(physics);
		physics.addStepHandler(new Simulator.GasStepHandler(gas));

		OneTimeTicker.clientTicker = new OneTimeTicker();
		ClientTickEvents.START_WORLD_TICK.register(world -> OneTimeTicker.clientTicker.tickAll());

		ClientTickEvents.START_WORLD_TICK.register(ClientPhysicsStorage::onWorldTickStart);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientPhysicsStorage.onDisconnect();
            ClientGasStorage.onDisconnect();
			// Re-add gas physics step handler
            ClientPhysicsStorage.get().addStepHandler(new Simulator.GasStepHandler(ClientGasStorage.get()));
        });

		ClientCommandRegistrationCallback.EVENT.register(ClientDebugCommands::register);

		// Register model loaders
		ModelLoadingPlugin.register(new AdditionalModelLoader());

		ClientNetworking.initialize();
	}
}