package com.patryk3211.fizite.simulation.physics;

import com.patryk3211.fizite.capability.Capability;
import com.patryk3211.fizite.simulation.physics.simulation.IForceGenerator;
import com.patryk3211.fizite.simulation.physics.simulation.RigidBody;

public abstract class ForceGeneratorCapability extends Capability implements IForceGenerator {
    private PhysicsCapability physics;

    public ForceGeneratorCapability() {
        super("physics_force");
    }

    @Override
    public void onLoad() {
        super.onLoad();

        physics = entity.getCapability(PhysicsCapability.class);
    }

    protected RigidBody body(int index) {
        return physics.body(index);
    }
}
