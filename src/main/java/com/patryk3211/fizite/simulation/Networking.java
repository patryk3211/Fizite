package com.patryk3211.fizite.simulation;

import com.patryk3211.fizite.Fizite;
import com.patryk3211.fizite.capability.CapabilitiesBlockEntity;
import com.patryk3211.fizite.simulation.gas.GasCapability;
import com.patryk3211.fizite.simulation.gas.GasCell;
import com.patryk3211.fizite.simulation.physics.IPhysicsProvider;
import com.patryk3211.fizite.simulation.physics.PhysicsCapability;
import com.patryk3211.fizite.simulation.physics.PhysicsStorage;
import io.wispforest.owo.network.OwoNetChannel;
import io.wispforest.owo.network.ServerAccess;
import io.wispforest.owo.network.serialization.PacketBufSerializer;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.world.World;

import java.util.*;

public class Networking {
    public static OwoNetChannel CHANNEL;

    public record GasState(long cellId, double Ek, double n, double V_x, double V_y, double V_z) { }

    public record ClientAddBlockEntity(BlockPos entityPosition, int[] rigidBodyIndices) { }
    public record ClientSyncState(int[] bodyIndices, Vec2f[] positions, Vec2f[] velocities, float[] angles, float[] angularVelocities) { }
    public record ClientGetSimulation(BlockPos[] entities, int[][] rigidBodyIndices) { }
    public record ClientSyncGasState(GasState[] states) { }
    public record ServerRequestBlockEntity(BlockPos entityPosition) { }
    public record ServerAddGasSyncPosition(BlockPos position) { }
    public record ServerRemoveGasSyncPosition(long id) { }

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
    private static final Map<RegistryKey<World>, Map<BlockPos, WaitListEntry>> waitingForEntity = new HashMap<>();

    private static final Map<ServerPlayerEntity, Map<Long, GasCell>> playerGasSync = new HashMap<>();

    private static void setWaitFlag(RegistryKey<World> world, BlockPos pos, ServerPlayerEntity player, int flags) {
        var worldEntry = waitingForEntity.computeIfAbsent(world, k -> new HashMap<>());
        var entry = worldEntry.get(pos);
        if(entry == null) {
            entry = new WaitListEntry(player, flags);
            worldEntry.put(pos, entry);
        } else {
            entry.players.compute(player, (key, value) -> value == null ? flags : value | flags);
        }
    }

//    private static void clearWaitFlag(RegistryKey<World> world, BlockPos pos, ServerPlayerEntity player, int flags) {
//        var worldEntry = waitingForEntity.get(world);
//        if(world == null)
//            return;
//        var entry = worldEntry.get(pos);
//        if(entry == null)
//            return;
//        var newVal = entry.players.computeIfPresent(player, (key, value) -> value & ~flags);
//        if(newVal == null) {
//            if(entry.players.isEmpty())
//                worldEntry.remove(pos);
//            return;
//        }
//        if(newVal == 0) {
//            entry.players.remove(player);
//            if(entry.players.isEmpty())
//                worldEntry.remove(pos);
//        }
//    }

    private static void clearWaitFlag(RegistryKey<World> world, BlockPos pos, int flags) {
        var worldEntry = waitingForEntity.get(world);
        if(worldEntry == null)
            return;
        var entry = worldEntry.get(pos);
        if(entry == null)
            return;
        entry.players.keySet().removeIf(player -> {
            var newVal = entry.players.computeIfPresent(player, (key, value) -> value & ~flags);
            return newVal == null || newVal == 0;
        });
        if(entry.players.isEmpty())
            worldEntry.remove(pos);
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
            packetByteBuf.writeLong(gasState.cellId);
            packetByteBuf.writeDouble(gasState.Ek);
            packetByteBuf.writeDouble(gasState.n);
            packetByteBuf.writeDouble(gasState.V_x);
            packetByteBuf.writeDouble(gasState.V_y);
            packetByteBuf.writeDouble(gasState.V_z);
        }, packetByteBuf -> {
            long id = packetByteBuf.readLong();
            double Ek = packetByteBuf.readDouble();
            double n = packetByteBuf.readDouble();
            double V_x = packetByteBuf.readDouble();
            double V_y = packetByteBuf.readDouble();
            double V_z = packetByteBuf.readDouble();
            return new GasState(id, Ek, n, V_x, V_y, V_z);
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
        var cells = playerGasSync.get(access.player());
        if(cells != null)
            cells.remove(packet.id);
    }

    private static void handleAddGasSync(ServerAddGasSyncPosition packet, ServerAccess access) {
        final var world = access.player().getServerWorld();
        if(!world.isChunkLoaded(packet.position))
            return;
        final var entity = world.getBlockEntity(packet.position);
        if(entity == null) {
            setWaitFlag(world.getRegistryKey(), packet.position, access.player(), WaitListEntry.GAS_FLAG);
            return;
        }
        if(!(entity instanceof final CapabilitiesBlockEntity capEntity)) {
            Fizite.LOGGER.warn("Requested gas sync position (" + packet.position.toString() + ") is not a capable block entity");
            return;
        }
        if(!capEntity.hasCapability(GasCapability.class)) {
            Fizite.LOGGER.warn("Requested gas sync position (" + packet.position.toString() + ") is not a gas capable entity");
            return;
        }

        final var playerCells = playerGasSync.computeIfAbsent(access.player(), k -> new HashMap<>());
        capEntity.getCapability(GasCapability.class).cells().forEach(cell -> playerCells.put(cell.getSyncId(), cell));
    }

