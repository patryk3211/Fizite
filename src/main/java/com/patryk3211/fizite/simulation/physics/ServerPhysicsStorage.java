package com.patryk3211.fizite.simulation.physics;

import com.patryk3211.fizite.Fizite;
import com.patryk3211.fizite.simulation.Networking;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.Constraint;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServerPhysicsStorage extends PhysicsStorage {
    public static final String STORAGE_ID = Fizite.MOD_ID + ":physics_world";
    public static final Type<ServerPhysicsStorage> TYPE = new Type<>(ServerPhysicsStorage::new, nbt -> new ServerPhysicsStorage(), null);

    private static final Map<RegistryKey<World>, ServerPhysicsStorage> simulations = new HashMap<>();

    private RegistryKey<World> world;
    private boolean redirtify = true;

    // We need to make sure that everything stays synchronized between the server and simulation threads,
    // to achieve this we simply run functions which modify the simulation on the simulating thread.
    private final Queue<Runnable> deferredActions = new ConcurrentLinkedQueue<>();

    @Override
    public void add(BlockPos position, PhysicsCapability capability) {
        final var entry = createPositionEntry(position, capability);

        // Add all rigid bodies and internal constraints to the simulation
        deferredActions.add(() -> {
            for(final var body : capability.bodies)
                simulation.addRigidBody(body);

            if(capability.internalConstraints != null) {
                for (final var constraint : capability.internalConstraints) {
                    simulation.addConstraint(constraint);
                }
            }

            if(entry.forceGenerator != null)
                simulation.addForceGenerator(entry.forceGenerator);
            if(entry.stepHandler != null)
                simulation.addStepHandler(entry.stepHandler);
        });

        // Notify clients of a new physical entity
        Networking.physicsAdded(world, position, capability);

        // Mark entities for saving
        redirtify = true;
    }

    @Override
    public void remove(BlockPos pos) {
        // Remove the position entry
        final var entry = dataMap.remove(pos);
        if(entry == null)
            return;

        // Remove simulation elements
        deferredActions.add(() -> {
            // Remove all internal constraints and rigid bodies from the simulation
            if(entry.capability.internalConstraints != null) {
                for (final var constraint : entry.capability.internalConstraints) {
                    simulation.removeConstraint(constraint);
                }
            }
            for(final var body : entry.capability.bodies)
                simulation.removeRigidBody(body);

            // Remove other capabilities
            if(entry.stepHandler != null)
                simulation.removeStepHandler(entry.stepHandler);
            if(entry.forceGenerator != null)
                simulation.removeForceGenerator(entry.forceGenerator);
        });
    }

    @Override
    public void add(Constraint constraint) {
        deferredActions.add(() -> simulation.addConstraint(constraint));
    }

    @Override
    public void remove(Constraint constraint) {
        deferredActions.add(() -> simulation.removeConstraint(constraint));
    }

    @Override
    public void save(File file) {
        // We need to remark all entities as dirty
        redirtify = true;
    }

    public static void simulateAll() {
        simulations.forEach((key, sim) -> {
            // Run all deferred actions
            Runnable action;
            while((action = sim.deferredActions.poll()) != null)
                action.run();

            // Perform the simulation
            sim.simulation.simulate();
        });
    }

    public static ServerPhysicsStorage addToWorld(ServerWorld world) {
        final var storage = world.getPersistentStateManager().getOrCreate(TYPE, STORAGE_ID);
        storage.world = world.getRegistryKey();
        simulations.put(world.getRegistryKey(), storage);
        return storage;
    }

    public static void onWorldTickEnd(ServerWorld world) {
        // Mark all block entities as dirty
        final var storage = (ServerPhysicsStorage) get(world);
        if(storage.redirtify) {
            storage.dataMap.forEach((pos, data) -> data.capability.getEntity().markDirty());
            storage.redirtify = false;
        }
    }

    public static void syncStates(MinecraftServer server) {
        final Map<RegistryKey<World>, List<ServerPlayerEntity>> playerDimensions = new HashMap<>();
        for(ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            final var key = player.getServerWorld().getRegistryKey();
            if(playerDimensions.get(key) == null) {
                final List<ServerPlayerEntity> list = new LinkedList<>();
                list.add(player);
                playerDimensions.put(key, list);
            } else {
                playerDimensions.get(key).add(player);
            }
        }
        playerDimensions.forEach((key, players) -> {
            final var sim = simulations.get(key);
            final var packet = sim.makeSyncPacket();
            Networking.CHANNEL.serverHandle(players).send(packet);
        });
    }

    public static void clearSimulations() {
        simulations.clear();
    }

    public static void recordFrames(RegistryKey<World> simWorld, int count, Runnable finishCallback) {
        simulations.get(simWorld).simulation.addOutputWriter(count, finishCallback);
    }
}
