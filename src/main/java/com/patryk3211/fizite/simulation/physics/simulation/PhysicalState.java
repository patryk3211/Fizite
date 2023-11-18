package com.patryk3211.fizite.simulation.physics.simulation;

import org.joml.Vector2d;
import org.joml.Vector2fc;

public class PhysicalState {
    public final Vector2d position;
    public double positionA;

    public final Vector2d velocity;
    public double velocityA;

    public final Vector2d acceleration;
    public double accelerationA;

    public final Vector2d extForce;
    public double extForceA;

    public final Vector2d cForce;
    public double cForceA;

    public PhysicalState() {
        position = new Vector2d(0, 0);
        positionA = 0;

        velocity = new Vector2d(0, 0);
        velocityA = 0;

        acceleration = new Vector2d(0, 0);
        accelerationA = 0;

        extForce = new Vector2d(0, 0);
        extForceA = 0;

        cForce = new Vector2d(0, 0);
        cForceA = 0;
    }

    public void worldPosition(Vector2fc local, Vector2d world) {
        final var thetaCos = Math.cos(positionA);
        final var thetaSin = Math.sin(positionA);

        world.set(
                local.x() * thetaCos - thetaSin * local.y() + this.position.x,
                local.x() * thetaSin + thetaCos * local.y() + this.position.y
        );
    }

    public void applyForceAt(float forceX, float forceY, float localX, float localY) {
        this.extForce.add(forceX, forceY);

        final var thetaCos = Math.cos(positionA);
        final var thetaSin = Math.sin(positionA);

        final var wX = thetaCos * localX - thetaSin * localY + this.position.x;
        final var wY = thetaSin * localX + thetaCos * localY + this.position.y;

        this.extForceA += (wY - this.position.y) * -forceX + (wX - this.position.x) * forceY;
    }

    public void copy(PhysicalState state) {
        position.set(state.position);
        positionA = state.positionA;

        velocity.set(state.velocity);
        velocityA = state.velocityA;

        acceleration.set(state.acceleration);
        accelerationA = state.accelerationA;

        extForce.set(state.extForce);
        extForceA = state.extForceA;

        cForce.set(state.cForce);
        cForceA = state.cForceA;
    }
}
