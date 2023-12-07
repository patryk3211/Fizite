package com.patryk3211.fizite.simulation.physics.simulation;

import com.patryk3211.fizite.Fizite;

public class RigidBody {
    private final PhysicalState state;
    private PhysicsWorld world;
    private int rbIndex;
    private String marker;

    private float mass;

    private boolean lockPosition;

    public RigidBody() {
        this.state = new PhysicalState();
        this.rbIndex = -1;
        this.world = null;
        this.mass = 1;
        this.lockPosition = false;
        this.marker = "";
    }

    public void setMarker(String marker) {
        this.marker = marker;
    }

    public String getMarker() {
        return marker;
    }

    public float getMass() {
        return mass;
    }

    private void updateMass() {
        if(world != null) {
            world.updateMass(rbIndex, posInvMass(), posInvMass(), rotInvMass());
        }
    }

    public float posInvMass() {
        return lockPosition ? 0 : 1.0f / mass;
    }

    public float rotInvMass() {
        // TODO: Change this to moment of inertia
        return 1.0f / mass;
    }

    public void setMass(float mass) {
        this.mass = mass;
        updateMass();
    }

    public void lockPosition() {
        this.lockPosition = true;
        updateMass();
    }

    public int index() {
        return rbIndex;
    }

    public void assign(int index, PhysicsWorld world) {
        if(this.rbIndex != -1) {
            Fizite.LOGGER.warn("Assigning a RigidBody to a world even though it was already assigned one");
        }
        this.rbIndex = index;
        this.world = world;
    }

    public PhysicalState getState() {
        return state;
    }

    public double kineticEnergy() {
        return (state.velocity.lengthSquared() * mass + state.velocityA * state.velocityA * mass) / 2;
    }

    public PhysicsWorld getWorld() {
        return world;
    }
}
