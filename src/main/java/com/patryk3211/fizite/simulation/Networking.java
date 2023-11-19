package com.patryk3211.fizite.simulation;

import com.patryk3211.fizite.Fizite;
import com.patryk3211.fizite.simulation.gas.GasSimulator;
import com.patryk3211.fizite.simulation.gas.IGasCellProvider;
import com.patryk3211.fizite.simulation.physics.IPhysicsProvider;
import com.patryk3211.fizite.simulation.physics.PhysicsStorage;
import io.wispforest.owo.network.OwoNetChannel;
import io.wispforest.owo.network.ServerAccess;
import io.wispforest.owo.network.serialization.PacketBufSerializer;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;

import java.util.*;
import java.util.function.BiFunction;

public class Networking {
    public static OwoNetChannel CHANNEL;

    public record GasState(BlockPos position, int cellIndex, double Ek, double n, double V_x, double V_y, double V_z) { }

    public record ClientAddBlockEntity(BlockPos entityPosition, int[] rigidBodyIndices) { }
    public record ClientSyncState(int[] bodyIndices, Vec2f[] positions, Vec2f[] velocities, float[] angles, float[] angularVelocities) { }
    public record ClientGetSimulation(BlockPos[] entities, int[][] rigidBodyIndices) { }
    public record ClientSyncGasState(GasState[] states) { }
    public record ServerRequestBlockEntity(BlockPos entityPosition, Identifier world) { }
    public record ServerAddGasSyncPosition(BlockPos position, Identifier world) { }
    public record ServerRemoveGasSyncPosition(BlockPos position) { }

    private static class WaitListEntry {
        public static final int PHYSICS_FLAG = 1;
        public static final int GAS_FLAG = 2;

        public final Map<ServerPlayerEntity, Integer> players;
        public int cycles;

        public WaitListEntry(ServerPlayerEntity player, int flags) {
            this.players = new HashMap<>();
            this.players.put(player, flags);
            cycles = 0;
        }
    }
    private static final Map<BlockPos, WaitListEntry> waitingForEntity = new HashMap<>();

    private static void setWaitFlag(BlockPos pos, ServerPlayerEntity player, int flags) {
        var entry = waitingForEntity.get(pos);
        if(entry == null) {
            entry = new WaitListEntry(player, flags);
            waitingForEntity.put(pos, entry);
        } else {
            entry.players.compute(player, (key, value) -> value == null ? flags : value | flags);
        }
    }

    private static void clearWaitFlag(BlockPos pos, ServerPlayerEntity player, int flags) {
        var entry = waitingForEntity.get(pos);
        if(entry == null)
            return;
        var newVal = entry.players.computeIfPresent(player, (key, value) -> value & ~flags);
        if(newVal == null) {
            if(entry.players.isEmpty())
                waitingForEntity.remove(pos);
            return;
        }
        if(newVal == 0) {
            entry.players.remove(player);
            if(entry.players.isEmpty())
                waitingForEntity.remove(pos);
        }
    }

    private static void clearWaitFlag(BlockPos pos, int flags) {
        var entry = waitingForEntity.get(pos);
        if(entry == null)
            return;
        entry.players.keySet().removeIf(player -> {
            var newVal = entry.players.computeIfPresent(player, (key, value) -> value & ~flags);
            return newVal == null || newVal == 0;
        });
        if(entry.players.isEmpty())
            waitingForEntity.remove(pos);
    }

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
        PacketBufSerializer.register(GasState.class, (packetByteBuf, gasState) -> {
            packetByteBuf.writeBlockPos(gasState.position);
            packetByteBuf.writeInt(gasState.cellIndex);
            packetByteBuf.writeDouble(gasState.Ek);
            packetByteBuf.writeDouble(gasState.n);
            packetByteBuf.writeDouble(gasState.V_x);
            packetByteBuf.writeDouble(gasState.V_y);
            packetByteBuf.writeDouble(gasState.V_z);
        }, packetByteBuf -> {
            BlockPos pos = packetByteBuf.readBlockPos();
            int index = packetByteBuf.readInt();
            double Ek = packetByteBuf.readDouble();
            double n = packetByteBuf.readDouble();
            double V_x = packetByteBuf.readDouble();
            double V_y = packetByteBuf.readDouble();
            double V_z = packetByteBuf.readDouble();
            return new GasState(pos, index, Ek, n, V_x, V_y, V_z);
        });

