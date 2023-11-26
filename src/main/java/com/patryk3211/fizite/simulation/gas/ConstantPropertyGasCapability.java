package com.patryk3211.fizite.simulation.gas;

import net.minecraft.util.math.Direction;

import java.util.List;

public class ConstantPropertyGasCapability extends GasCapability {
    private final GasCell cell;
    private final float crossSection;
    private final float flowConstant;

    public ConstantPropertyGasCapability(float volume, float crossSection, float flowConstant) {
        cell = new GasCell(volume);
        this.crossSection = crossSection;
        this.flowConstant = flowConstant;
    }

    public ConstantPropertyGasCapability(float volume, float crossSection, float flowConstant, Direction... directions) {
        super(directions);
        cell = new GasCell(volume);
        this.crossSection = crossSection;
        this.flowConstant = flowConstant;
    }

    @Override
    public List<GasCell> cells() {
        return List.of(cell);
    }

    @Override
    public GasCell cell(Direction dir) {
        return cell;
    }

    @Override
    public double crossSection(Direction dir) {
        return crossSection;
    }

    @Override
    public double flowConstant(Direction dir) {
        return flowConstant;
    }

    @Override
    public int deriveSuperclasses() {
        return 1;
    }
}
