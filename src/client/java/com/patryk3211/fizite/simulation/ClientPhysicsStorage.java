package com.patryk3211.fizite.simulation;

import com.patryk3211.fizite.Fizite;
import com.patryk3211.fizite.simulation.physics.PhysicsStorage;
import com.patryk3211.fizite.simulation.physics.simulation.PhysicsWorld;
import com.patryk3211.fizite.simulation.physics.simulation.RigidBody;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector3d;

public class ClientPhysicsStorage extends PhysicsStorage {
    private static final ClientPhysicsStorage physics = new ClientPhysicsStorage();

    public static ClientPhysicsStorage getInstance() {
        return physics;
    }

    private Vector3d[] prevPositions;
    private Vector3d[] prevVelocities;

    public ClientPhysicsStorage() {
        clientStorage = this;
    }

    public static void onWorldTickStart(ClientWorld world) {
        physics.copyPositions();
        physics.simulation.simulate();
//        for (RigidBody body : physics.forceResetBodies) {
//            final var state = body.getState();
//            state.extForce.x = 0;
//            state.extForce.y = 0;
//            state.extForceA = 0;
//        }
    }

    public static void onDisconnect(ClientPlayNetworkHandler networkHandler, MinecraftClient client) {
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

    public Vector3d lerpPos(RigidBody body, float partialTicks, boolean adjustAngle) {
        final var state = body.getState();
        final var pCurrent = new Vector3d(state.position.x, state.position.y, state.positionA);
        if(prevPositions == null)
            return pCurrent;
        if(body.index() < 0 || body.index() >= prevPositions.length)
            return pCurrent;

        final var prevPos = prevPositions[body.index()];
        if(prevPos == null)
            return pCurrent;
        final var prevVel = prevVelocities[body.index()];

        if(adjustAngle) {
            if (prevVel.z > 0 && prevPos.z > pCurrent.z) {
                pCurrent.z += Math.PI * 2;
            } else if (prevVel.z < 0 && prevPos.z < pCurrent.z) {
                pCurrent.z -= Math.PI * 2;
            }
        }

        return pCurrent.lerp(prevPos, 1 - partialTicks);
    }

    public Vector3d lerpPos(RigidBody body, float partialTicks) {
        return lerpPos(body, partialTicks, false);
    }

    public static void onBlockEntityUnload(BlockEntity entity, ClientWorld world) {
        physics.clearPosition(entity.getPos());
    }
}
