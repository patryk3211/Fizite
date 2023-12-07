package com.patryk3211.fizite.simulation.physics;

import com.patryk3211.fizite.simulation.physics.simulation.RigidBody;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.Constraint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public class SimplePhysicsCapability extends PhysicsCapability {
    private SimplePhysicsCapability(@NotNull List<Zone> zones, RigidBody body, @Nullable Function<RigidBody, Constraint[]> constrainer) {
        super(zones, new RigidBody[] { body }, constrainer == null ? null : constrainer.apply(body));
    }

    public SimplePhysicsCapability(@NotNull List<Zone> zones, @Nullable Function<RigidBody, Constraint[]> constrainer) {
        this(zones, new RigidBody(), constrainer);
    }

    public SimplePhysicsCapability(@NotNull List<Zone> zones) {
        this(zones, null);
    }
}
