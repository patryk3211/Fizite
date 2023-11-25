package com.patryk3211.fizite.simulation.gas;

import com.patryk3211.fizite.capability.ConnectableCapability;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;
import java.util.Objects;

public abstract class GasCapability extends ConnectableCapability<GasCapability> {
    private final GasBoundary[] boundaries;

    public GasCapability() {
        super("gas", GasCapability.class);
        boundaries = new GasBoundary[6];
    }

    @Override
    public void onLoad() {
        final var world = Objects.requireNonNull(entity.getWorld());
        super.onLoad();
        GasStorage.get(world).add(entity.getPos(), this);
    }

    @Override
    public void onUnload() {
        final var world = Objects.requireNonNull(entity.getWorld());
        super.onUnload();
        GasStorage.get(world).remove(entity.getPos());
    }

    @Override
    public void readNbt(NbtElement tag) {
        assert tag instanceof NbtCompound;
        final var compound = (NbtCompound) tag;
        super.readNbt(compound.get("mask"));

        final var list = compound.getList("states", NbtElement.COMPOUND_TYPE);
        final var cellList = cells();
        for(int i = 0; i < list.size(); ++i)
            cellList.get(i).deserialize(list.getCompound(i));
    }

    @Override
    public NbtElement writeNbt() {
        final var compound = new NbtCompound();
        compound.put("mask", super.writeNbt());

        final var list = new NbtList();
        final var cellList = cells();
        for(GasCell gasCell : cellList)
            list.add(gasCell.serialize());
        compound.put("states", list);
        return compound;
    }

    @Override
    public void connect(Direction dir, GasCapability connectTo) {
        Objects.requireNonNull(entity.getWorld());

        final var oDir = dir.getOpposite();
        final var boundary = new GasBoundary(
                cell(dir), connectTo.cell(oDir),
                crossSection(dir), connectTo.crossSection(oDir),
                dir, Math.min(flowConstant(dir), connectTo.flowConstant(oDir)));
        GasStorage.get(entity.getWorld()).add(boundary);
        boundaries[dir.getId()] = boundary;
        connectTo.boundaries[oDir.getId()] = boundary;
    }

    @Override
    public void disconnect(Direction dir, GasCapability disconnectFrom) {
        Objects.requireNonNull(entity.getWorld());

        final var oDir = dir.getOpposite();
        GasStorage.get(entity.getWorld()).remove(boundaries[dir.getId()]);
        boundaries[dir.getId()] = null;
        disconnectFrom.boundaries[oDir.getId()] = null;
    }

    @Override
    public GasCapability getNeighbor(BlockPos pos) {
        Objects.requireNonNull(entity.getWorld());
        return GasStorage.get(entity.getWorld()).get(pos);
    }

    /**
     * Get a collection of all gas cell states
     * @return Ordered list of all cell states
     */
    public abstract List<GasCell> cells();

    /**
     * Get the gas cell state container
     * @param dir Direction to query
     * @return The gas cell
     */
    public abstract GasCell cell(Direction dir);

    /**
     * Get the cross-section of the gas cell
     * @param dir Direction to query
     * @return The cross-section in meters
     */
    public abstract double crossSection(Direction dir);

    /**
     * Get the flow constant of the gas cell
     * @param dir Direction to query
     * @return The flow constant in range of 0 to 1
     */
    public abstract double flowConstant(Direction dir);

    @Override
    public void debugOutput(List<String> output) {
        int index = 0;
        for (GasCell cell : cells()) {
            output.add(String.format("[%d] Pressure = %.1f Pa", index, cell.pressure()));
            output.add(String.format("[%d] Temperature = %.2f K", index, cell.temperature()));
            ++index;
        }
    }
}
