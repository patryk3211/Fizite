package com.patryk3211.fizite.simulation.physics.simulation.constraints;

import com.patryk3211.fizite.simulation.physics.simulation.RigidBody;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.joml.Vector2f;

//public class LineConstraint extends Constraint {
//    private final Vector2f anchor;
//    private final Vector2f p0;
//    private final Vector2f normal;
//
//    public LineConstraint(RigidBody body, float anchorX, float anchorY, float p0X, float p0Y, float dirX, float dirY) {
//        super(1, new RigidBody[] { body });
//
//        anchor = new Vector2f(anchorX, anchorY);
//        p0 = new Vector2f(p0X, p0Y);
//        normal = new Vector2f(dirX, dirY).normalize();
//    }
//
//    @Override
//    public void calculate(int row, DMatrixRMaj C, DMatrixSparseCSC J, DMatrixSparseCSC JDot) {
//        final int column = bodies[0].index() * 3;
//        final var state = bodies[0].getState();
//
//        final var thetaCos = Math.cos(state.positionA);
//        final var thetaSin = Math.sin(state.positionA);
//
//        final var worldX = state.position.x + thetaCos * anchor.x - thetaSin * anchor.y;
//        final var worldY = state.position.y + thetaSin * anchor.x + thetaCos * anchor.y;
//
//        final var deltaX = worldX - p0.x;
//        final var deltaY = worldY - p0.y;
//
//        final var C1 = deltaX * -normal.y + deltaY * normal.x;
//        C.set(row, 0, C1);
//
//        J.set(row, column, -normal.y);
//        J.set(row, column + 1, normal.x);
//        J.set(row, column + 2,
//                (-thetaSin * anchor.x - thetaCos * anchor.y) * -normal.y +
//                (thetaCos * anchor.x - thetaSin * anchor.y) * normal.x);
//
////        JDot.set(row, column + 2, );
//    }
//}
