package com.patryk3211.fizite;

import com.patryk3211.fizite.block.AllBlocks;
import com.patryk3211.fizite.blockentity.AllBlockEntities;
import com.patryk3211.fizite.capability.OneTimeTicker;
import com.patryk3211.fizite.item.AllItems;
import com.patryk3211.fizite.simulation.Simulator;
import com.patryk3211.fizite.simulation.Networking;
import com.patryk3211.fizite.simulation.physics.ServerPhysicsStorage;
import com.patryk3211.fizite.utility.DebugCommands;
import io.wispforest.owo.registration.reflect.FieldRegistrationHandler;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Fizite implements ModInitializer {
	public static final String MOD_ID = "fizite";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Starting Fizite");

		FieldRegistrationHandler.register(AllBlocks.class, MOD_ID, false);
		FieldRegistrationHandler.register(AllItems.class, MOD_ID, false);
		FieldRegistrationHandler.register(AllBlockEntities.class, MOD_ID, false);

		ServerWorldEvents.LOAD.register(Simulator::onWorldStart);

		ServerTickEvents.START_SERVER_TICK.register(Simulator::onServerTickStart);
		ServerTickEvents.START_WORLD_TICK.register(OneTimeTicker::onWorldTickStart);
		ServerTickEvents.END_SERVER_TICK.register(Simulator::onServerTickEnd);
		ServerTickEvents.END_WORLD_TICK.register(ServerPhysicsStorage::onWorldTickEnd);

		ServerLifecycleEvents.SERVER_STARTING.register(server -> Simulator.initializeWorker());
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			Simulator.stopWorker();
			Networking.clear();
		});
//
//		ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((entity, world) -> {
//            if(entity instanceof final CapabilitiesBlockEntity capEntity)
//				capEntity.onUnload();
//        });

		CommandRegistrationCallback.EVENT.register(DebugCommands::registerDebugCommands);

		Networking.initialize();
	}
}