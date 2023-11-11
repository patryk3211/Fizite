package com.patryk3211.fizite.simulation.physics.simulation.constraints;

import com.patryk3211.fizite.simulation.physics.simulation.RigidBody;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.joml.Vector2f;

public class PositionConstraint extends Constraint {
    private final Vector2f position;

    public PositionConstraint(RigidBody body, float x, float y) {
        super(2, new RigidBody[] { body });

        position = new Vector2f(x, y);
    }

    @Override
    public void calculate(int row, DMatrixRMaj C, DMatrixSparseCSC J, DMatrixSparseCSC JDot) {
        final int column = bodies[0].index() * 3;
        final var state = bodies[0].getState();

        J.unsafe_set(row, column, 1);
        J.unsafe_set(row + 1, column + 1, 1);

        C.unsafe_set(row, 0, state.position.x - position.x);
        C.unsafe_set(row + 1, 0, state.position.y - position.y);
    }

    @Override
    public void restMatrix(int row, DMatrixRMaj C, DMatrixSparseCSC J) {
        C.unsafe_set(row, 0, -position.x);
        C.unsafe_set(row + 1, 0, -position.y);

        final int column = bodies[0].index() * 3;
        J.unsafe_set(row, column, 1);
        J.unsafe_set(row + 1, column + 1, 1);
    }
}
