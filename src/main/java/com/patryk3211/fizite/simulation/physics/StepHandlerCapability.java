package com.patryk3211.fizite.simulation.physics;

import com.patryk3211.fizite.capability.Capability;
import com.patryk3211.fizite.simulation.physics.simulation.IPhysicsStepHandler;

public abstract class StepHandlerCapability extends Capability implements IPhysicsStepHandler {
    public StepHandlerCapability() {
        super("physics_step");
    }
}
