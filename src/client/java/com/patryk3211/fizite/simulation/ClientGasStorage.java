package com.patryk3211.fizite.simulation;

import com.patryk3211.fizite.Fizite;
import com.patryk3211.fizite.simulation.gas.GasStorage;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

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
    public void removeBoundaries(BlockPos pos) {
        super.removeBoundaries(pos);
        ClientNetworking.removeFromGasSync(pos);
    }

    public static void onDisconnect(ClientPlayNetworkHandler networkHandler, MinecraftClient client) {
        Fizite.LOGGER.info("Clearing client gas simulation");
        gas.boundaries.clear();
        gas.collectedBoundaries.clear();
    }

    public static void onBlockEntityUnload(BlockEntity entity, ClientWorld world) {
        GasStorage.clientStorage.removeBoundaries(entity.getPos());
    }
}
