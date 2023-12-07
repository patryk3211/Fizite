package com.patryk3211.fizite.simulation;

import com.patryk3211.fizite.Fizite;
import com.patryk3211.fizite.capability.CapabilitiesBlockEntity;
import com.patryk3211.fizite.simulation.gas.GasCapability;
import com.patryk3211.fizite.simulation.gas.GasCell;
import com.patryk3211.fizite.simulation.physics.IPhysicsProvider;
import com.patryk3211.fizite.simulation.physics.PhysicsCapability;
import io.wispforest.owo.network.ClientAccess;
import io.wispforest.owo.network.OwoNetChannel;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Map;

public class ClientNetworking {
    public static void initialize() {
        final OwoNetChannel CHANNEL = Networking.CHANNEL;
        CHANNEL.registerClientbound(Networking.ClientAddBlockEntity.class, ClientNetworking::handleAddBlockEntity);
        CHANNEL.registerClientbound(Networking.ClientSyncState.class, ClientNetworking::handleSyncState);
        CHANNEL.registerClientbound(Networking.ClientGetSimulation.class, ClientNetworking::handleGetSimulation);
        CHANNEL.registerClientbound(Networking.ClientSyncGasState.class, ClientNetworking::handleGasSync);
    }

    public static void addToGasSync(BlockPos pos) {
        final var packet = new Networking.ServerAddGasSyncPosition(pos);
        Networking.CHANNEL.clientHandle().send(packet);
    }

    public static void removeFromGasSync(long id) {
        final var packet = new Networking.ServerRemoveGasSyncPosition(id);
        Networking.CHANNEL.clientHandle().send(packet);
    }

    public static void sendBlockEntityRequest(BlockPos position) {
        Networking.CHANNEL.clientHandle().send(new Networking.ServerRequestBlockEntity(position));
    }

    private static void handleGasSync(Networking.ClientSyncGasState packet, ClientAccess access) {
        final var client = access.runtime();
        if(client.world == null) {
            Fizite.LOGGER.error("Client received gas state sync but doesn't have a world");
            return;
        }
        // Try to synchronize all received states
        for (Networking.GasState state : packet.states()) {
            ClientGasStorage.get().setState(state.cellId(), state.Ek(), state.n(), new Vector3d(state.V_x(), state.V_y(), state.V_z()));
        }
    }

    private static void handleGetSimulation(Networking.ClientGetSimulation packet, ClientAccess access) {
        final var client = access.runtime();
        if(client.world == null) {
            Fizite.LOGGER.error("Client received block entity data but doesn't have a world");
            return;
        }
        final var positions = packet.entities();
        for(int i = 0; i < positions.length; ++i) {
            final var entity = CapabilitiesBlockEntity.getEntity(client.world, positions[i]);
            if(entity == null) {
                Fizite.LOGGER.error("Client block entity doesn't exist at " + positions[i]);
                continue;
            }
            final var physCap = entity.getCapability(PhysicsCapability.class);
            if(physCap == null) {
                Fizite.LOGGER.error("Client block entity doesn't have the physics capability");
                continue;
            }
            final var indices = packet.rigidBodyIndices()[i];
            ClientPhysicsStorage.get().add(positions[i], physCap, indices);
        }
    }

    private static void handleAddBlockEntity(Networking.ClientAddBlockEntity packet, ClientAccess access) {
        final var client = access.runtime();
        if(client.world == null) {
            Fizite.LOGGER.error("Client receive block entity data but doesn't have a world");
            return;
        }
        final var entity = CapabilitiesBlockEntity.getEntity(client.world, packet.entityPosition());
        if(entity == null) {
            Fizite.LOGGER.error("Client block entity doesn't exist at " + packet.entityPosition());
            return;
        }
        final var physCap = entity.getCapability(PhysicsCapability.class);
        if(physCap == null) {
            Fizite.LOGGER.error("Client block entity doesn't have the physics capability");
            return;
        }
        final var indices = packet.rigidBodyIndices();
        ClientPhysicsStorage.get().add(packet.entityPosition(), physCap, indices);
    }

    private static void handleSyncState(Networking.ClientSyncState packet, ClientAccess access) {
        final var bodies = ClientPhysicsStorage.get().simulation().bodies();

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

        ClientPhysicsStorage.get().postUpdate();
    }
}
