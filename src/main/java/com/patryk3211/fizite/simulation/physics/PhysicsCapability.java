package com.patryk3211.fizite.simulation.physics;

import com.patryk3211.fizite.Fizite;
import com.patryk3211.fizite.capability.CapabilitiesBlockEntity;
import com.patryk3211.fizite.capability.WrenchConnectableCapability;
import com.patryk3211.fizite.simulation.physics.simulation.RigidBody;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.Constraint;
import com.patryk3211.fizite.utility.CommonStyles;
import com.patryk3211.fizite.utility.Nbt;
import io.wispforest.owo.nbt.NbtKey;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2d;
import org.joml.Vector2f;

import java.text.ParseException;
import java.util.*;
import java.util.function.Function;

public abstract class PhysicsCapability extends WrenchConnectableCapability<PhysicsZone, PhysicsCapability> {
    private static final String MESSAGE_INCOMPATIBLE = "wrench.fizite.physics.fail_incompatible";
    private static final String MESSAGE_SAME_ENTITY = "wrench.fizite.physics.fail_same_entity";

    private static final NbtKey<NbtList> RIGID_BODIES = new NbtKey.ListKey<>("bodies", NbtKey.Type.COMPOUND);
    private static final NbtKey<Vector2d> RIGID_BODY_POSITION = new NbtKey<>("p", Nbt.Type.VECTOR_2D);
    private static final NbtKey<Double> RIGID_BODY_ANGLE = new NbtKey<>("a", NbtKey.Type.DOUBLE);
    private static final NbtKey<Vector2d> RIGID_BODY_VELOCITY = new NbtKey<>("v", Nbt.Type.VECTOR_2D);
    private static final NbtKey<Double> RIGID_BODY_ANGULAR_VELOCITY = new NbtKey<>("av", NbtKey.Type.DOUBLE);

    private static final NbtKey<NbtCompound> CONNECTIONS = new NbtKey<>("connections", NbtKey.Type.COMPOUND);
    private static final NbtKey<Integer> CONNECTION_FOREIGN_ZONE = new NbtKey<>("foreignZone", NbtKey.Type.INT);
    private static final NbtKey<BlockPos> CONNECTION_POSITION = new NbtKey<>("foreignPosition", Nbt.Type.BLOCK_POS);

    protected final RigidBody[] bodies;
    protected final Constraint[] internalConstraints;
    // Array of constraints whose index corresponds to the zone index
    protected final Constraint[] connectedConstraints;
    private final boolean zoneTransformByAxis;

    private record RestoredConnection(int zone, int foreignZone, BlockPos foreignPos) { }
    private List<RestoredConnection> restoredConnections;

    public PhysicsCapability(@NotNull List<PhysicsZone> zones, @NotNull RigidBody[] bodies, @Nullable Constraint[] internalConstraints, boolean zoneTransformByAxis) {
        super("kinetic", PhysicsCapability.class, zones);
        this.bodies = bodies;
        this.internalConstraints = internalConstraints;
        this.connectedConstraints = new Constraint[zones.size()];
        this.zoneTransformByAxis = zoneTransformByAxis;
    }

    public final RigidBody body(int index) {
        return bodies[index];
    }

    public final int bodyCount() {
        return bodies.length;
    }

    public final Iterator<Constraint> internalConstraints() {
        return internalConstraints == null ? null : Arrays.stream(internalConstraints).iterator();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        Objects.requireNonNull(entity.getWorld());
        PhysicsStorage.get(entity.getWorld()).add(entity.getPos(), this);
    }

    @Override
    public void onUnload() {
        super.onUnload();
        Objects.requireNonNull(entity.getWorld());
        PhysicsStorage.get(entity.getWorld()).remove(entity.getPos());
    }

