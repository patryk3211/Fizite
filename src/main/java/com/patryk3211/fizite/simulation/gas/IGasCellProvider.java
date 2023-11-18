package com.patryk3211.fizite.simulation.gas;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;

public interface IGasCellProvider {
    /**
     * Get the gas cell state container
     * @param dir Direction to query
     * @return The gas cell
     */
    GasCell getCell(@NotNull Direction dir);

    /**
     * Get the cross-section of the gas cell
     * @param dir Direction to query
     * @return The cross-section in meters
     */
    double getCrossSection(@NotNull Direction dir);

    /**
     * Get the flow constant of the gas cell
     * @param dir Direction to query
     * @return The flow constant in range of 0 to 1
     */
    double getFlowConstant(@NotNull Direction dir);

    /**
     * Get the gas state cell, used mostly for syncing client's data
     * @param i Index of the gas cell (range 0 to `getCellCount()`)
     * @return The gas cell
     */
    GasCell getCell(int i);

    /**
     * Get the total gas state cell count provided.
     * @return Cell count
     */
    default int getCellCount() {
        return 1;
    }

    /**
     * Utility function to try and create a gas boundary between the base
     * block entity and the block entity at a given direction.
     * @param base Base block entity to connect (assumes that it is a IGasCellProvider)
     * @param dir Direction to try and connect with
     * @return true if successfully connected, false if not
     */
    static boolean connect(@NotNull BlockEntity base, Direction dir) {
        assert base instanceof IGasCellProvider : "Expecting base block entity to be an actual gas cell provider";
        if(!(base.getWorld() instanceof final ServerWorld world)) {
            // Cannot create a boundary on the client side
            return false;
        }

        final IGasCellProvider baseProvider = (IGasCellProvider) base;
        if(baseProvider.getCell(dir) == null) {
            // This side doesn't provide a gas cell
            return false;
        }

        final Direction dirOpposite = dir.getOpposite();
        final BlockEntity neighborEntity = world.getBlockEntity(base.getPos().add(dir.getVector()));
        if(!(neighborEntity instanceof final IGasCellProvider provider)) {
            // Neighbor doesn't provide a gas cell provider
            return false;
        }
        final GasCell neighborCell = provider.getCell(dirOpposite);
        if(neighborCell == null) {
            // Neighbor doesn't provide a gas cell at this side
            return false;
        }

        // Create a new boundary and add it to the world boundaries
        final GasBoundary boundary = new GasBoundary(
                baseProvider.getCell(dir), neighborCell,
                baseProvider.getCrossSection(dir), provider.getCrossSection(dirOpposite),
                dir,
                Math.min(baseProvider.getFlowConstant(dir), provider.getFlowConstant(dirOpposite))
        );

        final GasStorage boundaries = GasStorage.get(world);
        boundaries.addBoundary(base.getPos(), dir, boundary);
        return true;
    }
}
