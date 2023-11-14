package com.patryk3211.fizite.simulation;

import com.patryk3211.fizite.Fizite;
import com.patryk3211.fizite.simulation.physics.IPhysicsProvider;
import com.patryk3211.fizite.simulation.physics.Networking;
import io.wispforest.owo.network.ClientAccess;
import io.wispforest.owo.network.OwoNetChannel;

public class ClientPhysicsNetworking {
    public static void initialize() {
        final OwoNetChannel CHANNEL = Networking.CHANNEL;
        CHANNEL.registerClientbound(Networking.ClientAddBlockEntity.class, ClientPhysicsNetworking::handleAddBlockEntity);
        CHANNEL.registerClientbound(Networking.ClientSyncState.class, ClientPhysicsNetworking::handleSyncState);
        CHANNEL.registerClientbound(Networking.ClientGetSimulation.class, ClientPhysicsNetworking::handleGetSimulation);
    }

    private static void handleGetSimulation(Networking.ClientGetSimulation packet, ClientAccess access) {
        final var client = access.runtime();
        if(client.world == null) {
            Fizite.LOGGER.error("Client receive block entity data but doesn't have a world");
            return;
        }
        final var positions = packet.entities();
        for(int i = 0; i < positions.length; ++i) {
            final var entity = client.world.getBlockEntity(positions[i]);
            if(!(entity instanceof IPhysicsProvider)) {
                Fizite.LOGGER.error("Client block entity is not a physics provider");
                continue;
            }
            final var indices = packet.rigidBodyIndices()[i];
            ClientPhysicsStorage.getInstance().addBlockEntity(entity, indices);
        }
    }

    private static void handleAddBlockEntity(Networking.ClientAddBlockEntity packet, ClientAccess access) {
        final var client = access.runtime();
        if(client.world == null) {
            Fizite.LOGGER.error("Client receive block entity data but doesn't have a world");
            return;
        }
        final var entity = client.world.getBlockEntity(packet.entityPosition());
        if(!(entity instanceof IPhysicsProvider)) {
            Fizite.LOGGER.error("Client block entity is not a physics provider");
            return;
        }
        final var indices = packet.rigidBodyIndices();
        ClientPhysicsStorage.getInstance().addBlockEntity(entity, indices);
    }

    private static void handleSyncState(Networking.ClientSyncState packet, ClientAccess access) {
        final var bodies = ClientPhysicsStorage.getInstance().simulation().bodies();

        final var indices = packet.bodyIndices();
        for(int i = 0; i < indices.length; ++i) {
            final var position = packet.positions()[i];
            final var velocity = packet.velocities()[i];
            final var angle = packet.angles()[i];
            final var angularVelocity = packet.angularVelocities()[i];

            if(bodies.size() > indices[i]) {
                final var body = bodies.get(indices[i]);
                if (body != null) {
                    final var state = body.getState();
                    state.position.x = position.x;
                    state.position.y = position.y;
                    state.velocity.x = velocity.x;
                    state.velocity.y = velocity.y;
                    state.positionA = angle;
                    state.velocityA = angularVelocity;
                }
            }
        }
    }
}