    private static void handleRequestBlockEntity(ServerRequestBlockEntity packet, ServerAccess access) {
        final var world = access.player().getWorld(); //server.getWorld(RegistryKey.of(RegistryKeys.WORLD, packet.world));
//        if(world == null) {
//            Fizite.LOGGER.error("Server doesn't have a world with key " + packet.world);
//            return;
//        }
        final var capability = PhysicsStorage.get(world).getCapability(packet.entityPosition);
        if(capability == null) {
            setWaitFlag(world.getRegistryKey(), packet.entityPosition, access.player(), WaitListEntry.PHYSICS_FLAG);
            return;
        }

//        final var bodies = capability.bo;
        final int[] indices = new int[capability.bodyCount()];
        for(int i = 0; i < indices.length; ++i) {
            indices[i] = capability.body(i).index();
        }

        CHANNEL.serverHandle(access.player()).send(new ClientAddBlockEntity(packet.entityPosition, indices));
    }

    public static void physicsAdded(RegistryKey<World> world, BlockPos pos, PhysicsCapability capability) {
        final var worldEntry = waitingForEntity.get(world);
        if(worldEntry == null)
            return;
        final var entry = worldEntry.get(pos);
        if(entry == null)
            return;

        final int[] indices = new int[capability.bodyCount()];
        for(int i = 0; i < indices.length; ++i) {
            indices[i] = capability.body(i).index();
        }

        final var packet = new ClientAddBlockEntity(pos, indices);
        entry.players.forEach((player, flags) -> {
            if((flags & WaitListEntry.PHYSICS_FLAG) != 0)
                CHANNEL.serverHandle(player).send(packet);
        });
        clearWaitFlag(world, pos, WaitListEntry.PHYSICS_FLAG);
    }

    public static void gasAdded(RegistryKey<World> world, BlockPos pos, GasCapability capability) {
        final var worldEntry = waitingForEntity.get(world);
        if(worldEntry == null)
            return;
        final var entry = worldEntry.get(pos);
        if(entry == null)
            return;

        entry.players.forEach((player, flags) -> {
            if((flags & WaitListEntry.GAS_FLAG) != 0) {
                final var playerCells = playerGasSync.computeIfAbsent(player, k -> new HashMap<>());
                capability.cells().forEach(cell -> playerCells.put(cell.getSyncId(), cell));
            }
        });
        clearWaitFlag(world, pos, WaitListEntry.GAS_FLAG);
    }

    public static void gasRemoved(RegistryKey<World> world, GasCapability capability) {
        playerGasSync.forEach((player, cells) -> {
            if(!player.getServerWorld().getRegistryKey().equals(world))
                return;
            capability.cells().forEach(cell -> cells.remove(cell.getSyncId()));
        });
    }

    public static void clear() {
        playerGasSync.clear();
    }

    public static void sync() {
        cleanupLists();
        syncGas();
    }

    private static void syncGas() {
        playerGasSync.forEach((player, syncStates) -> {
            final List<Networking.GasState> packetData = new LinkedList<>();
            syncStates.forEach((id, cell) -> {
                final var momentum = cell.getMomentum();
                packetData.add(new GasState(id, cell.getMoleculeKineticEnergy(), cell.getTotalMoles(), momentum.x, momentum.y, momentum.z));
            });
            final var packet = new Networking.ClientSyncGasState(packetData.toArray(new Networking.GasState[0]));
            Networking.CHANNEL.serverHandle(player).send(packet);
        });
    }

    private static void cleanupLists() {
        final Map<RegistryKey<World>, Set<BlockPos>> removePositions = new HashMap<>();
        waitingForEntity.forEach((world, worldEntry) -> worldEntry.forEach((pos, entry) -> {
            if (++entry.cycles >= 2) {
                removePositions.compute(world, (key, oldValue) -> {
                    if(oldValue == null)
                        oldValue = new HashSet<>();
                    oldValue.add(pos);
                    return oldValue;
                });
            }
        }));
        removePositions.forEach((world, posSet) -> {
            final var worldEntry = waitingForEntity.get(world);
            for (BlockPos pos : posSet) {
                final var entry = worldEntry.remove(pos);
                StringBuilder playerListBuilder = new StringBuilder();
                for(final var player : entry.players.keySet()) {
                    playerListBuilder.append("'").append(player.getName()).append("', ");
                }
                final var playerList = playerListBuilder.delete(playerListBuilder.length() - 3, playerListBuilder.length() - 1).toString();
                Fizite.LOGGER.warn("Players (" + playerList + ") have requested data for block entity at " + pos + ", but the entity was never created on the server");
            }
        });
    }
}
