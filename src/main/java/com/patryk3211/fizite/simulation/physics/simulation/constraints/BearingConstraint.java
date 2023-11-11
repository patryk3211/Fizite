package com.patryk3211.fizite.simulation.physics.simulation.constraints;

import com.patryk3211.fizite.simulation.physics.simulation.RigidBody;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.joml.Vector2f;

public class BearingConstraint extends Constraint {
    private final Vector2f anchor1;
    private final Vector2f anchor2;

    public BearingConstraint(RigidBody body1, RigidBody body2, float anchor1x, float anchor1y, float anchor2x, float anchor2y) {
        super(2, new RigidBody[] { body1, body2 });

        anchor1 = new Vector2f(anchor1x, anchor1y);
        anchor2 = new Vector2f(anchor2x, anchor2y);
    }

    protected BearingConstraint(int constraintCount, RigidBody body1, RigidBody body2, float anchor1x, float anchor1y, float anchor2x, float anchor2y) {
        super(constraintCount, new RigidBody[] { body1, body2 });

        anchor1 = new Vector2f(anchor1x, anchor1y);
        anchor2 = new Vector2f(anchor2x, anchor2y);
    }

    @Override
    public void calculate(int row, DMatrixRMaj C, DMatrixSparseCSC J, DMatrixSparseCSC JDot) {
        final int column1 = bodies[0].index() * 3;
        final int column2 = bodies[1].index() * 3;

        final var state1 = bodies[0].getState();
        final var state2 = bodies[1].getState();

        final var b1Cos = Math.cos(state1.positionA);
        final var b1Sin = Math.sin(state1.positionA);

        final var b2Cos = Math.cos(state2.positionA);
        final var b2Sin = Math.sin(state2.positionA);

        final var world1X = anchor1.x * b1Cos - anchor1.y * b1Sin + state1.position.x;
        final var world1Y = anchor1.y * b1Cos + anchor1.x * b1Sin + state1.position.y;
        final var world2X = anchor2.x * b2Cos - anchor2.y * b2Sin + state2.position.x;
        final var world2Y = anchor2.y * b2Cos + anchor2.x * b2Sin + state2.position.y;

        // Changes to x based on body1 positions
        J.unsafe_set(row, column1, 1);
        J.unsafe_set(row, column1 + 2, -b1Sin * anchor1.x - b1Cos * anchor1.y);

        // Changes to y based on body1 positions
        J.unsafe_set(row + 1, column1 + 1, 1);
        J.unsafe_set(row + 1, column1 + 2, b1Cos * anchor1.x - b1Sin * anchor1.y);

        // Changes to x based on body2 positions
        J.unsafe_set(row, column2, -1);
        J.unsafe_set(row, column2 + 2, b2Sin * anchor2.x + b2Cos * anchor2.y);

        // Changes to y based on body2 positions
        J.unsafe_set(row + 1, column2 + 1, -1);
        J.unsafe_set(row + 1, column2 + 2, -b2Cos * anchor2.x + b2Sin * anchor2.y);

        // Changes to velocity based on body1 velocity
        JDot.unsafe_set(row, column1 + 2, -b1Cos * state1.velocityA * anchor1.x + b1Sin * state1.velocityA * anchor1.y);
        JDot.unsafe_set(row + 1, column1 + 2, -b1Sin * state1.velocityA * anchor1.x - b1Cos * state1.velocityA * anchor1.y);

        // Changes to velocity based on body2 velocity
        JDot.unsafe_set(row, column2 + 2, b2Cos * state2.velocityA * anchor2.x - b2Sin * state2.velocityA * anchor2.y);
        JDot.unsafe_set(row + 1, column2 + 2, b2Sin * state2.velocityA * anchor2.x + b2Cos * state2.velocityA * anchor2.y);

        // Additional position stabilization constraint
        final double C1 = world1X - world2X;
        final double C2 = world1Y - world2Y;

        C.unsafe_set(row, 0, C1);
        C.unsafe_set(row + 1, 0, C2);
    }

    @Override
    public void restMatrix(int row, DMatrixRMaj C, DMatrixSparseCSC J) {
        C.unsafe_set(row, 0, anchor1.x - anchor2.x);
        C.unsafe_set(row + 1, 0, anchor1.y - anchor2.y);

        final int column1 = bodies[0].index() * 3;
        final int column2 = bodies[1].index() * 3;
        J.unsafe_set(row, column1, 1);
        J.unsafe_set(row, column1 + 2, -anchor1.y);
        J.unsafe_set(row + 1, column1 + 1, 1);
        J.unsafe_set(row + 1, column1 + 2, anchor1.x);
        J.unsafe_set(row, column2, -1);
        J.unsafe_set(row, column2 + 2, anchor2.y);
        J.unsafe_set(row + 1, column2 + 1, -1);
        J.unsafe_set(row + 1, column2 + 2, -anchor2.x);
    }
}
