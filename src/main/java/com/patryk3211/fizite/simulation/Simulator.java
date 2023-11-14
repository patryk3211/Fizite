package com.patryk3211.fizite.simulation;

import com.patryk3211.fizite.Fizite;
import com.patryk3211.fizite.simulation.gas.GasSimulator;
import com.patryk3211.fizite.simulation.physics.PhysicsStorage;
import net.minecraft.server.MinecraftServer;

import java.util.concurrent.Semaphore;

public class Simulator {
    public static final float TICK_RATE = 1.0f / 20.0f;

    private static boolean solverRunning;
    private static Semaphore solveStart;
    private static Semaphore solveFinished;

    public static void initializeWorker() {
        Fizite.LOGGER.info("Starting simulation thread");
        solveStart = new Semaphore(0, true);
        solveFinished = new Semaphore(1, true);
        Thread solverThread = new Thread(() -> {
            Fizite.LOGGER.info("Simulation thread started");
            while (solverRunning) {
                try {
                    // Wait for start signal
                    solveStart.acquire();
                    // Simulate physics
                    PhysicsStorage.simulateAll();
                    // Simulate gas flow
                    GasSimulator.simulateAll();
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
        GasSimulator.clearSimulations();
    }
    public static void onServerTickStart(MinecraftServer server) {
        // Dispatch worker thread
        solveStart.release();
    }

    public static void onServerTickEnd(MinecraftServer server) {
        // Wait for worker to finish
        try {
            solveFinished.acquire();
            PhysicsStorage.onSimulationEnd(server);
        } catch (InterruptedException e) {
            Fizite.LOGGER.error(e.getMessage());
        }
    }
}
