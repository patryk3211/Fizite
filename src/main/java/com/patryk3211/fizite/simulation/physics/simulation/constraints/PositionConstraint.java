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

        J.set(row, column, 1);
        J.set(row + 1, column + 1, 1);

        C.set(row, 0, state.position.x - position.x);
        C.set(row + 1, 0, state.position.y - position.y);
    }
}
