package com.patryk3211.fizite.simulation.gas;

import com.patryk3211.fizite.Fizite;
import com.patryk3211.fizite.simulation.Networking;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ServerGasStorage extends GasStorage {
    public static final String STORAGE_ID = Fizite.MOD_ID + ":gas_boundary_storage";
    public static final Type<GasStorage> TYPE = new Type<>(ServerGasStorage::new, (nbt) -> new ServerGasStorage(), null);

    private RegistryKey<World> world;
    private long nextGasCellId;

    public ServerGasStorage() {
        nextGasCellId = 1;
    }

    public static ServerGasStorage addToWorld(ServerWorld world) {
        final var storage = new ServerGasStorage();
        storage.world = world.getRegistryKey();
        world.getPersistentStateManager().set(STORAGE_ID, storage);
        return storage;
    }

    @Override
    protected void sidedAdd(BlockPos pos, GasCapability capability) {
        capability.cells().forEach(cell -> cell.setSyncId(nextGasCellId++));
        Networking.gasAdded(world, pos, capability);
    }

    @Override
    protected void sidedRemove(BlockPos pos, GasCapability capability) {
        Networking.gasRemoved(world, capability);
    }
}
