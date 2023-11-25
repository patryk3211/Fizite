package com.patryk3211.fizite.simulation.gas;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
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

//    public void addGasProviderProcessSides(RegistryKey<World> world, BlockPos pos, IGasCellProvider provider) {
//        PositionData entry = new PositionData(provider);
//        boundaries.put(pos, entry);
//
//        for (final var dir : Direction.values()) {
//            if (provider.getCell(dir) == null) {
//                // This side doesn't provide a gas cell
//                continue;
//            }
//
//            final var entryAt = boundaries.get(pos.offset(dir));
//            if (entryAt == null)
//                // This position doesn't have a gas cell provider
//                continue;
//            final var neighbor = entryAt.provider;
//            final var oDir = dir.getOpposite();
//            final var neighborCell = neighbor.getCell(oDir);
//            if (neighborCell == null)
//                // This side doesn't have a gas cell
//                continue;
//
//            // Create a new boundary and add it to the world boundaries
//            final var boundary = new GasBoundary(
//                    provider.getCell(dir), neighborCell,
//                    provider.getCrossSection(dir), neighbor.getCrossSection(oDir),
//                    dir,
//                    Math.min(provider.getFlowConstant(dir), neighbor.getFlowConstant(oDir))
//            );
//
//            addBoundary(pos, dir, boundary);
//        }
//
//        // Might add some clients to sync lists
//        Networking.gasAdded(world, pos, provider);
//    }
//
//    public void addGasProvider(RegistryKey<World> world, BlockPos pos, IGasCellProvider provider) {
//        PositionData entry = new PositionData(provider);
//        boundaries.put(pos, entry);
//        Networking.gasAdded(world, pos, provider);
//    }

//    public void addBoundary(BlockPos pos, @NotNull Direction dir, @NotNull GasBoundary boundary) {
//        if (dir.getDirection() == Direction.AxisDirection.NEGATIVE) {
//            // Add the negative offset to get the actual position of the boundary
//            pos = pos.add(dir.getVector());
//            // Get the opposite (positive) direction
//            dir = dir.getOpposite();
//        }
//        final int index = DirectionUtilities.positiveDirectionIndex(dir);
//
//        var boundariesAt = boundaries.get(pos);
//        if (boundariesAt == null) {
//            throw new IllegalStateException("Given direction doesn't have a gas provider");
//        }
//        if (boundariesAt.directions[index] != null) {
//            Fizite.LOGGER.warn("Overriding GasBoundary at {}, direction {}", pos, dir);
//            collectedBoundaries.remove(boundariesAt.directions[index]);
//        }
//        boundariesAt.directions[index] = boundary;
//        collectedBoundaries.add(boundary);
//    }
//
//    public GasBoundary getBoundary(BlockPos pos, @NotNull Direction dir) {
//        if (dir.getDirection() == Direction.AxisDirection.NEGATIVE) {
//            // Add the negative offset to get the actual position of the boundary
//            pos = pos.add(dir.getVector());
//            // Get the opposite (positive) direction
//            dir = dir.getOpposite();
//        }
//
//        final var boundariesAt = boundaries.get(pos);
//        if (boundariesAt == null)
//            return null;
//
//        int index = DirectionUtilities.positiveDirectionIndex(dir);
//        return boundariesAt.directions[index];
//    }
//
//    public void removeBoundary(BlockPos pos, @NotNull Direction dir) {
//        if (dir.getDirection() == Direction.AxisDirection.NEGATIVE) {
//            // Add the negative offset to get the actual position of the boundary
//            pos = pos.add(dir.getVector());
//            // Get the opposite (positive) direction
//            dir = dir.getOpposite();
//        }
//
//        final var boundariesAt = boundaries.get(pos);
//        if (boundariesAt == null)
//            return;
//
//        final int index = DirectionUtilities.positiveDirectionIndex(dir);
//        collectedBoundaries.remove(boundariesAt.directions[index]);
//        boundariesAt.directions[index] = null;
//    }
//
//    public void clearPosition(BlockPos pos) {
//        for (final var dir : Direction.values()) {
//            removeBoundary(pos, dir);
//        }
//        boundaries.remove(pos);
//    }
//
//    public void removeGasProvider(RegistryKey<World> world, BlockPos pos) {
//        boundaries.remove(pos);
//        GasSimulator.removeFromSync(world, pos);
//    }

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