    @Override
    public void initialTick() {
        super.initialTick();
        if(restoredConnections == null)
            return;

        Objects.requireNonNull(entity.getWorld());
        for(final var conn : restoredConnections) {
            if (connectedConstraints[conn.zone] != null) {
                // Connection already restored, check its validity
                if (conn.foreignZone != super.connections[conn.zone].otherZoneIndex ||
                    !conn.foreignPos.equals(super.connections[conn.zone].otherCapability.getEntity().getPos())) {
                    // Connection restored to a different point, invalid save state.
                    Fizite.LOGGER.error("PhysicsCapability has a restored connection pointing to a different body than what's specified by the stored entry.");
                }
            } else {
                // Recreate the connection
                final var thisZone = getZone(conn.zone);
                final var otherEntity = CapabilitiesBlockEntity.getEntity(entity.getWorld(), conn.foreignPos);
                if (otherEntity == null) {
                    Fizite.LOGGER.error("PhysicsCapability connection restoration failed, position doesn't have a valid block entity");
                    continue;
                }
                final var otherCap = otherEntity.getCapability(PhysicsCapability.class);
                if (otherCap == null) {
                    Fizite.LOGGER.error("PhysicsCapability connection restoration failed, position doesn't have a valid block entity");
                    continue;
                }
                final var otherZone = otherCap.getZone(conn.foreignZone);
                connect(conn.zone, thisZone, otherCap, conn.foreignZone, otherZone);
            }
        }
        restoredConnections = null;
    }

    @Override
    protected Vec3d transformLocalPos(Vec3d worldLocalPos) {
        final var state = entity.getCachedState();
        if(state.contains(Properties.FACING) || state.contains(Properties.HORIZONTAL_FACING)) {
            // Conduct some basic position transformations based on the facing of the block
            Direction facing;
            if(state.contains(Properties.FACING))
                facing = state.get(Properties.FACING);
            else
                facing = state.get(Properties.HORIZONTAL_FACING);

            if(zoneTransformByAxis)
                return transformByAxis(facing.getAxis(), worldLocalPos);
            else
                return transformByFacing(facing, worldLocalPos);
        }
        return worldLocalPos;
    }

    @Override
    public NbtElement writeNbt() {
        final var nbt = new NbtCompound();
        // Save body states
        final var bodyStateList = new NbtList();
        for (final RigidBody body : bodies) {
            final var state = body.getState();
            final var entry = new NbtCompound();

            entry.put(RIGID_BODY_POSITION, state.position);
            entry.put(RIGID_BODY_ANGLE, state.positionA);
            entry.put(RIGID_BODY_VELOCITY, state.velocity);
            entry.put(RIGID_BODY_ANGULAR_VELOCITY, state.velocityA);
            bodyStateList.add(entry);
        }
        nbt.put(RIGID_BODIES, bodyStateList);

        // Save connections
        final var connections = new NbtCompound();
        for(int i = 0; i < connectedConstraints.length; ++i) {
            final var conn = connectedConstraints[i];
            if(conn == null)
                continue;
            // Save the data required to recreate this connection
            final var entry = new NbtCompound();
            entry.put(CONNECTION_FOREIGN_ZONE, super.connections[i].otherZoneIndex);
            entry.put(CONNECTION_POSITION, super.connections[i].otherCapability.getEntity().getPos());
            connections.put(Integer.toString(i), entry);
        }
        nbt.put(CONNECTIONS, connections);
        return nbt;
    }

    @Override
    public void readNbt(@NotNull NbtElement tag) {
        if(!(tag instanceof final NbtCompound nbt)) {
            Fizite.LOGGER.error("PhysicsCapability received an invalid type of NBT tag from save data");
            return;
        }
        // Load body states
        final var bodyStateList = nbt.get(RIGID_BODIES);
        if(bodyStateList.size() != bodies.length) {
            Fizite.LOGGER.error("PhysicsCapability NBT tag list length mismatch");
            return;
        }
        for(int i = 0; i < bodies.length; ++i) {
            final var state = bodies[i].getState();
            final var entry = bodyStateList.getCompound(i);

            state.position.set(entry.get(RIGID_BODY_POSITION));
            state.positionA = entry.get(RIGID_BODY_ANGLE);
            state.velocity.set(entry.get(RIGID_BODY_VELOCITY));
            state.velocityA = entry.get(RIGID_BODY_ANGULAR_VELOCITY);
        }
        // Recreate connections
        restoredConnections = new LinkedList<>();
        final var connections = nbt.get(CONNECTIONS);
        for (String key : connections.getKeys()) {
            try {
                final var i = Integer.parseUnsignedInt(key);
                final var entry = connections.getCompound(key);
                final var zone = entry.get(CONNECTION_FOREIGN_ZONE);
                final var pos = entry.get(CONNECTION_POSITION);

                restoredConnections.add(new RestoredConnection(i, zone, pos));
            } catch (NumberFormatException e) {
                Fizite.LOGGER.error("Invalid key format in PhysicsCapability.Connections NBT tag");
            }
        }
    }

