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
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;

import java.awt.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class PhysicsStorage extends PersistentState {
    public static final String STORAGE_ID = Fizite.MOD_ID + ":physics_world";
    public static final Type<PhysicsStorage> TYPE = new Type<>(PhysicsStorage::new, (nbt) -> new PhysicsStorage(), null);

    private static class PositionData {
        public final Constraint[] constraints;
        public final RigidBody[] bodies;
        public final Constraint[] internalConstraints;
        public final IPhysicsProvider provider;
        public Vector2f origin;

        public PositionData(IPhysicsProvider provider, RigidBody[] bodies, Constraint[] internalConstraints) {
            this.provider = provider;
            this.bodies = bodies.clone();
            this.internalConstraints = internalConstraints != null ? internalConstraints.clone() : null;
            this.constraints = new Constraint[3];
            this.origin = null;
        }
    }

    private final PhysicsWorld simulation;
    private final Map<BlockPos, PositionData> dataMap;

    private OutputStreamWriter writer;

    public PhysicsStorage() {
        simulation = new PhysicsWorld();
        dataMap = new HashMap<>();
    }

    private void addOutputWriter() throws FileNotFoundException {
        var fileStream = new FileOutputStream("output.phys");
        writer = new OutputStreamWriter(fileStream);
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
//            if(neighbor == null)
//                continue;
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
                    // Try to move the neighbor origin
//                    entry.origin
//                }
            }
        }
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

    public static void onWorldStart(MinecraftServer minecraftServer, ServerWorld serverWorld) {
        final var storage = new PhysicsStorage();
        serverWorld.getPersistentStateManager().set(STORAGE_ID, storage);
        if(serverWorld.getDimensionKey().equals(DimensionTypes.OVERWORLD)) {
            try {
                storage.addOutputWriter();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static int frames = 0;
    public static void onWorldTickStart(ServerWorld serverWorld) {
        final var physicsStorage = get(serverWorld);
        physicsStorage.simulation.simulate();
        if(frames < 100) {
            if(serverWorld.getDimensionKey().equals(DimensionTypes.OVERWORLD)) {
                try {
                    physicsStorage.simulation.dumpState(physicsStorage.writer);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            ++frames;
        }
    }
}