        CHANNEL.registerClientboundDeferred(ClientAddBlockEntity.class);
        CHANNEL.registerClientboundDeferred(ClientSyncState.class);
        CHANNEL.registerClientboundDeferred(ClientGetSimulation.class);
        CHANNEL.registerClientboundDeferred(ClientSyncGasState.class);
        CHANNEL.registerServerbound(ServerRequestBlockEntity.class, Networking::handleRequestBlockEntity);
        CHANNEL.registerServerbound(ServerAddGasSyncPosition.class, Networking::handleAddGasSync);
        CHANNEL.registerServerbound(ServerRemoveGasSyncPosition.class, Networking::handleRemoveGasSync);
    }

    private static void handleRemoveGasSync(ServerRemoveGasSyncPosition packet, ServerAccess access) {
        GasSimulator.removeFromSync(access.player(), packet.position);
    }

    private static void handleAddGasSync(ServerAddGasSyncPosition packet, ServerAccess access) {
        final var server = access.runtime();
        final var world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, packet.world));
        if(world == null) {
            Fizite.LOGGER.error("Server doesn't have a world with key " + packet.world);
            return;
        }
        if(!world.isChunkLoaded(packet.position))
            return;
        final var entity = world.getBlockEntity(packet.position);
        if(entity == null) {
            setWaitFlag(packet.position, access.player(), WaitListEntry.GAS_FLAG);
//            Fizite.LOGGER.warn("Requested gas sync position doesn't have an entity");
            return;
        }
        if(entity instanceof final IGasCellProvider provider) {
            GasSimulator.addToSync(access.player(), packet.position, provider);
        } else {
            Fizite.LOGGER.warn("Requested gas sync position is not a gas cell provider");
        }
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
            setWaitFlag(packet.entityPosition, access.player(), WaitListEntry.PHYSICS_FLAG);
//            if(waitingForEntity.containsKey(packet.entityPosition)) {
//                waitingForEntity.get(packet.entityPosition).players.add(access.player());
//            } else {
//                waitingForEntity.put(packet.entityPosition, new WaitListEntry(access.player(), WaitListEntry.PHYSICS_FLAG));
//            }
            return;
        }

        final var bodies = provider.bodies();
        final int[] indices = new int[bodies.length];
        for(int i = 0; i < bodies.length; ++i) {
            indices[i] = bodies[i].index();
        }

        CHANNEL.serverHandle(access.player()).send(new ClientAddBlockEntity(packet.entityPosition, indices));
    }

    public static void physicsAdded(BlockPos pos, IPhysicsProvider provider) {
        final var entry = waitingForEntity.get(pos);
        if(entry == null)
            return;

        final var bodies = provider.bodies();
        final int[] indices = new int[bodies.length];
        for(int i = 0; i < bodies.length; ++i) {
            indices[i] = bodies[i].index();
        }

        final var packet = new ClientAddBlockEntity(pos, indices);
        entry.players.forEach((player, flags) -> {
            if((flags & WaitListEntry.PHYSICS_FLAG) != 0)
                CHANNEL.serverHandle(player).send(packet);
        });
        clearWaitFlag(pos, WaitListEntry.PHYSICS_FLAG);
    }

    public static void gasAdded(BlockPos pos, IGasCellProvider provider) {
        final var entry = waitingForEntity.get(pos);
        if(entry == null)
            return;

        entry.players.forEach((player, flags) -> {
            if((flags & WaitListEntry.GAS_FLAG) != 0)
                GasSimulator.addToSync(player, pos, provider);
        });
        clearWaitFlag(pos, WaitListEntry.GAS_FLAG);
    }

//    public static void entityAdded(BlockEntity entity) {
//        if(entity instanceof final IPhysicsProvider provider)
//            physicsAdded(entity.getPos(), provider);
//        if(entity instanceof final IGasCellProvider provider)
//            gasAdded(entity.getPos(), provider);
//    }

    public static void cleanupLists() {
        final Set<BlockPos> removePositions = new HashSet<>();
        waitingForEntity.forEach((pos, entry) -> {
            if(++entry.cycles >= 2) {
                removePositions.add(pos);
            }
        });
        removePositions.forEach(pos -> {
            final var entry = waitingForEntity.remove(pos);
            StringBuilder playerListBuilder = new StringBuilder();
            for(final var player : entry.players.keySet()) {
                playerListBuilder.append("'").append(player.getName()).append("', ");
            }
            final var playerList = playerListBuilder.delete(playerListBuilder.length() - 3, playerListBuilder.length() - 1).toString();
            Fizite.LOGGER.warn("Players (" + playerList + ") have requested data for block entity at " + pos + ", but the entity was never created on the server");
        });
    }
}
