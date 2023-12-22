package com.patryk3211.fizite.simulation;

import com.patryk3211.fizite.Fizite;
import com.patryk3211.fizite.simulation.physics.PhysicsCapability;
import com.patryk3211.fizite.simulation.physics.PhysicsStorage;
import com.patryk3211.fizite.simulation.physics.simulation.PhysicsWorld;
import com.patryk3211.fizite.simulation.physics.simulation.RigidBody;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector2d;
import org.joml.Vector3d;

public class ClientPhysicsStorage extends PhysicsStorage {
    private static ClientPhysicsStorage physics;

    private Vector3d[] prevPositions;
    private Vector3d[] prevVelocities;

    public ClientPhysicsStorage() {
        clientStorage = this;
        physics = this;
    }

    public static ClientPhysicsStorage get() {
        return physics;
    }

    @SuppressWarnings("unused")
    public static void onWorldTickStart(ClientWorld world) {
        physics.copyPositions();
        physics.simulation.simulate();
    }

    public static void onDisconnect() {
        Fizite.LOGGER.info("Clearing client physics simulation");
        physics.dataMap.clear();
        physics.simulation.clear();
    }

    public static void recordClientFrames(int frameCount, Runnable finishedPhysicsRecording) {
        physics.simulation.addOutputWriter(frameCount, finishedPhysicsRecording);
    }

    public PhysicsWorld simulation() {
        return simulation;
    }

    @Override
    public void add(BlockPos position, PhysicsCapability capability) {
        ClientNetworking.sendBlockEntityRequest(position);
    }

    public void add(BlockPos position, PhysicsCapability capability, int[] indices) {
        super.add(position, capability);

        // Add all rigid bodies and internal constraints to the simulation
        for(int i = 0; i < capability.bodyCount(); ++i) {
            simulation.addRigidBody(capability.body(i), indices[i]);
        }
        final var internalConstraints = capability.internalConstraints();
        if(internalConstraints != null)
            internalConstraints.forEachRemaining(simulation::addConstraint);
    }

    private void copyPositions() {
        final var system = physics.simulation.system();
        if(prevPositions == null || prevPositions.length < system.size()) {
            prevPositions = new Vector3d[system.size()];
            prevVelocities = new Vector3d[system.size()];
        }

        final var states = system.getStates();
        for(int i = 0; i < system.size(); ++i) {
            if(states[i] == null)
                continue;
            if(prevPositions[i] == null) {
                prevPositions[i] = new Vector3d();
                prevVelocities[i] = new Vector3d();
            }
            final var pos = prevPositions[i];
            pos.x = states[i].position.x;
            pos.y = states[i].position.y;
            pos.z = states[i].positionA;
            final var vel = prevVelocities[i];
            vel.x = states[i].velocity.x;
            vel.y = states[i].velocity.y;
            vel.z = states[i].velocityA;
        }
    }

    public Vector2d lerpPos(RigidBody body, float partialTicks) {
        final var state = body.getState();
        final var pCurrent = new Vector2d(state.position.x, state.position.y);
        if(prevPositions == null)
            return pCurrent;
        if(body.index() < 0 || body.index() >= prevPositions.length)
            return pCurrent;

        final var prevPos3d = prevPositions[body.index()];
        if(prevPos3d == null)
            return pCurrent;
        final var prevPos = new Vector2d(prevPos3d.x, prevPos3d.y);

        return pCurrent.lerp(prevPos, 1 - partialTicks);
    }

    public double lerpAngle(RigidBody body, float partialTicks) {
        final var state = body.getState();
        final var pCurrent = state.positionA;
        if(prevPositions == null)
            return pCurrent;
        if(body.index() < 0 || body.index() >= prevPositions.length)
            return pCurrent;

        final var prevPos3d = prevPositions[body.index()];
        if(prevPos3d == null)
            return pCurrent;
        final var pPrev = prevPos3d.z;

        // Do the lerping on a 2d vector
        final var target = new Vector2d(Math.cos(pCurrent), Math.sin(pCurrent));
        final var current = new Vector2d(Math.cos(pPrev), Math.sin(pPrev));
        final var lerped = current.lerp(target, partialTicks);

        return Math.atan2(lerped.y, lerped.x);
    }

    public void postUpdate() {
        simulation.fireStepHandler();
    }
}
