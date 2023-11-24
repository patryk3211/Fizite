package com.patryk3211.fizite.simulation.physics.simulation.constraints;

import com.patryk3211.fizite.simulation.physics.simulation.RigidBody;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;

public class WeldConstraint extends BearingConstraint {
    public WeldConstraint(RigidBody body1, RigidBody body2, float anchor1x, float anchor1y, float anchor2x, float anchor2y) {
        super(3, new RigidBody[] { body1, body2 }, anchor1x, anchor1y, anchor2x, anchor2y);
    }

    protected WeldConstraint(int constraintCount, RigidBody[] bodies, float anchor1x, float anchor1y, float anchor2x, float anchor2y) {
        super(constraintCount, bodies, anchor1x, anchor1y, anchor2x, anchor2y);
        assert bodies.length >= 2;
        assert constraintCount >= 3;
    }

    @Override
    public void calculate(int row, DMatrixRMaj C, DMatrixSparseCSC J, DMatrixSparseCSC JDot) {
        final int column1 = bodies[0].index() * 3;
        final int column2 = bodies[1].index() * 3;

        final var state1 = bodies[0].getState();
        final var state2 = bodies[1].getState();

        super.calculate(row, C, J, JDot);

        // Apply additional constraints to keep the two bodies inline
        J.unsafe_set(row + 2, column1 + 2, 1);
        J.unsafe_set(row + 2, column2 + 2, -1);
        C.unsafe_set(row + 2, 0, state1.positionA - state2.positionA);
    }

    @Override
    public void setBodyPosition(int index) {
        super.setBodyPosition(index);

        System.err.println("Warning! setBodyPosition not fully implemented for WeldConstraint");
    }
}
