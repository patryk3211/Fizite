package com.patryk3211.fizite.simulation.physics;

import com.patryk3211.fizite.Fizite;
import com.patryk3211.fizite.simulation.physics.simulation.IPhysicsStepHandler;
import com.patryk3211.fizite.simulation.physics.simulation.PhysicsWorld;
import com.patryk3211.fizite.simulation.physics.simulation.RigidBody;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.Constraint;
import com.patryk3211.fizite.utility.DirectionUtilities;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.callback.CallbackHandler;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class PhysicsStorage extends PersistentState {
    public static final String STORAGE_ID = Fizite.MOD_ID + ":physics_world";
    public static final Type<PhysicsStorage> TYPE = new Type<>(PhysicsStorage::new, (nbt) -> new PhysicsStorage(), null);

    private static final Map<RegistryKey<World>, PhysicsStorage> simulations = new HashMap<>();
    private static int frameCounter = 0;

    protected static PhysicsStorage clientStorage;

    protected static class PositionData {
        public final Constraint[] constraints;
        public final RigidBody[] bodies;
        public final Constraint[] internalConstraints;
        public final IPhysicsProvider provider;
        public IPhysicsStepHandler ticker;

        public PositionData(IPhysicsProvider provider, RigidBody[] bodies, Constraint[] internalConstraints) {
            this.provider = provider;
            this.bodies = bodies.clone();
            this.internalConstraints = internalConstraints != null ? internalConstraints.clone() : null;
            this.constraints = new Constraint[3];
        }
    }

    protected final PhysicsWorld simulation;
    protected final Map<BlockPos, PositionData> dataMap;
//    protected final List<RigidBody> forceResetBodies;
    private final List<IPhysicsTicker> tickable;
    private ServerWorld world;

    public PhysicsStorage() {
        simulation = new PhysicsWorld();
        dataMap = new HashMap<>();
//        forceResetBodies = new LinkedList<>();
        tickable = new LinkedList<>();
    }

    public void addConstraint(Constraint constraint, BlockPos position, Direction direction) {
        if(direction.getDirection() == Direction.AxisDirection.NEGATIVE) {
            // Add the negative offset to get the actual position of the boundary
            position = position.add(direction.getVector());
            // Get the opposite (positive) direction
            direction = direction.getOpposite();
        }

        final var entry = dataMap.get(position);
        if(entry == null) {
            throw new IllegalStateException("Trying to put a constraint for a body which doesn't exist");
        }

        entry.constraints[DirectionUtilities.positiveDirectionIndex(direction)] = constraint;
        simulation.addConstraint(constraint);
    }

    public void removeConstraint(BlockPos position, Direction direction) {
        if(direction.getDirection() == Direction.AxisDirection.NEGATIVE) {
            // Add the negative offset to get the actual position of the boundary
            position = position.add(direction.getVector());
            // Get the opposite (positive) direction
            direction = direction.getOpposite();
        }

        final var entry = dataMap.get(position);
        if(entry == null)
            return;

        final var constraint = entry.constraints[DirectionUtilities.positiveDirectionIndex(direction)];
        entry.constraints[DirectionUtilities.positiveDirectionIndex(direction)] = null;
        simulation.removeConstraint(constraint);
    }

    public void clearPosition(BlockPos position) {
        for(final var dir : Direction.values()) {
            removeConstraint(position, dir);
        }

        final var entry = dataMap.get(position);
        if(entry == null)
            return;
        if(entry.internalConstraints != null) {
            for (final var constraint : entry.internalConstraints) {
                simulation.removeConstraint(constraint);
            }
        }
        for(final var body : entry.bodies) {
            simulation.removeRigidBody(body);
//            forceResetBodies.remove(body);
        }
        if(entry.ticker != null)
            simulation.removeStepHandler(entry.ticker);
//            tickable.remove(entry.ticker);
        dataMap.remove(position);
    }

    public IPhysicsProvider getProvider(BlockPos position) {
        final var entry = dataMap.get(position);
        return entry != null ? entry.provider : null;
    }

    private void processSides(IPhysicsProvider provider, BlockEntity entity) {
        // Process all directions and add constraints (if the neighbor provider exists)
        for(final var dir : Direction.values()) {
            if(provider.getConnectionType(dir) == PhysicalConnection.ConnectionType.NONE)
                continue;
            final BlockPos neighborPos = entity.getPos().offset(dir);
            final var neighborEntry = dataMap.get(neighborPos);
            if(neighborEntry == null)
                continue;
            final IPhysicsProvider neighbor = neighborEntry.provider;
            final var constraint = PhysicalConnection.makeConnection(dir, provider, neighbor);
            if(constraint != null) {
                // Can connect to this provider
                addConstraint(constraint, entity.getPos(), dir);

                provider.setExternalConstraint(dir, constraint);
                neighbor.setExternalConstraint(dir.getOpposite(), constraint);
            }
        }
    }

    public void addBlockEntity(BlockEntity entity) {
        assert entity instanceof IPhysicsProvider : "Only IPhysicsProvider block entities can be added to PhysicsStorage";
        final var provider = (IPhysicsProvider) entity;

        // Add all rigid bodies and internal constraints to the simulation
        final var entry = new PositionData(provider, provider.bodies(), provider.internalConstraints());
        if(entity instanceof IPhysicsStepHandler) {
            simulation.addStepHandler((IPhysicsStepHandler) entity);
            entry.ticker = (IPhysicsStepHandler) entity;
//            tickable.add((IPhysicsTicker) entity);
//            entry.ticker = (IPhysicsTicker) entity;
        }
        for(final var body : entry.bodies) {
            simulation.addRigidBody(body);
//            if(provider.externalForceReset())
//                addToForceReset(body);
        }
        if(entry.internalConstraints != null) {
            for (final var constraint : entry.internalConstraints) {
                simulation.addConstraint(constraint);
            }
        }
        dataMap.put(entity.getPos(), entry);

        processSides(provider, entity);

        // Notify clients of a new physical entity
        Networking.entityAdded(entity.getPos(), provider);
    }

    @Environment(EnvType.CLIENT)
    public void addBlockEntity(BlockEntity entity, int[] bodyIndices) {
        assert entity instanceof IPhysicsProvider : "Only IPhysicsProvider block entities can be added to PhysicsStorage";
        final var provider = (IPhysicsProvider) entity;

        // Add all rigid bodies and internal constraints to the simulation
        final var entry = new PositionData(provider, provider.bodies(), provider.internalConstraints());
        if(entity instanceof IPhysicsStepHandler) {
            simulation.addStepHandler((IPhysicsStepHandler) entity);
            entry.ticker = (IPhysicsStepHandler) entity;
//            tickable.add((IPhysicsTicker) entity);
//            entry.ticker = (IPhysicsTicker) entity;
        }
        int i = 0;
        for(final var body : entry.bodies) {
            simulation.addRigidBody(body, bodyIndices[i++]);
//            if(provider.externalForceReset())
//                addToForceReset(body);
        }
        if(entry.internalConstraints != null) {
            for (final var constraint : entry.internalConstraints) {
                simulation.addConstraint(constraint);
            }
        }
        dataMap.put(entity.getPos(), entry);

        processSides(provider, entity);
    }
//
//    public void addToForceReset(RigidBody body) {
//        forceResetBodies.add(body);
//    }

    public Networking.ClientSyncState makeSyncPacket() {
        // Since we store bodies in a sparse list, we have to skip the null entries,
        // this reduces the effective size of our sync packet since we don't have
        // to encode and send them.
        final var bodies = simulation.bodies();
        final var length = simulation.bodyCount();
        int[] indices = new int[length];
        Vec2f[] positions = new Vec2f[length];
        Vec2f[] velocities = new Vec2f[length];
        float[] angles = new float[length];
        float[] angularVelocities = new float[length];

        int entryIndex = 0;
        for (final RigidBody body : bodies) {
            if (body == null)
                continue;
            indices[entryIndex] = body.index();
            final var state = body.getState();
            positions[entryIndex] = new Vec2f((float) state.position.x, (float) state.position.y);
            velocities[entryIndex] = new Vec2f((float) state.velocity.x, (float) state.velocity.y);
            angles[entryIndex] = (float) state.positionA;
            angularVelocities[entryIndex] = (float) state.velocityA;
            ++entryIndex;
        }

        return new Networking.ClientSyncState(indices, positions, velocities, angles, angularVelocities);
    }

    public Networking.ClientGetSimulation getSimulationPacket() {
        BlockPos[] positions = new BlockPos[dataMap.size()];
        int[][] rigidBodies = new int[dataMap.size()][];

        AtomicInteger index = new AtomicInteger();
        dataMap.forEach((pos, data) -> {
            positions[index.get()] = pos;
            rigidBodies[index.get()] = new int[data.bodies.length];

            final var rbIndices = rigidBodies[index.getAndIncrement()];
            for(int i = 0; i < data.bodies.length; ++i) {
                rbIndices[i] = data.bodies[i].index();
            }
        });

        return new Networking.ClientGetSimulation(positions, rigidBodies);
    }

    public String timingReport() {
        return "Simulation times (1 step):\n" +
                "  constraint = " + (simulation.constraintSolveTime / 1000.0) + "us\n" +
                "  physicsStep = " + (simulation.physicsStepTime / 1000.0) + "us\n" +
                "  physicsSolve = " + (simulation.physicsSolveTime / 1000.0) + "us\n" +
                "Start time = " + (simulation.startTime / 1000.0) + "us\n" +
                "Total simulation time = " + (simulation.totalTime / 1000.0) + "us";
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        return null;
    }

    @Override
    public void save(File file) {
        // Do nothing...
    }

    @NotNull
    public static PhysicsStorage get(World world) {
        if(!world.isClient) {
            final PhysicsStorage physicsWorld = ((ServerWorld) world).getPersistentStateManager().get(TYPE, STORAGE_ID);
            assert physicsWorld != null : "This server world doesn't have a physics world attached";
            return physicsWorld;
        } else {
            return clientStorage;
        }
    }

    public static void simulateAll() {
        simulations.forEach((key, sim) -> {
            for (IPhysicsTicker ticker : sim.tickable)
                ticker.tick();
            sim.simulation.simulate();
//            for (RigidBody forceResetBody : sim.forceResetBodies) {
//                final var state = forceResetBody.getState();
//                state.extForce.x = 0;
//                state.extForce.y = 0;
//                state.extForceA = 0;
//            }
        });
    }

//    public static void initializeWorker() {
//        Fizite.LOGGER.info("Starting physics solver thread");
//        solveStart = new Semaphore(0, true);
//        solveFinished = new Semaphore(1, true);
//        Thread solverThread = new Thread(() -> {
//            Fizite.LOGGER.info("Physics solver thread started");
//            while (solverRunning) {
//                try {
//                    solveStart.acquire();
//                    simulations.forEach((key, sim) -> {
//                        sim.simulation.simulate();
//                        for (RigidBody forceResetBody : sim.forceResetBodies) {
//                            final var state = forceResetBody.getState();
//                            state.extForce.x = 0;
//                            state.extForce.y = 0;
//                            state.extForceA = 0;
//                        }
//                    });
//                    solveFinished.release();
//                } catch (InterruptedException e) {
//                    Fizite.LOGGER.error(e.getMessage());
//                } catch (Exception e) {
//                    // Make sure we don't deadlock the main thread after an error
//                    Fizite.LOGGER.error(e.getMessage());
//                    solveFinished.release();
//                }
//            }
//            Fizite.LOGGER.info("Physics solver thread stopped");
//        });
//        solverRunning = true;
//        solverThread.setName("PhysicsSimulatorWorker");
//        solverThread.start();
//    }
//
//    public static void stopWorker() {
//        Fizite.LOGGER.info("Stopping physics solver thread");
//        solverRunning = false;
//        simulations.clear();
//    }

    public static void onWorldStart(MinecraftServer minecraftServer, ServerWorld serverWorld) {
        final var storage = new PhysicsStorage();
        serverWorld.getPersistentStateManager().set(STORAGE_ID, storage);
        storage.world = serverWorld;
        simulations.put(serverWorld.getRegistryKey(), storage);
    }

//    public static void onWorldTickStart(ServerWorld serverWorld) {
////        final var physicsStorage = get(serverWorld);
//    }

    public static void onSimulationEnd(MinecraftServer server) {
        if(++frameCounter >= 20 * 2) {
            frameCounter = 0;
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
            Networking.cleanupList();
        }
    }

    public static void clearSimulations() {
        simulations.clear();
    }
//    public static void onServerTickStart(MinecraftServer server) {
//        // Dispatch worker thread
//        solveStart.release();
//    }
//
//    public static void onServerTickEnd(MinecraftServer server) {
//        // Wait for worker to finish
//        try {
//            solveFinished.acquire();
//            if(++frameCounter >= 20 * 2) {
//                frameCounter = 0;
//                final Map<RegistryKey<World>, List<ServerPlayerEntity>> playerDimensions = new HashMap<>();
//                for(ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
//                    final var key = player.getServerWorld().getRegistryKey();
//                    if(playerDimensions.get(key) == null) {
//                        final List<ServerPlayerEntity> list = new LinkedList<>();
//                        list.add(player);
//                        playerDimensions.put(key, list);
//                    } else {
//                        playerDimensions.get(key).add(player);
//                    }
//                }
//                playerDimensions.forEach((key, players) -> {
//                    final var sim = simulations.get(key);
//                    final var packet = sim.makeSyncPacket();
//                    Networking.CHANNEL.serverHandle(players).send(packet);
//                });
//                Networking.cleanupList();
//            }
//        } catch (InterruptedException e) {
//            Fizite.LOGGER.error(e.getMessage());
//        }
//    }

    public static void onPlayerChangeWorld(ServerPlayerEntity player, ServerWorld oldWorld, ServerWorld newWorld) {
        final var key = newWorld.getRegistryKey();
        final var sim = simulations.get(key);
        if(sim == null) {
            Fizite.LOGGER.error("World " + key + " doesn't have a physics simulation");
            return;
        }

        // Send current simulation state to the new player
        Fizite.LOGGER.info("Sending simulation (" + key + ") state to player '" + player.getName() + "'");
        Networking.CHANNEL.serverHandle(player).send(sim.getSimulationPacket(), sim.makeSyncPacket());
    }

    public static void recordFrames(RegistryKey<World> simWorld, int count, Runnable finishCallback) {
        simulations.get(simWorld).simulation.addOutputWriter(count, finishCallback);
    }
}
