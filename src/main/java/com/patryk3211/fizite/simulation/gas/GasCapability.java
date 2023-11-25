package com.patryk3211.fizite.simulation.gas;

import com.patryk3211.fizite.capability.Capability;
import com.patryk3211.fizite.capability.ConnectableCapability;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GasCapability extends ConnectableCapability<GasCapability> implements IGasCellProvider {
    private final GasCell gasCell;

    public GasCapability(float volume) {
        super("gas", GasCapability.class);

        gasCell = new GasCell(volume);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        final var world = entity.getWorld();
        assert world != null;
        GasStorage.get(world).addGasProviderProcessSides(world.getRegistryKey(), entity.getPos(), this);//.addBlockEntity(entity);
//        GasStorage.get(world).addGasProvider(world.getRegistryKey(), entity.getPos(), this);
//        if(!world.isClient)
//            Networking.gasAdded(world.getRegistryKey(), entity.getPos(), this);
    }

    @Override
    public void onUnload() {
        super.onUnload();
        final var world = entity.getWorld();
        assert world != null;
        GasStorage.get(world).clearPosition(entity.getPos());
//        GasStorage.get(world).removeGasProvider(world.getRegistryKey(), entity.getPos());
    }

    @Override
    public void readNbt(NbtElement tag) {
        assert tag instanceof NbtCompound;
        final var compound = (NbtCompound) tag;
        super.readNbt(compound.get("mask"));
        gasCell.deserialize(compound.getCompound("state"));
    }

    @Override
    public NbtElement writeNbt() {
        final var compound = new NbtCompound();
        compound.put("mask", super.writeNbt());
        compound.put("state", gasCell.serialize());
        return compound;
    }

    @Override
    public boolean connect(Direction dir, GasCapability connectTo) {
//        IGasCellProvider.
//        GasStorage.get(entity.getWorld()).addBoundary();
        return false;
    }

    @Override
    public void disconnect(Direction dir, GasCapability disconnectFrom) {

    }

    @Override
    public GasCell getCell(@NotNull Direction dir) {
        return gasCell;//canConnect(dir) ? gasCell : null;
    }

    @Override
    public double getCrossSection(@NotNull Direction dir) {
        return 1;
    }

    @Override
    public double getFlowConstant(@NotNull Direction dir) {
        return 1;
    }

    @Override
    public GasCell getCell(int i) {
        return gasCell;
    }

    @Override
    public void debugOutput(List<String> output) {
        output.add(String.format("Pressure = %.1f Pa", gasCell.pressure()));
        output.add(String.format("Temperature = %.2f K", gasCell.temperature()));
    }
}
