package com.patryk3211.fizite.simulation.physics;

import com.patryk3211.fizite.simulation.physics.simulation.RigidBody;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.Constraint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.List;
import java.util.function.Function;

public class SimplePhysicsCapability extends PhysicsCapability {
    private SimplePhysicsCapability(@NotNull List<PhysicsZone> zones, RigidBody body, @Nullable Function<RigidBody, Constraint[]> constrainer, boolean zoneTransformByAxis) {
        super(zones, new RigidBody[] { body }, constrainer == null ? null : constrainer.apply(body), zoneTransformByAxis);
    }

    public SimplePhysicsCapability(@NotNull List<PhysicsZone> zones, @Nullable Function<RigidBody, Constraint[]> constrainer, boolean zoneTransformByAxis) {
        this(zones, new RigidBody(), constrainer, zoneTransformByAxis);
    }

    public SimplePhysicsCapability(@NotNull List<PhysicsZone> zones, boolean zoneTransformByAxis) {
        this(zones, null, zoneTransformByAxis);
    }
}
