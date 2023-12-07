package com.patryk3211.fizite.simulation.physics;

import com.patryk3211.fizite.Fizite;
import com.patryk3211.fizite.simulation.Networking;
import com.patryk3211.fizite.simulation.physics.simulation.RigidBody;
import com.patryk3211.fizite.utility.Nbt;
import io.wispforest.owo.nbt.NbtKey;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.joml.Vector3d;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ServerPhysicsStorage extends PhysicsStorage {
    public static final String STORAGE_ID = Fizite.MOD_ID + ":physics_world";
    public static final Type<ServerPhysicsStorage> TYPE = new Type<>(ServerPhysicsStorage::new, ServerPhysicsStorage::new, null);

    /* Nbt stuff for saving the simulation */
    private record RigidBodyData(Vector3d position, Vector3d velocity) {
        public static RigidBodyData decode(NbtCompound data) {
            final var posNbt = data.getList("p", NbtElement.DOUBLE_TYPE);
            final var position = new Vector3d(posNbt.getDouble(0), posNbt.getDouble(1), posNbt.getDouble(2));
            final var velNbt = data.getList("v", NbtElement.DOUBLE_TYPE);
            final var velocity = new Vector3d(velNbt.getDouble(0), velNbt.getDouble(1), velNbt.getDouble(2));
            return new RigidBodyData(position, velocity);
        }

        public static NbtCompound encode(RigidBodyData rigidBodyData) {
            final var data = new NbtCompound();
            final var posNbt = new NbtList();
            posNbt.add(NbtDouble.of(rigidBodyData.position.x));
            posNbt.add(NbtDouble.of(rigidBodyData.position.y));
            posNbt.add(NbtDouble.of(rigidBodyData.position.z));
            data.put("p", posNbt);
            final var velNbt = new NbtList();
            velNbt.add(NbtDouble.of(rigidBodyData.velocity.x));
            velNbt.add(NbtDouble.of(rigidBodyData.velocity.y));
            velNbt.add(NbtDouble.of(rigidBodyData.velocity.z));
            data.put("v", velNbt);
            return data;
        }
    }
    private static final NbtKey.Type<RigidBodyData> NBT_TYPE_RIGID_BODY = NbtKey.Type.of(NbtElement.COMPOUND_TYPE, (nbt, key) -> {
        final var data = nbt.getCompound(key);
        return RigidBodyData.decode(data);
    }, (nbt, key, data) -> {
        final var compound = RigidBodyData.encode(data);
        nbt.put(key, compound);
    });
    private static final NbtKey<RigidBodyData> NBT_RIGID_BODY = new NbtKey<>("rb", NBT_TYPE_RIGID_BODY);
    private static final NbtKey<BlockPos> NBT_POSITION = new NbtKey<>("pos", Nbt.Type.BLOCK_POS);
    private static final NbtKey<NbtList> NBT_BODIES = new NbtKey.ListKey<>("bodies", NBT_TYPE_RIGID_BODY);
    private static final NbtKey<NbtList> NBT_POSITION_DATA = new NbtKey.ListKey<>("positionData", NbtKey.Type.COMPOUND);

    private static final Map<RegistryKey<World>, PhysicsStorage> simulations = new HashMap<>();

    private final Map<BlockPos, RigidBodyData[]> saveData;
    private RegistryKey<World> world;

    public ServerPhysicsStorage() {
        super();
        saveData = new HashMap<>();
    }

    public ServerPhysicsStorage(NbtCompound nbt) {
        this();
        readNbt(nbt);
    }

    @Override
    public void add(BlockPos position, PhysicsCapability capability) {
        super.add(position, capability);

        // Load the saved state
        final var data = saveData.get(position);

        // Add all rigid bodies and internal constraints to the simulation
        int index = 0;
        for(final var body : capability.bodies) {
            simulation.addRigidBody(body);
            if(data != null) {
                final var bState = capability.bodies[index].getState();
                final var sState = data[index];

                bState.position.x = sState.position.x;
                bState.position.y = sState.position.y;
                bState.positionA = sState.position.z;
                bState.velocity.x = sState.velocity.x;
                bState.velocity.y = sState.velocity.y;
                bState.velocityA = sState.velocity.z;
                ++index;
            }
        }
        if(capability.internalConstraints != null) {
            for (final var constraint : capability.internalConstraints) {
                simulation.addConstraint(constraint);
            }
        }

        // Notify clients of a new physical entity
        Networking.physicsAdded(world, position, capability);
    }

    private RigidBodyData encodeBody(RigidBody body) {
        final var state = body.getState();
        return new RigidBodyData(
                new Vector3d(state.position.x, state.position.y, state.positionA),
                new Vector3d(state.velocity.x, state.velocity.y, state.velocityA)
        );
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        final var positionData = new NbtList();
        dataMap.forEach((pos, data) -> {
            final var entry = new NbtCompound();
            entry.put(NBT_POSITION, pos);
            if(data.capability.bodies.length == 1) {
                // Encode a single body
                entry.put(NBT_RIGID_BODY, encodeBody(data.capability.bodies[0]));
            } else {
                // Encode a body list
                final var bodies = new NbtList();
                for(final var body : data.capability.bodies)
                    bodies.add(RigidBodyData.encode(encodeBody(body)));
                entry.put(NBT_BODIES, bodies);
            }
            positionData.add(entry);
        });
        nbt.put(NBT_POSITION_DATA, positionData);
        return nbt;
    }

    public void readNbt(NbtCompound nbt) {
        final var positionData = nbt.get(NBT_POSITION_DATA);
        positionData.forEach(element -> {
            if(!(element instanceof final NbtCompound entry))
                return;
            final var pos = entry.get(NBT_POSITION);
            RigidBodyData[] data;
            if(entry.has(NBT_RIGID_BODY)) {
                // This entry has a single body
                data = new RigidBodyData[] { entry.get(NBT_RIGID_BODY) };
            } else {
                // This entry has multiple bodies
                NbtList bodies = entry.get(NBT_BODIES);
                data = new RigidBodyData[bodies.size()];
                for(int i = 0; i < bodies.size(); ++i)
                    data[i] = RigidBodyData.decode(bodies.getCompound(i));
            }
            saveData.put(pos, data);
        });
    }

    @Override
    public void save(File file) {
        // Since even the smallest change need to be saved we just make
        // this storage class constantly dirty.
        markDirty();
        super.save(file);
    }

    public static void simulateAll() {
        simulations.forEach((key, sim) -> sim.simulation.simulate());
    }

    public static ServerPhysicsStorage addToWorld(ServerWorld world) {
        final var storage = world.getPersistentStateManager().getOrCreate(TYPE, STORAGE_ID);
        storage.world = world.getRegistryKey();
        simulations.put(world.getRegistryKey(), storage);
        return storage;
    }

    public static void onWorldTickEnd(ServerWorld world) {
//        get(world).dataMap.forEach((pos, data) -> {
////            if(data.provider instanceof final BlockEntity blockEntity) {
////                // Mark all block entities as dirty
////                blockEntity.markDirty();
////            }
//        });
    }

    public static void syncStates(MinecraftServer server) {
        final Map<RegistryKey<World>, List<ServerPlayerEntity>> playerDimensions = new HashMap<>();
        for(ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            final var key = player.getServerWorld().getRegistryKey();
            if(playerDimensions.get(key) == null) {
                final List<ServerPlayerEntity> list = new LinkedList<>();
                list.add(player);
                playerDimensions.put(key, list);
            } else {
                playerDimensions.get(key).add(player);
            }
        }
        playerDimensions.forEach((key, players) -> {
            final var sim = simulations.get(key);
            final var packet = sim.makeSyncPacket();
            Networking.CHANNEL.serverHandle(players).send(packet);
        });
    }

    public static void clearSimulations() {
        simulations.clear();
    }

    public static void recordFrames(RegistryKey<World> simWorld, int count, Runnable finishCallback) {
        simulations.get(simWorld).simulation.addOutputWriter(count, finishCallback);
    }
}
