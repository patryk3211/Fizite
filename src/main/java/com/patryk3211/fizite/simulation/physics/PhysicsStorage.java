package com.patryk3211.fizite.simulation.physics;

import com.patryk3211.fizite.simulation.Networking;
import com.patryk3211.fizite.simulation.Simulator;
import com.patryk3211.fizite.simulation.physics.simulation.IForceGenerator;
import com.patryk3211.fizite.simulation.physics.simulation.IPhysicsStepHandler;
import com.patryk3211.fizite.simulation.physics.simulation.PhysicsWorld;
import com.patryk3211.fizite.simulation.physics.simulation.RigidBody;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.Constraint;
import com.patryk3211.fizite.utility.DirectionUtilities;
import net.minecraft.nbt.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class PhysicsStorage extends PersistentState {
    protected static PhysicsStorage clientStorage;

    protected static class PositionData {
        public final Constraint[] constraints;
//        public final RigidBody[] bodies;
//        public final Constraint[] internalConstraints;
        public final PhysicsCapability capability;

        public IPhysicsStepHandler stepHandler;
        public IForceGenerator forceGenerator;

        public PositionData(PhysicsCapability capability/*, RigidBody[] bodies, Constraint[] internalConstraints*/) {
            this.capability = capability;
//            this.bodies = bodies.clone();
//            this.internalConstraints = internalConstraints != null ? internalConstraints.clone() : null;
            this.constraints = new Constraint[3];
        }
    }

    protected final PhysicsWorld simulation;
    protected final Map<BlockPos, PositionData> dataMap;

    public PhysicsStorage() {
        simulation = new PhysicsWorld(Simulator.TICK_RATE, 100);
        dataMap = new HashMap<>();
    }

    public PhysicsWorld physicsSimulation() {
        return simulation;
    }

    public void add(Constraint constraint, BlockPos position, Direction direction) {
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

    public void remove(BlockPos position, Direction direction) {
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

    public void add(Constraint constraint) {
        simulation.addConstraint(constraint);
    }

    public void remove(Constraint constraint) {
        simulation.removeConstraint(constraint);
    }

    public PhysicsCapability getCapability(BlockPos pos) {
        final var entry = dataMap.get(pos);
        return entry == null ? null : entry.capability;
    }

//    public void clearPosition(BlockPos position) {
//        for(final var dir : Direction.values()) {
//            removeConstraint(position, dir);
//        }
//
//        final var entry = dataMap.get(position);
//        if(entry == null)
//            return;
//        if(entry.internalConstraints != null) {
//            for (final var constraint : entry.internalConstraints) {
//                simulation.removeConstraint(constraint);
//            }
//        }
//        for(final var body : entry.bodies) {
//            simulation.removeRigidBody(body);
//        }
//        if(entry.stepHandler != null)
//            simulation.removeStepHandler(entry.stepHandler);
//        if(entry.forceGenerator != null)
//            simulation.removeForceGenerator(entry.forceGenerator);
//        dataMap.remove(position);
//    }

//    public IPhysicsProvider getProvider(BlockPos position) {
//        final var entry = dataMap.get(position);
//        return entry != null ? entry.provider : null;
//    }

//    protected void processSides(IPhysicsProvider provider, BlockEntity entity) {
//        // Process all directions and add constraints (if the neighbor provider exists)
//        for(final var dir : Direction.values()) {
//            if(provider.getConnectionType(dir) == PhysicalConnection.ConnectionType.NONE)
//                continue;
//            final BlockPos neighborPos = entity.getPos().offset(dir);
//            final var neighborEntry = dataMap.get(neighborPos);
//            if(neighborEntry == null)
//                continue;
//            final IPhysicsProvider neighbor = neighborEntry.provider;
//            final var constraint = PhysicalConnection.makeConnection(dir, provider, neighbor);
//            if(constraint != null) {
//                // Can connect to this provider
//                addConstraint(constraint, entity.getPos(), dir);
//                // TODO: Make sure this doesn't get called twice for a single body cause some funky stuff might happen.
//                // Constraint body index 0 is always the base entity's body, because of the way
//                // that the constraint is created. See `PhysicalConnection::makeConnection`
//                constraint.setBodyPosition(0);
//
//                provider.setExternalConstraint(dir, constraint);
//                neighbor.setExternalConstraint(dir.getOpposite(), constraint);
//            }
//        }
//    }

//    protected PositionData createEntry(BlockEntity entity, IPhysicsProvider provider) {
//        final var entry = new PositionData(provider, provider.bodies(), provider.internalConstraints());
//        if(entity instanceof final IPhysicsStepHandler handler) {
//            simulation.addStepHandler(handler);
//            entry.stepHandler = handler;
//        }
//        if(entity instanceof final IForceGenerator generator) {
//            simulation.addForceGenerator(generator);
//            entry.forceGenerator = generator;
//        }
//        return entry;
//    }

    protected void createPositionEntry(BlockPos position, PhysicsCapability capability) {
        // Create a position entry for the capability
        final var data = new PositionData(capability);
        dataMap.put(position, data);

        // Check for other physics related capabilities
        final var entity = capability.getEntity();
        final var forceCap = entity.getCapability(ForceGeneratorCapability.class);
        if(forceCap != null) {
            simulation.addForceGenerator(forceCap);
            data.forceGenerator = forceCap;
        }
        final var stepCap = entity.getCapability(StepHandlerCapability.class);
        if(stepCap != null) {
            simulation.addStepHandler(stepCap);
            data.stepHandler = stepCap;
        }
    }

    public void add(BlockPos position, PhysicsCapability capability) {
        // Create a position entry for the capability
        createPositionEntry(position, capability);
    }

    public void remove(BlockPos pos) {
        // Get the position data
        final var entry = dataMap.get(pos);
        if(entry == null)
            return;

        // Remove all external constraints attached to this position
        for(final var dir : Direction.values())
            remove(pos, dir);

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

        // Remove the position entry
        dataMap.remove(pos);
    }
    /* {
        // Add all rigid bodies and internal constraints to the simulation
//        for(final var body : capability.bodies)
//            simulation.addRigidBody(body);
//        if(capability.internalConstraints != null) {
//            for (final var constraint : capability.internalConstraints) {
//                simulation.addConstraint(constraint);
//            }
//        }
//
//        sidedAdd(position, capability);
    }*/
//    public void addBlockEntity(BlockEntity entity) {
//        assert entity instanceof IPhysicsProvider : "Only IPhysicsProvider block entities can be added to PhysicsStorage";
//        final var provider = (IPhysicsProvider) entity;
//
//        final var entry = createEntry(entity, provider);
//        final var savedState = saveData.remove(entity.getPos());
//
//        int index = 0;
//        for(final var body : entry.bodies) {
//            simulation.addRigidBody(body);
//            if(savedState != null) {
//                // Set body state to save state for every body
//                final var state = savedState[index++];
//                final var bodyState = body.getState();
//            }
//        }
//        if(entry.internalConstraints != null) {
//            for (final var constraint : entry.internalConstraints) {
//                simulation.addConstraint(constraint);
//            }
//        }
//        dataMap.put(entity.getPos(), entry);
//
//        processSides(provider, entity);
//
//    }

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

    private static Text writeTime(String name, int[] frames) {
        int sum = 0, min = frames[0], max = frames[0];
        for (int frame : frames) {
            sum += frame;
            if (frame < min)
                min = frame;
            if (frame > max)
                max = frame;
        }
        float avg = (float) sum / frames.length;

        final var result = Text.empty();
        result.append("[Fizite]   " + name + ": ");
        result.append(Text.literal(String.format("%.2f", avg)).setStyle(Style.EMPTY.withColor(Formatting.WHITE)));
        result.append("/");
        result.append(Text.literal(String.format("%d", min)).setStyle(Style.EMPTY.withColor(Formatting.WHITE)));
        result.append("/");
        result.append(Text.literal(String.format("%d", max)).setStyle(Style.EMPTY.withColor(Formatting.WHITE)));
        result.append("\n");
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
        result.append(writeTime("constraintIterCount", simulation.iterationCount));
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


    @NotNull
    public static PhysicsStorage get(World world) {
        if(!world.isClient) {
            final PhysicsStorage physicsWorld = ((ServerWorld) world).getPersistentStateManager().get(ServerPhysicsStorage.TYPE, ServerPhysicsStorage.STORAGE_ID);
            assert physicsWorld != null : "This server world doesn't have a physics world attached";
            return physicsWorld;
        } else {
            return clientStorage;
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        return null;
    }
}
