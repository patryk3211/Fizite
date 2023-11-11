package com.patryk3211.fizite.simulation.physics;

import com.patryk3211.fizite.Fizite;
import com.patryk3211.fizite.simulation.physics.simulation.PhysicsWorld;
import com.patryk3211.fizite.simulation.physics.simulation.RigidBody;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.Constraint;
import com.patryk3211.fizite.utility.DirectionUtilities;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.PersistentState;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Semaphore;

public class PhysicsStorage extends PersistentState {
    public static final String STORAGE_ID = Fizite.MOD_ID + ":physics_world";
    public static final Type<PhysicsStorage> TYPE = new Type<>(PhysicsStorage::new, (nbt) -> new PhysicsStorage(), null);

    private static final List<PhysicsStorage> simulations = new LinkedList<>();
    private static boolean solverRunning;
    private static Semaphore solveStart;
    private static Semaphore solveFinished;

    private static class PositionData {
        public final Constraint[] constraints;
        public final RigidBody[] bodies;
        public final Constraint[] internalConstraints;
        public final IPhysicsProvider provider;

        public PositionData(IPhysicsProvider provider, RigidBody[] bodies, Constraint[] internalConstraints) {
            this.provider = provider;
            this.bodies = bodies.clone();
            this.internalConstraints = internalConstraints != null ? internalConstraints.clone() : null;
            this.constraints = new Constraint[3];
        }
    }

    private final PhysicsWorld simulation;
    private final Map<BlockPos, PositionData> dataMap;

    public PhysicsStorage() {
        simulation = new PhysicsWorld();
        dataMap = new HashMap<>();

        simulations.add(this);
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
        if(entry.internalConstraints != null) {
            for (final var constraint : entry.internalConstraints) {
                simulation.removeConstraint(constraint);
            }
        }
        for(final var body : entry.bodies) {
            simulation.removeRigidBody(body);
        }
        dataMap.remove(position);
    }

    public IPhysicsProvider getProvider(BlockPos position) {
        final var entry = dataMap.get(position);
        return entry != null ? entry.provider : null;
    }

//    private void updateOrigin(Vector2fc delta, BlockPos position, PositionData entry, Set<BlockPos> processedPositions) {
////        entry.provider.
////        entry.
//
//        for(final var dir : Direction.values()) {
//            final var neighborPos = position.offset(dir);
//            if(processedPositions.add(neighborPos)) {
//                // FIXME: delta should be offset by offset of entry
//                updateOrigin(delta, position, entry, processedPositions);
//            }
//        }
//    }

    public void addBlockEntity(BlockEntity entity) {
        assert entity instanceof IPhysicsProvider : "Only IPhysicsProvider block entities can be added to PhysicsStorage";
        final var provider = (IPhysicsProvider) entity;

        // Add all rigid bodies and internal constraints to the simulation
        final var entry = new PositionData(provider, provider.bodies(), provider.internalConstraints());
        for(final var body : entry.bodies) {
            simulation.addRigidBody(body);
        }
        if(entry.internalConstraints != null) {
            for (final var constraint : entry.internalConstraints) {
                simulation.addConstraint(constraint);
            }
        }
        dataMap.put(entity.getPos(), entry);

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

//                if(entry.origin == null) {
//                    // Adjust origin according to neighbor
//                    entry.origin = new Vector2f();
//                    neighborEntry.origin.add(neighbor.getOffset(dir.getOpposite()), entry.origin);
//                } else {
//                    // Try to move the neighbor origin
//                    final var processed = new HashSet<BlockPos>();
//                    if(!neighborEntry.originLocked) {
//                        processed.add(entity.getPos());
//                        updateOrigin(entry.origin, neighborPos, neighborEntry, processed);
//                    } else {
//                        processed.add(neighborPos);
//                        updateOrigin(neighborEntry.origin, entity.getPos(), entry, processed);
//                    }
//                }
            }
        }

//        if(entry.origin == null) {
//            entry.origin = new Vector2f();
//        }
//
//        // Inform the provider of its origin after all calculations are finished
//        entry.provider.setOrigin(entry.origin);
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
    public static PhysicsStorage get(ServerWorld world) {
        final PhysicsStorage physicsWorld = world.getPersistentStateManager().get(TYPE, STORAGE_ID);
        assert physicsWorld != null : "This server world doesn't have a physics world attached";
        return physicsWorld;
    }

    public static void initializeWorker() {
        Fizite.LOGGER.info("Starting physics solver thread");
        solveStart = new Semaphore(0, true);
        solveFinished = new Semaphore(1, true);
        Thread solverThread = new Thread(() -> {
            Fizite.LOGGER.info("Physics solver thread started");
            while (solverRunning) {
                try {
                    solveStart.acquire();
                    simulations.forEach(sim -> sim.simulation.simulate());
                    solveFinished.release();
                } catch (InterruptedException e) {
                    Fizite.LOGGER.error(e.getMessage());
                } catch (Exception e) {
                    // Make sure we don't deadlock the main thread after an error
                    Fizite.LOGGER.error(e.getMessage());
                    solveFinished.release();
                }
            }
            Fizite.LOGGER.info("Physics solver thread stopped");
        });
        solverRunning = true;
        solverThread.setName("PhysicsSimulatorWorker");
        solverThread.start();
    }

    public static void stopWorker() {
        Fizite.LOGGER.info("Stopping physics solver thread");
        solverRunning = false;
        simulations.clear();
    }

    public static void onWorldStart(MinecraftServer minecraftServer, ServerWorld serverWorld) {
        final var storage = new PhysicsStorage();
        serverWorld.getPersistentStateManager().set(STORAGE_ID, storage);
    }

    private static int frames = 0;
    public static void onWorldTickStart(ServerWorld serverWorld) {
//        final var physicsStorage = get(serverWorld);

//        if(frames < 100) {
//            if(serverWorld.getDimensionKey().equals(DimensionTypes.OVERWORLD)) {
//                try {
//                    if(physicsStorage.writer == null)
//                        physicsStorage.addOutputWriter();
//                    physicsStorage.simulation.dumpState(physicsStorage.writer);
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//            ++frames;
//        }
    }

    public static void onServerTickStart(MinecraftServer minecraftServer) {
        // Dispatch worker thread
        solveStart.release();
    }

    public static void onServerTickEnd(MinecraftServer minecraftServer) {
        // Wait for worker to finish
        try {
            solveFinished.acquire();
        } catch (InterruptedException e) {
            Fizite.LOGGER.error(e.getMessage());
        }
    }

    public static void recordFrames(int simIndex, int count, Runnable finishCallback) {
        simulations.get(simIndex).simulation.addOutputWriter(count, finishCallback);
    }
}
