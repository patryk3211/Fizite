package com.patryk3211.fizite.simulation.physics.simulation;

import com.patryk3211.fizite.Fizite;
import net.minecraft.util.math.Vec2f;
import org.joml.Vector2d;
import org.joml.Vector2dc;
import org.joml.Vector2f;
import org.joml.Vector2fc;

public class RigidBody {
    private final PhysicalState state;
    private PhysicsWorld world;
    private int rbIndex;

    private final Vector2f restPosition;
    private float restAngle;

    private float mass;

    public RigidBody() {
        this.state = new PhysicalState();
        this.restPosition = new Vector2f();
        this.restAngle = 0;
        this.rbIndex = -1;
        this.world = null;
        this.mass = 1;
    }

    public void velocityWithImpulse(float dT, Vector2d velocity) {
        velocity.set(
                state.cForce.x * dT / mass + state.velocity.x,
                state.cForce.y * dT / mass + state.velocity.y
        );
    }

    public void velocityWithImpulseAt(Vector2fc local, float dT, Vector2d velocity) {
        final var thetaCos = Math.cos(state.positionA);
        final var thetaSin = Math.sin(state.positionA);

        final var wX = thetaCos * local.x() - thetaSin * local.y();
        final var wY = thetaSin * local.x() + thetaCos * local.y();

        final var velAWithImpulse = state.velocityA + state.cForceA * dT / mass;
        final var linearVelX = -velAWithImpulse * wY;
        final var linearVelY = velAWithImpulse * wX;

        velocity.set(
                state.cForce.x * dT / mass + state.velocity.x + linearVelX,
                state.cForce.y * dT / mass + state.velocity.y + linearVelY
        );
    }

    public void applyImpulse(Vector2dc impulse) {
        state.cForce.x += impulse.x();
        state.cForce.y += impulse.y();
    }

    public void applyImpulse(Vector2fc local, Vector2dc impulse) {
        state.cForce.x += impulse.x();
        state.cForce.y += impulse.y();

        final var thetaCos = Math.cos(state.positionA);
        final var thetaSin = Math.sin(state.positionA);

        final var wX = thetaCos * local.x() - thetaSin * local.y();
        final var wY = thetaSin * local.x() + thetaCos * local.y();

        state.cForceA += wY * -impulse.x() + wX * impulse.y();
    }

    public void applyThetaImpulse(Vector2fc local, Vector2dc impulse) {
        final var thetaCos = Math.cos(state.positionA);
        final var thetaSin = Math.sin(state.positionA);

        final var wX = thetaCos * local.x() - thetaSin * local.y();
        final var wY = thetaSin * local.x() + thetaCos * local.y();

        state.cForceA += wY * -impulse.x() + wX * impulse.y();
    }

    public void applyThetaImpulse(double value) {
        state.cForceA += value;
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
