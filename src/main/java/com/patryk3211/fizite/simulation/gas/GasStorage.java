package com.patryk3211.fizite.simulation.gas;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

public abstract class GasStorage extends PersistentState {
    protected static GasStorage clientStorage = null;

    protected final Map<BlockPos, GasCapability> registeredCapabilities;
    protected final List<GasBoundary> collectedBoundaries;

    public GasStorage() {
        registeredCapabilities = new HashMap<>();
        collectedBoundaries = new LinkedList<>();
    }

    @NotNull
    public static GasStorage get(@NotNull World world) {
        if(world.isClient) {
            return clientStorage;
        } else {
            final GasStorage boundaries = ((ServerWorld) world).getPersistentStateManager().get(ServerGasStorage.TYPE, ServerGasStorage.STORAGE_ID);
            assert boundaries != null : "World gas boundary storage class was not initialized for this world";
            return boundaries;
        }
    }

    protected abstract void sidedAdd(BlockPos pos, GasCapability capability);
    public final void add(BlockPos pos, GasCapability capability) {
        registeredCapabilities.put(pos, capability);
        sidedAdd(pos, capability);
    }

    protected abstract void sidedRemove(BlockPos pos, GasCapability capability);
    public final void remove(BlockPos pos) {
        final var cap = registeredCapabilities.remove(pos);
        if(cap != null)
            sidedRemove(pos, cap);
    }

    public GasCapability get(BlockPos pos) {
        return registeredCapabilities.get(pos);
    }

    public void add(GasBoundary boundary) {
        collectedBoundaries.add(boundary);
    }

    public void remove(GasBoundary boundary) {
        collectedBoundaries.remove(boundary);
    }

    public void simulate(double deltaTime) {
        for(GasBoundary boundary : collectedBoundaries) {
            boundary.simulate(deltaTime);
        }
    }

    @Override
    public void save(File file) {
        // Do nothing...
        // We don't want to save boundary data,
        // it will get recomputed on world start
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        return null;
    }
}
