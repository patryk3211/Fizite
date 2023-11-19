package com.patryk3211.fizite.simulation.physics.simulation;

import com.patryk3211.fizite.Fizite;
import org.joml.Vector2f;

public class RigidBody {
    private final PhysicalState state;
    private PhysicsWorld world;
    private int rbIndex;
    private String marker;

    private final Vector2f restPosition;
    private float restAngle;
    private boolean restInitialized;

    private float mass;

    public boolean externalForceReset;

    public RigidBody() {
        this.state = new PhysicalState();
        this.restPosition = new Vector2f();
        this.restAngle = 0;
        this.rbIndex = -1;
        this.world = null;
        this.mass = 1;
        this.externalForceReset = false;
        this.restInitialized = false;
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

    public void setMass(float mass) {
        this.mass = mass;
        if(world != null) {
            world.updateMass(rbIndex, mass);
        }
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

    public void setRestPosition(float x, float y, float angle) {
        if(!restInitialized) {
            state.position.x = x;
            state.position.y = y;
            state.positionA = angle;
            restInitialized = true;
        } else {
            // Offset current position by rest position change
            state.position.x += x - restPosition.x;
            state.position.y += y - restPosition.y;
            state.positionA += angle - restAngle;
        }
        // Update rest position
        this.restPosition.set(x, y);
        this.restAngle = angle;
    }

    public Vector2f getRestPosition() {
        return restPosition;
    }

    public float getRestAngle() {
        return restAngle;
    }
}
