package com.patryk3211.fizite.simulation.physics;

import com.patryk3211.fizite.capability.WrenchConnectableCapability;
import com.patryk3211.fizite.simulation.physics.simulation.RigidBody;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.Constraint;
import com.patryk3211.fizite.utility.CommonStyles;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public abstract class PhysicsCapability extends WrenchConnectableCapability<PhysicsCapability.Zone, PhysicsCapability> {
    public static final class Zone extends InteractionZone {
        public final Vector2f anchor;
        public final MovementType movementType;
        public final int bodyIndex;

        public Zone(Box boundingBox, Vector2f anchor, MovementType movementType, int bodyIndex) {
            super(boundingBox);
            this.anchor = anchor;
            this.movementType = movementType;
            this.bodyIndex = bodyIndex;
        }
    }

    private static final String MESSAGE_INCOMPATIBLE = "wrench.fizite.physics.fail_incompatible";
    private static final String MESSAGE_SAME_ENTITY = "wrench.fizite.physics.fail_same_entity";

    protected final RigidBody[] bodies;
    protected final Constraint[] internalConstraints;
    // Array of constraints whose index corresponds to the zone index
    protected final Constraint[] connectedConstraints;

    public PhysicsCapability(@NotNull List<Zone> zones, @NotNull RigidBody[] bodies, @Nullable Constraint[] internalConstraints) {
        super("kinetic", PhysicsCapability.class, zones);
        this.bodies = bodies;
        this.internalConstraints = internalConstraints;
        this.connectedConstraints = new Constraint[zones.size()];
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
    public final InteractionResult connect(Zone thisZone, PhysicsCapability connectTo, Zone otherZone) {
        if(connectTo == this)
            return new InteractionResult(ActionResult.FAIL, Text.translatable(MESSAGE_SAME_ENTITY).setStyle(CommonStyles.RED));

        PhysicalConnection.ConnectionType connectionType = null;
        // Select movement type for the joint
        MovementType jointMovementType;
        if(thisZone.movementType == otherZone.movementType) {
            jointMovementType = thisZone.movementType;
        } else if(thisZone.movementType == MovementType.XY && otherZone.movementType == MovementType.LINEAR) {
            jointMovementType = MovementType.XY;
        } else if(thisZone.movementType == MovementType.LINEAR && otherZone.movementType == MovementType.XY) {
            jointMovementType = MovementType.XY;
        } else {
            // Incompatible movement types, inform the player
            return new InteractionResult(ActionResult.FAIL, Text.translatable(MESSAGE_INCOMPATIBLE, thisZone.movementType, otherZone.movementType).setStyle(CommonStyles.RED));
        }
        // Convert movement type into connection type
        switch(jointMovementType) {
            case LINEAR -> connectionType = PhysicalConnection.ConnectionType.LINEAR;
            case XY -> connectionType = PhysicalConnection.ConnectionType.BEARING;
            case ROTATION -> connectionType = PhysicalConnection.ConnectionType.ROTATION;
        }
        // Create a constraint
        final var thisBody = bodies[thisZone.bodyIndex];
        final var otherBody = connectTo.bodies[otherZone.bodyIndex];
        final var constraint = PhysicalConnection.makeConnection(connectionType, thisBody, otherBody, thisZone.anchor, otherZone.anchor);
        connectedConstraints[zoneIndex(thisZone)] = constraint;
        connectTo.connectedConstraints[connectTo.zoneIndex(otherZone)] = constraint;

        // Add the constraint to simulation
        Objects.requireNonNull(entity.getWorld());
        PhysicsStorage.get(entity.getWorld()).add(constraint);
        return new InteractionResult(ActionResult.SUCCESS, null);
    }

    @Override
    public final void disconnect(Zone thisZone, PhysicsCapability disconnectFrom, Zone otherZone) {
        Objects.requireNonNull(entity.getWorld());
        PhysicsStorage.get(entity.getWorld()).remove(connectedConstraints[zoneIndex(thisZone)]);

        connectedConstraints[zoneIndex(thisZone)] = null;
        disconnectFrom.connectedConstraints[disconnectFrom.zoneIndex(otherZone)] = null;
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
