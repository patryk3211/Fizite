package com.patryk3211.fizite.simulation.physics;

import com.patryk3211.fizite.Fizite;
import io.wispforest.owo.network.OwoNetChannel;
import io.wispforest.owo.network.ServerAccess;
import io.wispforest.owo.network.serialization.PacketBufSerializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.world.World;

import java.util.*;

public class Networking {
    public static OwoNetChannel CHANNEL;

    public record ClientAddBlockEntity(BlockPos entityPosition, int[] rigidBodyIndices) { }
    public record ClientSyncState(int[] bodyIndices, Vec2f[] positions, Vec2f[] velocities, float[] angles, float[] angularVelocities) { }
    public record ClientGetSimulation(BlockPos[] entities, int[][] rigidBodyIndices) { }
//    public record ClientRemoveRigidBodies(BlockPos entityPosition) { }
    public record ServerRequestBlockEntity(BlockPos entityPosition, Identifier world) { }

    private static class WaitListEntry {
        public final Set<ServerPlayerEntity> players;
        public int cycles;

        public WaitListEntry(ServerPlayerEntity player) {
            this.players = new HashSet<>();
            this.players.add(player);
            cycles = 0;
        }
    }
    private static final Map<BlockPos, WaitListEntry> waitingForEntity = new HashMap<>();

    public static void initialize() {
        CHANNEL = OwoNetChannel.create(new Identifier(Fizite.MOD_ID, "physics"));

        PacketBufSerializer.register(Vec2f.class, (packetByteBuf, vec) -> {
            packetByteBuf.writeFloat(vec.x);
            packetByteBuf.writeFloat(vec.y);
        }, packetByteBuf -> {
            float x = packetByteBuf.readFloat();
            float y = packetByteBuf.readFloat();
            return new Vec2f(x, y);
        });

        CHANNEL.registerClientboundDeferred(ClientAddBlockEntity.class);
        CHANNEL.registerClientboundDeferred(ClientSyncState.class);
        CHANNEL.registerClientboundDeferred(ClientGetSimulation.class);
//        CHANNEL.registerClientboundDeferred(ClientRemoveRigidBodies.class);
        CHANNEL.registerServerbound(ServerRequestBlockEntity.class, Networking::handleRequestBlockEntity);
    }

    @Environment(EnvType.CLIENT)
    public static void sendBlockEntityRequest(BlockPos position, RegistryKey<World> worldKey) {
        CHANNEL.clientHandle().send(new Networking.ServerRequestBlockEntity(position, worldKey.getValue()));
    }

    private static void handleRequestBlockEntity(ServerRequestBlockEntity packet, ServerAccess access) {
        final var server = access.runtime();
        final var world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, packet.world));
        if(world == null) {
            Fizite.LOGGER.error("Server doesn't have a world with key " + packet.world);
            return;
        }
        final var provider = PhysicsStorage.get(world).getProvider(packet.entityPosition);
        if(provider == null) {
            if(waitingForEntity.containsKey(packet.entityPosition)) {
                waitingForEntity.get(packet.entityPosition).players.add(access.player());
            } else {
                waitingForEntity.put(packet.entityPosition, new WaitListEntry(access.player()));
            }
//            Fizite.LOGGER.error("Requested physics indices for position without a physics provider");
            return;
        }

        final var bodies = provider.bodies();
        final int[] indices = new int[bodies.length];
        for(int i = 0; i < bodies.length; ++i) {
            indices[i] = bodies[i].index();
        }

        CHANNEL.serverHandle(access.player()).send(new ClientAddBlockEntity(packet.entityPosition, indices));
    }

    public static void entityAdded(BlockPos pos, IPhysicsProvider provider) {
        final var entry = waitingForEntity.remove(pos);
        if(entry == null)
            return;

        final var bodies = provider.bodies();
        final int[] indices = new int[bodies.length];
        for(int i = 0; i < bodies.length; ++i) {
            indices[i] = bodies[i].index();
        }

        CHANNEL.serverHandle(entry.players).send(new ClientAddBlockEntity(pos, indices));
    }

    public static void cleanupList() {
        final Set<BlockPos> removePositions = new HashSet<>();
        waitingForEntity.forEach((pos, entry) -> {
            if(++entry.cycles >= 2) {
                removePositions.add(pos);
            }
        });
        removePositions.forEach(pos -> {
            final var entry = waitingForEntity.remove(pos);
            StringBuilder playerListBuilder = new StringBuilder();
            for(final var player : entry.players) {
                playerListBuilder.append("'").append(player.getName()).append("', ");
            }
            final var playerList = playerListBuilder.delete(playerListBuilder.length() - 3, playerListBuilder.length() - 1).toString();
            Fizite.LOGGER.warn("Players (" + playerList + ") have requested physics data for block entity at " + pos + ", but the entity was never created on the server");
        });
    }
}
