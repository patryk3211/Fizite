package com.patryk3211.fizite.simulation;

import com.patryk3211.fizite.Fizite;
import com.patryk3211.fizite.simulation.gas.GasStorage;
import com.patryk3211.fizite.simulation.gas.IGasCellProvider;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ClientGasStorage extends GasStorage {
    private static ClientGasStorage gas;

    public ClientGasStorage() {
        GasStorage.clientStorage = this;
        gas = this;
    }

    public static ClientGasStorage get() {
        return gas;
    }

    @Override
    public void addBlockEntity(BlockEntity entity) {
        assert entity.getWorld() != null;
        super.addBlockEntity(entity);
        ClientNetworking.addToGasSync(entity.getPos(), entity.getWorld().getRegistryKey());
    }

    @Override
    public void addGasProviderProcessSides(RegistryKey<World> world, BlockPos pos, IGasCellProvider provider) {
        super.addGasProviderProcessSides(world, pos, provider);
        ClientNetworking.addToGasSync(pos, world);
    }

    @Override
    public void clearPosition(BlockPos pos) {
        super.clearPosition(pos);
        ClientNetworking.removeFromGasSync(pos);
    }

    @Override
    public void addGasProvider(RegistryKey<World> world, BlockPos pos, IGasCellProvider provider) {
        super.addGasProvider(world, pos, provider);
        ClientNetworking.addToGasSync(pos, world);
    }

    @Override
    public void removeGasProvider(RegistryKey<World> world, BlockPos pos) {
        super.removeGasProvider(world, pos);
        ClientNetworking.removeFromGasSync(pos);
    }

    public static void onDisconnect() {
        Fizite.LOGGER.info("Clearing client gas simulation");
        gas.boundaries.clear();
        gas.collectedBoundaries.clear();
    }

    public static void onBlockEntityUnload(BlockEntity entity, ClientWorld world) {
        GasStorage.clientStorage.clearPosition(entity.getPos());
    }
}
