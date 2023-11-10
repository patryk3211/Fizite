package com.patryk3211.fizite.simulation.physics.simulation.constraints;

import com.patryk3211.fizite.simulation.physics.simulation.RigidBody;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;

public class LockYConstraint extends Constraint {
    private final float position;

    public LockYConstraint(RigidBody body, float y) {
        super(2, new RigidBody[] { body });

        position = y;
    }

    @Override
    public void calculate(int row, DMatrixRMaj C, DMatrixSparseCSC J, DMatrixSparseCSC JDot) {
        final int column = bodies[0].index() * 3;
        final var state = bodies[0].getState();

        J.set(row, column + 1, 1);
        J.set(row + 1, column + 2, 1);

        JDot.set(row + 1, column + 2, 1);

        C.set(row, 0, state.position.y - position);
        C.set(row + 1, 0, state.positionA);
    }
}
