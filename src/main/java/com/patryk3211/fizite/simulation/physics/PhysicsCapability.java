package com.patryk3211.fizite.simulation.physics;

import com.patryk3211.fizite.capability.WrenchConnectableCapability;
import net.minecraft.util.math.Box;

import java.util.Collection;

public class PhysicsCapability extends WrenchConnectableCapability<PhysicsCapability.Zone, PhysicsCapability> {
    public static final class Zone extends InteractionZone {
        public Zone(Box boundingBox) {
            super(boundingBox);
        }
    }

    public PhysicsCapability(Collection<Zone> zones) {
        super("kinetic", PhysicsCapability.class, zones);
    }

    @Override
    public boolean connect(PhysicsCapability connectTo) {
        return false;
    }

    @Override
    public void disconnect(PhysicsCapability disconnectFrom) {

    }
}
