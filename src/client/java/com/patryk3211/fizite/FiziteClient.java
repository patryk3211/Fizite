package com.patryk3211.fizite;

import com.patryk3211.fizite.blockentity.AllBlockEntities;
import com.patryk3211.fizite.renderer.ConnectingRodRenderer;
import com.patryk3211.fizite.renderer.CrankShaftRenderer;
import com.patryk3211.fizite.renderer.CylinderRenderer;
import com.patryk3211.fizite.renderer.HandCrankRenderer;
import com.patryk3211.fizite.simulation.ClientPhysicsNetworking;
import com.patryk3211.fizite.simulation.ClientPhysicsStorage;
import com.patryk3211.fizite.simulation.physics.Networking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.server.command.CommandManager;
import net.minecraft.world.WorldEvents;

public class FiziteClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BlockEntityRendererFactories.register(AllBlockEntities.CYLINDER_ENTITY, CylinderRenderer::new);
		BlockEntityRendererFactories.register(AllBlockEntities.CONNECTING_ROD_ENTITY, ConnectingRodRenderer::new);
		BlockEntityRendererFactories.register(AllBlockEntities.CRANK_SHAFT_ENTITY, CrankShaftRenderer::new);
		BlockEntityRendererFactories.register(AllBlockEntities.HAND_CRANK_ENTITY, HandCrankRenderer::new);

		ClientTickEvents.START_WORLD_TICK.register(ClientPhysicsStorage::onWorldTickStart);
		ClientPlayConnectionEvents.DISCONNECT.register(ClientPhysicsStorage::onDisconnect);
		ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register(ClientPhysicsStorage::onBlockEntityUnload);

		ClientCommandRegistrationCallback.EVENT.register(ClientDebugCommands::register);

		ClientPhysicsNetworking.initialize();
	}
}