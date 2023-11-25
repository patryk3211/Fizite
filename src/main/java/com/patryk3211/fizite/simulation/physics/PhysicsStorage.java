package com.patryk3211.fizite.simulation.physics;

import com.patryk3211.fizite.Fizite;
import com.patryk3211.fizite.simulation.Networking;
import com.patryk3211.fizite.simulation.physics.simulation.IForceGenerator;
import com.patryk3211.fizite.simulation.physics.simulation.IPhysicsStepHandler;
import com.patryk3211.fizite.simulation.physics.simulation.PhysicsWorld;
import com.patryk3211.fizite.simulation.physics.simulation.RigidBody;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.Constraint;
import com.patryk3211.fizite.utility.DirectionUtilities;
import io.wispforest.owo.nbt.NbtKey;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.*;
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
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.io.*;
import java.util.*;
import java.util.List;

public class PhysicsStorage extends PersistentState {
    public static final String STORAGE_ID = Fizite.MOD_ID + ":physics_world";
    public static final Type<PhysicsStorage> TYPE = new Type<>(PhysicsStorage::new, PhysicsStorage::new, null);

    /* Nbt stuff for saving the simulation */
    private record RigidBodyData(Vector3d position, Vector3d velocity) {
        public static RigidBodyData decode(NbtCompound data) {
            final var posNbt = data.getList("p", NbtElement.DOUBLE_TYPE);
            final var position = new Vector3d(posNbt.getDouble(0), posNbt.getDouble(1), posNbt.getDouble(2));
            final var velNbt = data.getList("v", NbtElement.DOUBLE_TYPE);
            final var velocity = new Vector3d(velNbt.getDouble(0), velNbt.getDouble(1), velNbt.getDouble(2));
//            final var restNbt = data.getList("p0", NbtElement.FLOAT_TYPE);
//            final var restPosition = new Vector3f(restNbt.getFloat(0), restNbt.getFloat(1), restNbt.getFloat(2));
            return new RigidBodyData(position, velocity);
        }

        public static NbtCompound encode(RigidBodyData rigidBodyData) {
            final var data = new NbtCompound();
            final var posNbt = new NbtList();
            posNbt.add(NbtDouble.of(rigidBodyData.position.x));
            posNbt.add(NbtDouble.of(rigidBodyData.position.y));
            posNbt.add(NbtDouble.of(rigidBodyData.position.z));
            data.put("p", posNbt);
            final var velNbt = new NbtList();
            velNbt.add(NbtDouble.of(rigidBodyData.velocity.x));
            velNbt.add(NbtDouble.of(rigidBodyData.velocity.y));
            velNbt.add(NbtDouble.of(rigidBodyData.velocity.z));
            data.put("v", velNbt);
//            final var restNbt = new NbtList();
//            restNbt.add(NbtFloat.of(rigidBodyData.restPosition.x));
//            restNbt.add(NbtFloat.of(rigidBodyData.restPosition.y));
//            restNbt.add(NbtFloat.of(rigidBodyData.restPosition.z));
//            data.put("p0", restNbt);
            return data;
        }
    }
    private static final NbtKey.Type<RigidBodyData> NBT_TYPE_RIGID_BODY = NbtKey.Type.of(NbtElement.COMPOUND_TYPE, (nbt, key) -> {
        final var data = nbt.getCompound(key);
        return RigidBodyData.decode(data);
    }, (nbt, key, data) -> {
        final var compound = RigidBodyData.encode(data);
        nbt.put(key, compound);
    });
    private static final NbtKey.Type<BlockPos> NBT_TYPE_BLOCK_POS = NbtKey.Type.of(NbtElement.INT_ARRAY_TYPE, (nbtCompound, key) -> {
        final var array = nbtCompound.getIntArray(key);
        return new BlockPos(array[0], array[1], array[2]);
    }, (nbtCompound, key, pos) -> nbtCompound.putIntArray(key, new int[] { pos.getX(), pos.getY(), pos.getZ() }));
    private static final NbtKey<RigidBodyData> NBT_RIGID_BODY = new NbtKey<>("rb", NBT_TYPE_RIGID_BODY);
    private static final NbtKey<BlockPos> NBT_POSITION = new NbtKey<>("pos", NBT_TYPE_BLOCK_POS);
    private static final NbtKey<NbtList> NBT_BODIES = new NbtKey.ListKey<>("bodies", NBT_TYPE_RIGID_BODY);
    private static final NbtKey<NbtList> NBT_POSITION_DATA = new NbtKey.ListKey<>("positionData", NbtKey.Type.COMPOUND);

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
    private final Map<BlockPos, RigidBodyData[]> saveData;

