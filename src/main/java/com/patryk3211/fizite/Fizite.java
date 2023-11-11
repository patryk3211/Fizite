package com.patryk3211.fizite;

import com.patryk3211.fizite.block.AllBlocks;
import com.patryk3211.fizite.blockentity.AllBlockEntities;
import com.patryk3211.fizite.item.AllItems;
import com.patryk3211.fizite.simulation.gas.GasSimulator;
import com.patryk3211.fizite.simulation.physics.PhysicsStorage;
import com.patryk3211.fizite.utility.DebugCommands;
import io.wispforest.owo.registration.reflect.FieldRegistrationHandler;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
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

		ServerWorldEvents.LOAD.register(GasSimulator::onWorldStart);
		ServerWorldEvents.LOAD.register(PhysicsStorage::onWorldStart);
		ServerTickEvents.START_WORLD_TICK.register(GasSimulator::onWorldTickStart);
		ServerTickEvents.START_WORLD_TICK.register(PhysicsStorage::onWorldTickStart);

		ServerTickEvents.START_SERVER_TICK.register(PhysicsStorage::onServerTickStart);
		ServerTickEvents.END_SERVER_TICK.register(PhysicsStorage::onServerTickEnd);

		ServerLifecycleEvents.SERVER_STARTING.register(server -> PhysicsStorage.initializeWorker());
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> PhysicsStorage.stopWorker());

		CommandRegistrationCallback.EVENT.register(DebugCommands::registerDebugCommands);
	}
}