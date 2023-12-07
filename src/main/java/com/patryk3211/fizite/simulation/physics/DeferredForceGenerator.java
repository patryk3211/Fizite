package com.patryk3211.fizite.simulation.physics;

import com.patryk3211.fizite.simulation.physics.simulation.RigidBody;

public final class DeferredForceGenerator extends ForceGeneratorCapability {
    public float forceX;
    public float forceY;
    public float forceA;

    private final int bodyIndex;
    private final boolean forceReset;
    private RigidBody body;

    public DeferredForceGenerator(int bodyIndex, boolean forceReset) {
        this.bodyIndex = bodyIndex;
        this.forceReset = forceReset;
        tickOn(TickOn.BOTH);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        final var physCap = entity.getCapability(PhysicsCapability.class);
        assert physCap != null : "Entity has to have a physics capability when force generator capability is used";
        body = physCap.body(bodyIndex);
    }

    @Override
    public void apply(double deltaTime) {
        final var state = body.getState();
        state.extForce.x = forceX;
        state.extForce.y = forceY;
        state.extForceA = forceA;
    }

    @Override
    public void tick() {
        super.tick();
        if(forceReset) {
            forceX = 0;
            forceY = 0;
            forceA = 0;
        }
    }
}