    public PhysicsStorage() {
        simulation = new PhysicsWorld();
        dataMap = new HashMap<>();
        saveData = new HashMap<>();
    }

    public PhysicsStorage(NbtCompound nbt) {
        this();
        readNbt(nbt);
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
                // TODO: Make sure this doesn't get called twice for a single body cause some funky stuff might happen.
                // Constraint body index 0 is always the base entity's body, because of the way
                // that the constraint is created. See `PhysicalConnection::makeConnection`
                constraint.setBodyPosition(0);

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
        final var savedState = saveData.remove(entity.getPos());

        // Add all rigid positionData and internal constraints to the simulation
        int index = 0;
        for(final var body : entry.bodies) {
            simulation.addRigidBody(body);
            if(savedState != null) {
                // Set body state to save state for every body
                final var state = savedState[index++];
//                body.setRestPosition(state.restPosition.x, state.restPosition.y, state.restPosition.z);
                final var bodyState = body.getState();
                bodyState.position.x = state.position.x;
                bodyState.position.y = state.position.y;
                bodyState.positionA = state.position.z;
                bodyState.velocity.x = state.velocity.x;
                bodyState.velocity.y = state.velocity.y;
                bodyState.velocityA = state.velocity.z;
            }
        }
        if(entry.internalConstraints != null) {
            for (final var constraint : entry.internalConstraints) {
                simulation.addConstraint(constraint);
            }
        }
        dataMap.put(entity.getPos(), entry);

        processSides(provider, entity);

        // Notify clients of a new physical entity
        Networking.physicsAdded(entity.getWorld().getRegistryKey(), entity.getPos(), provider);
    }

    public void addStepHandler(IPhysicsStepHandler handler) {
        simulation.addStepHandler(handler);
    }

    public Networking.ClientSyncState makeSyncPacket() {
        // Since we store positionData in a sparse list, we have to skip the null entries,
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

    private RigidBodyData encodeBody(RigidBody body) {
        final var state = body.getState();
        return new RigidBodyData(
                new Vector3d(state.position.x, state.position.y, state.positionA),
                new Vector3d(state.velocity.x, state.velocity.y, state.velocityA)
//                new Vector3f(body.getRestPosition().x, body.getRestPosition().y, body.getRestAngle())
        );
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        final var positionData = new NbtList();
        dataMap.forEach((pos, data) -> {
            final var entry = new NbtCompound();
            entry.put(NBT_POSITION, pos);
            if(data.bodies.length == 1) {
                // Encode a single body
                entry.put(NBT_RIGID_BODY, encodeBody(data.bodies[0]));
            } else {
                // Encode a body list
                final var bodies = new NbtList();
                for(final var body : data.bodies)
                    bodies.add(RigidBodyData.encode(encodeBody(body)));
                entry.put(NBT_BODIES, bodies);
            }
            positionData.add(entry);
        });
        nbt.put(NBT_POSITION_DATA, positionData);
        return nbt;
    }

    public void readNbt(NbtCompound nbt) {
        final var positionData = nbt.get(NBT_POSITION_DATA);
        positionData.forEach(element -> {
            if(!(element instanceof final NbtCompound entry))
                return;
            final var pos = entry.get(NBT_POSITION);
            RigidBodyData[] data;
            if(entry.has(NBT_RIGID_BODY)) {
                // This entry has a single body
                data = new RigidBodyData[] { entry.get(NBT_RIGID_BODY) };
            } else {
                // This entry has multiple bodies
                NbtList bodies = entry.get(NBT_BODIES);
                data = new RigidBodyData[bodies.size()];
                for(int i = 0; i < bodies.size(); ++i)
                    data[i] = RigidBodyData.decode(bodies.getCompound(i));
            }
            saveData.put(pos, data);
        });
    }

    @Override
    public void save(File file) {
        // Since even the smallest change need to be saved we just make
        // this storage class constantly dirty.
        markDirty();
        super.save(file);
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
        simulations.forEach((key, sim) -> sim.simulation.simulate());
    }

    public static PhysicsStorage addToWorld(ServerWorld world) {
        final var storage = world.getPersistentStateManager().getOrCreate(TYPE, STORAGE_ID);
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
    }

    public static void clearSimulations() {
        simulations.clear();
    }

    public static void recordFrames(RegistryKey<World> simWorld, int count, Runnable finishCallback) {
        simulations.get(simWorld).simulation.addOutputWriter(count, finishCallback);
    }
}
