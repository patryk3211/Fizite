package com.patryk3211.fizite;

import com.patryk3211.fizite.block.AllBlocks;
import com.patryk3211.fizite.blockentity.AllBlockEntities;
import com.patryk3211.fizite.simulation.gas.GasSimulator;
import com.patryk3211.fizite.simulation.physics.PhysicsStorage;
import io.wispforest.owo.registration.reflect.FieldRegistrationHandler;
import net.fabricmc.api.ModInitializer;

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
		FieldRegistrationHandler.register(AllBlockEntities.class, MOD_ID, false);

		ServerWorldEvents.LOAD.register(GasSimulator::onWorldStart);
		ServerWorldEvents.LOAD.register(PhysicsStorage::onWorldStart);
		ServerTickEvents.START_WORLD_TICK.register(GasSimulator::onWorldTickStart);
		ServerTickEvents.START_WORLD_TICK.register(PhysicsStorage::onWorldTickStart);
	}
}