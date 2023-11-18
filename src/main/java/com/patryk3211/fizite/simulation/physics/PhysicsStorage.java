package com.patryk3211.fizite.simulation.physics;

import com.patryk3211.fizite.Fizite;
import com.patryk3211.fizite.simulation.Networking;
import com.patryk3211.fizite.simulation.physics.simulation.IForceGenerator;
import com.patryk3211.fizite.simulation.physics.simulation.IPhysicsStepHandler;
import com.patryk3211.fizite.simulation.physics.simulation.PhysicsWorld;
import com.patryk3211.fizite.simulation.physics.simulation.RigidBody;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.Constraint;
import com.patryk3211.fizite.utility.DirectionUtilities;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PhysicsStorage extends PersistentState {
    public static final String STORAGE_ID = Fizite.MOD_ID + ":physics_world";
    public static final Type<PhysicsStorage> TYPE = new Type<>(PhysicsStorage::new, (nbt) -> new PhysicsStorage(), null);

    private static final Map<RegistryKey<World>, PhysicsStorage> simulations = new HashMap<>();

    protected static PhysicsStorage clientStorage;

    protected static class PositionData {
        public final Constraint[] constraints;
        public final RigidBody[] bodies;
        public final Constraint[] internalConstraints;
        public final IPhysicsProvider provider;

        public IPhysicsStepHandler stepHandler;
        public IForceGenerator forceGenerator;

        public PositionData(IPhysicsProvider provider, RigidBody[] bodies, Constraint[] internalConstraints) {
            this.provider = provider;
            this.bodies = bodies.clone();
            this.internalConstraints = internalConstraints != null ? internalConstraints.clone() : null;
            this.constraints = new Constraint[3];
        }
    }

    protected final PhysicsWorld simulation;
    protected final Map<BlockPos, PositionData> dataMap;
    private ServerWorld world;

    public PhysicsStorage() {
        simulation = new PhysicsWorld();
        dataMap = new HashMap<>();
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
        }
        if(entry.stepHandler != null)
            simulation.removeStepHandler(entry.stepHandler);
        if(entry.forceGenerator != null)
            simulation.removeForceGenerator(entry.forceGenerator);
        dataMap.remove(position);
    }

    public IPhysicsProvider getProvider(BlockPos position) {
        final var entry = dataMap.get(position);
        return entry != null ? entry.provider : null;
    }

    protected void processSides(IPhysicsProvider provider, BlockEntity entity) {
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

    protected PositionData createEntry(BlockEntity entity, IPhysicsProvider provider) {
        final var entry = new PositionData(provider, provider.bodies(), provider.internalConstraints());
        if(entity instanceof final IPhysicsStepHandler handler) {
            simulation.addStepHandler(handler);
            entry.stepHandler = handler;
        }
        if(entity instanceof final IForceGenerator generator) {
            simulation.addForceGenerator(generator);
            entry.forceGenerator = generator;
        }
        return entry;
    }

    public void addBlockEntity(BlockEntity entity) {
        assert entity instanceof IPhysicsProvider : "Only IPhysicsProvider block entities can be added to PhysicsStorage";
        final var provider = (IPhysicsProvider) entity;

        final var entry = createEntry(entity, provider);

        // Add all rigid bodies and internal constraints to the simulation
        for(final var body : entry.bodies) {
            simulation.addRigidBody(body);
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

    public void addStepHandler(IPhysicsStepHandler handler) {
        simulation.addStepHandler(handler);
    }

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

    private static Style chooseStyle(double time) {
        if(time < 10000) {
            return Style.EMPTY.withColor(Formatting.GREEN);
        } else if(time < 20000) {
            return Style.EMPTY.withColor(Formatting.YELLOW);
        } else {
            return Style.EMPTY.withColor(Formatting.RED);
        }
    }

    private static Style chooseStyleMs(double time) {
        if(time < 40) {
            return Style.EMPTY.withColor(Formatting.GREEN);
        } else if(time < 50) {
            return Style.EMPTY.withColor(Formatting.YELLOW);
        } else {
            return Style.EMPTY.withColor(Formatting.RED);
        }
    }

    private static Text writeTime(String name, long[] frames) {
        double avg = 0, min = frames[0] / 1000.0, max = frames[0] / 1000.0;
        for (long frame : frames) {
            final var value = frame / 1000.0;
            avg += value;
            if (value < min)
                min = value;
            if (value > max)
                max = value;
        }
        avg /= frames.length;

        final var result = Text.empty();
        result.append("[Fizite]   " + name + ": ");
        result.append(Text.literal(String.format("%.2f", avg)).setStyle(chooseStyle(avg)));
        result.append("/");
        result.append(Text.literal(String.format("%.2f", min)).setStyle(chooseStyle(min)));
        result.append("/");
        result.append(Text.literal(String.format("%.2f", max)).setStyle(chooseStyle(max)));
        result.append(" µs\n");
        return result;
    }

    public Text timingReport() {
        final var result = Text.empty().setStyle(Style.EMPTY.withColor(Formatting.GRAY));
        result.append("[Fizite] Simulation times (1 step, Avg/Min/Max):\n");
        result.append(writeTime("forceApply", simulation.forceGeneratorTime));
        result.append(writeTime("constraint", simulation.constraintSolveTime));
        result.append(writeTime("physicsStep", simulation.physicsStepTime));
        result.append(writeTime("physicsSolve", simulation.physicsSolveTime));
        result.append(writeTime("stepHandlers", simulation.stepHandlersTime));
        result.append("[Fizite] Simulation times (singular, " + simulation.stepCount() + " steps)\n");
        result.append("[Fizite]   Start time = ");
        final var startTime = simulation.startTime / 1000.0;
        result.append(Text.literal(String.format("%.2f", startTime)).setStyle(chooseStyle(startTime)));
        result.append(" µs\n");
        final var totalTime = simulation.totalTime / 1000000.0;
        result.append("[Fizite]   Total tick time = ");
        result.append(Text.literal(String.format("%.2f", totalTime)).setStyle(chooseStyleMs(totalTime)));
        result.append(" ms\n");
        return result;
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
            sim.simulation.simulate();
        });
    }

    public static PhysicsStorage addToWorld(ServerWorld world) {
        final var storage = new PhysicsStorage();
        world.getPersistentStateManager().set(STORAGE_ID, storage);
        storage.world = world;
        simulations.put(world.getRegistryKey(), storage);
        return storage;
    }

    public static void onWorldTickEnd(ServerWorld world) {
        get(world).dataMap.forEach((pos, data) -> {
            if(data.provider instanceof final BlockEntity blockEntity) {
                // Mark all block entities as dirty
                blockEntity.markDirty();
            }
        });
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
        Networking.cleanupList();
    }

    public static void clearSimulations() {
        simulations.clear();
    }

//    public static void onPlayerChangeWorld(ServerPlayerEntity player, ServerWorld oldWorld, ServerWorld newWorld) {
//        final var key = newWorld.getRegistryKey();
//        final var sim = simulations.get(key);
//        if(sim == null) {
//            Fizite.LOGGER.error("World " + key + " doesn't have a physics simulation");
//            return;
//        }
//
//        // Send current simulation state to the new player
//        Fizite.LOGGER.info("Sending simulation (" + key + ") state to player '" + player.getName() + "'");
//        Networking.CHANNEL.serverHandle(player).send(sim.getSimulationPacket(), sim.makeSyncPacket());
//    }

    public static void recordFrames(RegistryKey<World> simWorld, int count, Runnable finishCallback) {
        simulations.get(simWorld).simulation.addOutputWriter(count, finishCallback);
    }
}
