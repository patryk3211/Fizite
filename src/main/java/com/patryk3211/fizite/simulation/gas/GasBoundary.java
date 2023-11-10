package com.patryk3211.fizite.simulation.gas;

import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;

public class GasBoundary {
    private final GasCell cell1;
    private final GasCell cell2;
    private final double crossSection1;
    private final double crossSection2;
    private final Vector3d directionVector;
    private double flowConstant;

    public GasBoundary(@NotNull GasCell cell1, @NotNull GasCell cell2, double crossSection1, double crossSection2, Direction direction, double flowConstant) {
        this.directionVector = new Vector3d();
        if(direction.getDirection() == Direction.AxisDirection.POSITIVE) {
            this.cell1 = cell1;
            this.cell2 = cell2;
            this.crossSection1 = crossSection1;
            this.crossSection2 = crossSection2;
            direction.getUnitVector().get(directionVector);
        } else {
            this.cell1 = cell2;
            this.cell2 = cell1;
            this.crossSection1 = crossSection2;
            this.crossSection2 = crossSection1;
            direction.getUnitVector().negate().get(directionVector);
        }
        this.flowConstant = flowConstant;
    }

    public void setFlowConstant(double flowConstant) {
        this.flowConstant = flowConstant;
    }

    public void simulate(double dT) {
        GasSimulator.flow(dT, flowConstant, cell1, cell2, crossSection1, crossSection2, directionVector);
    }

    public Vector3d getDirectionVector() {
        return directionVector;
    }
}
