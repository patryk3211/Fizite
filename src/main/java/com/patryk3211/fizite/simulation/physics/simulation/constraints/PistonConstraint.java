package com.patryk3211.fizite.simulation.physics.simulation.constraints;

import com.patryk3211.fizite.simulation.physics.simulation.RigidBody;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;

public class PistonConstraint extends Constraint {
    private final float position;
    private final float maxPosition;

    public PistonConstraint(RigidBody body, float y, float maxX) {
        super(3, new RigidBody[] { body });

        position = y;
        maxPosition = maxX;
    }

    protected PistonConstraint(int constraintCount, RigidBody[] bodies, float y, float maxX) {
        super(constraintCount, bodies);
        assert bodies.length >= 1;
        assert constraintCount >= 3;

        position = y;
        maxPosition = maxX;
    }

    @Override
    public void calculate(int row, DMatrixRMaj C, DMatrixSparseCSC J, DMatrixSparseCSC JDot) {
        final int column = bodies[0].index() * 3;
        final var state = bodies[0].getState();

        J.unsafe_set(row, column + 1, 1);
        J.unsafe_set(row + 1, column + 2, 1);

        JDot.unsafe_set(row + 1, column + 2, 1);

//        final var C2 = Math.max(state.position.x - maxPosition, 0);
//        if(C2 != 0) {
////            JDot.unsafe_set(row + 2, column, 1);
//            J.unsafe_set(row + 2, column, 0.1);
//            C.unsafe_set(row + 2, 0, C2);
//        } else {
//            JDot.unsafe_set(row + 2, column, 0);
//            J.unsafe_set(row + 2, column, 0);
//            C.unsafe_set(row + 2, 0, 0);
//        }

        C.unsafe_set(row, 0, state.position.y - position);
        C.unsafe_set(row + 1, 0, state.positionA);
    }

    @Override
    public void restMatrix(int row, DMatrixRMaj C, DMatrixSparseCSC J) {
        final int column = bodies[0].index() * 3;
        final var state = bodies[0].getState();

        J.unsafe_set(row, column + 1, 1);
        J.unsafe_set(row + 1, column + 2, 1);

        C.unsafe_set(row, 0, state.position.y - position);
        C.unsafe_set(row + 1, 0, state.positionA);
    }

    @Override
    public void setBodyPosition(int index) {
        assert index == 0;
        final var state = bodies[0].getState();
        state.position.y = position;
        state.positionA = 0;
    }
}
