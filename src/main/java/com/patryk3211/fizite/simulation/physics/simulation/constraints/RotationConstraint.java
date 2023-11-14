package com.patryk3211.fizite.simulation.physics.simulation.constraints;

import com.patryk3211.fizite.simulation.physics.simulation.RigidBody;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;

public class RotationConstraint extends Constraint {
    public RotationConstraint(RigidBody body1, RigidBody body2) {
        super(1, new RigidBody[] { body1, body2 });
    }

    @Override
    public void calculate(int row, DMatrixRMaj C, DMatrixSparseCSC J, DMatrixSparseCSC JDot) {
        final var column1 = bodies[0].index() * 3;
        final var column2 = bodies[1].index() * 3;

        final var a1 = bodies[0].getState().positionA;
        final var a2 = bodies[1].getState().positionA;

        J.set(row, column1 + 2, 1);
        J.set(row, column2 + 2, -1);

        C.set(row, a1 - a2);
    }

    @Override
    public void restMatrix(int row, DMatrixRMaj C, DMatrixSparseCSC J) {
        final var column1 = bodies[0].index() * 3;
        final var column2 = bodies[1].index() * 3;

        J.set(row, column1 + 2, 1);
        J.set(row, column2 + 2, -1);
    }
}
