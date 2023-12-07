package com.patryk3211.fizite.simulation.physics;

import com.patryk3211.fizite.capability.Capability;
import com.patryk3211.fizite.simulation.physics.simulation.IForceGenerator;

public abstract class ForceGeneratorCapability extends Capability implements IForceGenerator {
    public ForceGeneratorCapability() {
        super("physics_force");
    }
}
