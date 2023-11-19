package com.patryk3211.fizite.simulation;

import com.patryk3211.fizite.Fizite;
import com.patryk3211.fizite.simulation.gas.GasSimulator;
import com.patryk3211.fizite.simulation.gas.GasStorage;
import com.patryk3211.fizite.simulation.physics.PhysicsStorage;
import com.patryk3211.fizite.simulation.physics.simulation.IPhysicsStepHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

import java.util.concurrent.Semaphore;

public class Simulator {
    public record GasStepHandler(GasStorage boundaries) implements IPhysicsStepHandler {
        @Override
        public void onStepEnd(double deltaTime) {
            if(deltaTime == 0)
                return;
            boundaries.simulate(deltaTime);
        }
    }

    public static final float TICK_RATE = 1.0f / 20.0f;

    private static boolean solverRunning;
    private static Semaphore solveStart;
    private static Semaphore solveFinished;
    private static int tickCount = 0;

    public static void initializeWorker() {
        Fizite.LOGGER.info("Starting simulation thread");
        solveStart = new Semaphore(0, true);
        solveFinished = new Semaphore(0, true);
        Thread solverThread = new Thread(() -> {
            Fizite.LOGGER.info("Simulation thread started");
            while (solverRunning) {
                try {
                    // Wait for start signal
                    solveStart.acquire();
                    // Simulate physics
                    PhysicsStorage.simulateAll();
                    // Signal end of simulation tick
                    solveFinished.release();
                } catch (InterruptedException e) {
                    Fizite.LOGGER.error(e.getMessage());
                } catch (Exception e) {
                    // Make sure we don't deadlock the main thread after an error
                    Fizite.LOGGER.error(e.getMessage());
                    solveFinished.release();
                }
            }
            Fizite.LOGGER.info("Simulation thread stopped");
        });
        solverRunning = true;
        solverThread.setName("Simulator Worker");
        solverThread.start();
    }

    public static void stopWorker() {
        Fizite.LOGGER.info("Stopping simulation thread");
        solverRunning = false;
        PhysicsStorage.clearSimulations();
        GasSimulator.clearSync();
        tickCount = 0;
    }

    @SuppressWarnings("unused")
    public static void onWorldStart(MinecraftServer server, ServerWorld world) {
        final var physics = PhysicsStorage.addToWorld(world);
        final var gas = GasSimulator.addToWorld(world);
        physics.addStepHandler(new GasStepHandler(gas));
    }

    @SuppressWarnings("unused")
    public static void onServerTickStart(MinecraftServer server) {
        // Dispatch worker thread
        solveStart.release();
    }

    public static void onServerTickEnd(MinecraftServer server) {
        // Wait for worker to finish
        try {
            solveFinished.acquire();
            if(tickCount++ >= 20 * 2) {
                tickCount = 0;
                PhysicsStorage.syncStates(server);
                GasSimulator.syncStates();
                Networking.cleanupLists();
            }
        } catch (InterruptedException e) {
            Fizite.LOGGER.error(e.getMessage());
        }
    }
}
