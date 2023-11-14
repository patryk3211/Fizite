package com.patryk3211.fizite;

import com.mojang.brigadier.CommandDispatcher;
import com.patryk3211.fizite.block.AllBlocks;
import com.patryk3211.fizite.blockentity.AllBlockEntities;
import com.patryk3211.fizite.item.AllItems;
import com.patryk3211.fizite.simulation.Simulator;
import com.patryk3211.fizite.simulation.gas.GasSimulator;
import com.patryk3211.fizite.simulation.physics.Networking;
import com.patryk3211.fizite.simulation.physics.PhysicsStorage;
import com.patryk3211.fizite.utility.DebugCommands;
import io.wispforest.owo.registration.reflect.FieldRegistrationHandler;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
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

		ServerTickEvents.START_SERVER_TICK.register(Simulator::onServerTickStart);
		ServerTickEvents.END_SERVER_TICK.register(Simulator::onServerTickEnd);

		ServerLifecycleEvents.SERVER_STARTING.register(server -> Simulator.initializeWorker());
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> Simulator.stopWorker());

		CommandRegistrationCallback.EVENT.register(DebugCommands::registerDebugCommands);

		Networking.initialize();
	}
}