    @Override
    public final InteractionResult connect(int thisIndex, PhysicsZone thisZone, PhysicsCapability connectTo, int otherIndex, PhysicsZone otherZone) {
        if(connectTo == this)
            return new InteractionResult(ActionResult.FAIL, Text.translatable(MESSAGE_SAME_ENTITY).setStyle(CommonStyles.RED));

        final var thisInfo = thisZone.getConnectionInfo(this);
        final var otherInfo = otherZone.getConnectionInfo(connectTo);

        PhysicalConnection.ConnectionType connectionType = null;
        // Select movement type for the joint
        MovementType jointMovementType;
        if(thisInfo.movementType() == otherInfo.movementType()) {
            jointMovementType = thisInfo.movementType();
        } else if(thisInfo.movementType() == MovementType.XY && otherInfo.movementType() == MovementType.LINEAR) {
            jointMovementType = MovementType.XY;
        } else if(thisInfo.movementType() == MovementType.LINEAR && otherInfo.movementType() == MovementType.XY) {
            jointMovementType = MovementType.XY;
        } else {
            // Incompatible movement types, inform the player
            return new InteractionResult(ActionResult.FAIL, Text.translatable(MESSAGE_INCOMPATIBLE, thisInfo.movementType(), otherInfo.movementType()).setStyle(CommonStyles.RED));
        }
        // Convert movement type into connection type
        switch(jointMovementType) {
            case LINEAR -> connectionType = PhysicalConnection.ConnectionType.LINEAR;
            case XY -> connectionType = PhysicalConnection.ConnectionType.BEARING;
            case ROTATION -> connectionType = PhysicalConnection.ConnectionType.ROTATION;
        }
        // Create a constraint
        final var thisBody = bodies[thisInfo.bodyIndex()];
        final var otherBody = connectTo.bodies[otherInfo.bodyIndex()];
        final var constraint = PhysicalConnection.makeConnection(connectionType, thisBody, otherBody, thisInfo.anchor(), otherInfo.anchor());
        connectedConstraints[thisIndex] = constraint;
        connectTo.connectedConstraints[otherIndex] = constraint;

        // Add the constraint to simulation
        Objects.requireNonNull(entity.getWorld());
        PhysicsStorage.get(entity.getWorld()).add(constraint);
        return super.connect(thisIndex, thisZone, connectTo, otherIndex, otherZone);
    }

    @Override
    public final void disconnect(int thisIndex, PhysicsZone thisZone, PhysicsCapability disconnectFrom, int otherIndex, PhysicsZone otherZone) {
        Objects.requireNonNull(entity.getWorld());
        PhysicsStorage.get(entity.getWorld()).remove(connectedConstraints[zoneIndex(thisZone)]);

        connectedConstraints[thisIndex] = null;
        disconnectFrom.connectedConstraints[otherIndex] = null;
        super.disconnect(thisIndex, thisZone, disconnectFrom, otherIndex, otherZone);
    }

    @Override
    public void debugOutput(List<Text> output) {
        output.add(Text.literal("Physics Capability:"));
        for(int i = 0; i < bodies.length; ++i) {
            final var body = bodies[i];
            final var state = body.getState();
            output.add(Text.literal(String.format("  [%d] Position = (%.3e, %.3e)", i, state.position.x, state.position.y)));
            output.add(Text.literal(String.format("  [%d] Angle = %.3f", i, state.positionA)));
            output.add(Text.literal(String.format("  [%d] Velocity = (%.3e, %.3e)", i, state.velocity.x, state.velocity.y)));
            output.add(Text.literal(String.format("  [%d] Angular Velocity = %.3e", i, state.velocityA)));
        }
    }
}
