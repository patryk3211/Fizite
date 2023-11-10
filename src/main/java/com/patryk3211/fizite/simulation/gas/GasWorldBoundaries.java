package com.patryk3211.fizite.simulation.gas;

import com.patryk3211.fizite.Fizite;
import com.patryk3211.fizite.utility.DirectionUtilities;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.PersistentState;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

public class GasWorldBoundaries extends PersistentState {
    public static final String STORAGE_ID = Fizite.MOD_ID + ":gas_boundary_storage";
    public static final Type<GasWorldBoundaries> TYPE = new Type<>(GasWorldBoundaries::new, (nbt) -> new GasWorldBoundaries(), null);

    public static class PositionData {
        public final GasBoundary[] directions;
        public final IGasCellProvider provider;

        public PositionData(IGasCellProvider provider) {
            this.provider = provider;
            directions = new GasBoundary[3];
        }

        public boolean isEmpty() {
            return directions[0] == null && directions[1] == null && directions[2] == null;
        }
    }

    private final Map<BlockPos, PositionData> boundaries;
    private final List<GasBoundary> collectedBoundaries;

    public GasWorldBoundaries() {
        boundaries = new HashMap<>();
        collectedBoundaries = new LinkedList<>();
    }

    public void addBlockEntity(BlockEntity entity) {
        assert entity instanceof IGasCellProvider : "Expecting block entity to be an actual gas cell provider";
        final IGasCellProvider baseProvider = (IGasCellProvider) entity;

        PositionData entry = new PositionData(baseProvider);
        boundaries.put(entity.getPos(), entry);

        for(final var dir : Direction.values()) {
            if (baseProvider.getCell(dir) == null) {
                // This side doesn't provide a gas cell
                continue;
            }

            final var entryAt = boundaries.get(entity.getPos().offset(dir));
            if(entryAt == null)
                // This position doesn't have a gas cell provider
                continue;
            final var neighbor = entryAt.provider;
            final var oDir = dir.getOpposite();
            final var neighborCell = neighbor.getCell(oDir);
            if(neighborCell == null)
                // This side doesn't have a gas cell
                continue;

            // Create a new boundary and add it to the world boundaries
            final var boundary = new GasBoundary(
                    baseProvider.getCell(dir), neighborCell,
                    baseProvider.getCrossSection(dir), neighbor.getCrossSection(oDir),
                    dir,
                    Math.min(baseProvider.getFlowConstant(dir), neighbor.getFlowConstant(oDir))
            );

            addBoundary(entity.getPos(), dir, boundary);
        }
    }

    @NotNull
    public static GasWorldBoundaries getBoundaries(@NotNull ServerWorld world) {
        final GasWorldBoundaries boundaries = world.getPersistentStateManager().get(TYPE, STORAGE_ID);
        assert boundaries != null : "World gas boundary storage class was not initialized for this world";
        return boundaries;
    }

    public void addBoundary(BlockPos pos, @NotNull Direction dir, @NotNull GasBoundary boundary) {
        if(dir.getDirection() == Direction.AxisDirection.NEGATIVE) {
            // Add the negative offset to get the actual position of the boundary
            pos = pos.add(dir.getVector());
            // Get the opposite (positive) direction
            dir = dir.getOpposite();
        }
        final int index = DirectionUtilities.positiveDirectionIndex(dir);

        var boundariesAt = boundaries.get(pos);
        if(boundariesAt == null) {
            throw new IllegalStateException("Given direction doesn't have a gas provider");
//            boundariesAt = new PositionData();
//            boundaries.put(pos, boundariesAt);
        }
        if(boundariesAt.directions[index] != null) {
            Fizite.LOGGER.warn("Overriding GasBoundary at {}, direction {}", pos, dir);
            collectedBoundaries.remove(boundariesAt.directions[index]);
        }
        boundariesAt.directions[index] = boundary;
        collectedBoundaries.add(boundary);
    }

    public GasBoundary getBoundary(BlockPos pos, @NotNull Direction dir) {
        if(dir.getDirection() == Direction.AxisDirection.NEGATIVE) {
            // Add the negative offset to get the actual position of the boundary
            pos = pos.add(dir.getVector());
            // Get the opposite (positive) direction
            dir = dir.getOpposite();
        }

        final var boundariesAt = boundaries.get(pos);
        if(boundariesAt == null)
            return null;

        int index = DirectionUtilities.positiveDirectionIndex(dir);
        return boundariesAt.directions[index];
    }

    public void removeBoundary(BlockPos pos, @NotNull Direction dir) {
        if(dir.getDirection() == Direction.AxisDirection.NEGATIVE) {
            // Add the negative offset to get the actual position of the boundary
            pos = pos.add(dir.getVector());
            // Get the opposite (positive) direction
            dir = dir.getOpposite();
        }

        final var boundariesAt = boundaries.get(pos);
        if(boundariesAt == null)
            return;

        final int index = DirectionUtilities.positiveDirectionIndex(dir);
        collectedBoundaries.remove(boundariesAt.directions[index]);
        boundariesAt.directions[index] = null;
//        if(boundariesAt.isEmpty())
//            boundaries.remove(pos);
    }

    public void removeBoundaries(BlockPos pos) {
        for(final var dir : Direction.values()) {
            removeBoundary(pos, dir);
        }
        boundaries.remove(pos);
    }

    public Collection<GasBoundary> getAllBoundaries() {
        return collectedBoundaries;
    }

    @Override
    public void save(File file) {
        // Do nothing...
        // We don't want to save boundary data, it will get recomputed
        // on world start
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        return null;
    }
}
