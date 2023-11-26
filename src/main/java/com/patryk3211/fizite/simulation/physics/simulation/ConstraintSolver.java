package com.patryk3211.fizite.simulation.physics.simulation;

import com.patryk3211.fizite.simulation.physics.simulation.constraints.Constraint;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.sparse.csc.CommonOps_DSCC;

import java.util.List;
import java.util.function.BiConsumer;

public class ConstraintSolver {
    private static final double K_D = 10; // Applied to C_dot
    private static final double K_S = 10; // Applied to C

    private DMatrixRMaj qDot;
    private DMatrixSparseCSC J;
    private DMatrixSparseCSC JDot;
    private DMatrixRMaj W;
    private DMatrixRMaj C;
    private DMatrixRMaj CDot;
    private DMatrixRMaj bodyLock;

    private DMatrixSparseCSC JT;

    private DMatrixRMaj extForce;
    private DMatrixRMaj cForce;

    private DMatrixRMaj right;
    private DMatrixRMaj left;
    private DMatrixRMaj lambda;

    private DMatrixSparseCSC sparseReg;
    private DMatrixRMaj denseReg1CC;
    private DMatrixRMaj denseReg2CC;
    private DMatrixRMaj denseReg1BC;

    private DMatrixRMaj pMatrix;
    private DMatrixRMaj rMatrix;

    private final List<RigidBody> rigidBodies;
    private final List<Constraint> constraints;

    // Profiling data
    public long rightPrepareTime;
    public long solveTime;
    public int iterationCount;

    public ConstraintSolver(List<RigidBody> bodies, List<Constraint> constraints) {
        this.rigidBodies = bodies;
        this.constraints = constraints;
    }

    public void updateMassMatrix(int index, float mass) {
        W.set(index * 3, 0, 1.0 / mass);
        W.set(index * 3 + 1, 0, 1.0 / mass);
        W.set(index * 3 + 2, 0, 1.0 / mass);
    }

    public void resizeMatrices(int totalConstraintCount) {
        final var bodyCount = rigidBodies.size();

        // Rebuild the inverse mass matrix
        W = new DMatrixRMaj(bodyCount * 3, 1);
        for(final var body : rigidBodies) {
            if(body == null)
                continue;
            final var row = body.index() * 3;
            W.set(row, 0, 1.0 / body.getMass());
            W.set(row + 1, 0, 1.0 / body.getMass());
            // TODO: Change this to moment of inertia
            W.set(row + 2, 0, 1.0 / body.getMass());
        }
        bodyLock = new DMatrixRMaj(bodyCount * 3, 1);
        for(final var body : rigidBodies) {
            if(body == null)
                continue;
            final var row = body.index() * 3;
            final var value = body.isInitialized() ? 0 : 1;
            bodyLock.set(row, 0, value);
            bodyLock.set(row + 1, 0, value);
            bodyLock.set(row + 2, 0, value);
        }

        // Create matrices with new sizes
        qDot = new DMatrixRMaj(bodyCount * 3, 1);
        J = new DMatrixSparseCSC(totalConstraintCount, bodyCount * 3);
        JDot = new DMatrixSparseCSC(totalConstraintCount, bodyCount * 3);

        extForce = new DMatrixRMaj(bodyCount * 3, 1);
        cForce = new DMatrixRMaj(bodyCount * 3, 1);
        C = new DMatrixRMaj(totalConstraintCount, 1);
        CDot = new DMatrixRMaj(totalConstraintCount, 1);

        JT = new DMatrixSparseCSC(bodyCount * 3, totalConstraintCount);

        right = new DMatrixRMaj(totalConstraintCount, 1);
        left = new DMatrixRMaj(totalConstraintCount, 1);
        lambda = new DMatrixRMaj(totalConstraintCount, 1);

        sparseReg = new DMatrixSparseCSC(totalConstraintCount, bodyCount * 3);
        denseReg1CC = new DMatrixRMaj(totalConstraintCount, 1);
        denseReg2CC = new DMatrixRMaj(totalConstraintCount, 1);

        denseReg1BC = new DMatrixRMaj(bodyCount * 3, 1);

        pMatrix = new DMatrixRMaj(totalConstraintCount, 1);
        rMatrix = new DMatrixRMaj(totalConstraintCount, 1);
    }

    public void clear() {
        W = null;
        qDot = null;
        J = null;
        JDot = null;

        extForce = null;
        cForce = null;
        C = null;
        CDot = null;

        JT = null;

        right = null;
        left = null;
        lambda = null;

        sparseReg = null;
        denseReg1CC = null;
        denseReg2CC = null;

        denseReg1BC = null;

        pMatrix = null;
        rMatrix = null;

        bodyLock = null;
    }

    private static final double MAX_ERROR = 1E-4;
    private static final double MIN_ERROR = 1E-5;
    private static final int MAX_ITERATIONS = 1024;

    private boolean checkError(DMatrixRMaj x, DMatrixRMaj right) {
        for(int i = 0; i < x.numRows; ++i) {
            final double error = x.get(i, 0);
            final double target = right.get(i, 0);
            if(Math.abs(error) > Math.max(Math.abs(MAX_ERROR * target), MIN_ERROR)) {
                return false;
            }
        }
        return true;
    }

    // Calculates the left side of the equation:
    // J * W * J_T * x = ...
    private void makeLeft(DMatrixRMaj x, DMatrixRMaj output) {
        CommonOps_DSCC.mult(JT, x, denseReg1BC);
        CommonOps_DDRM.elementMult(denseReg1BC, W);
        CommonOps_DSCC.mult(J, denseReg1BC, output);
    }

    private void makeLeftMassless(DMatrixRMaj x, DMatrixRMaj output) {
        CommonOps_DSCC.mult(JT, x, denseReg1BC);
//        CommonOps_DDRM.elementMult(denseReg1BC, bodyLock);
        CommonOps_DSCC.mult(J, denseReg1BC, output);
    }

    private static double squareMag(DMatrixRMaj matrix) {
        double mag = 0;
        for(int i = 0; i < matrix.numRows; ++i) {
            final var v = matrix.unsafe_get(i, 0);
            mag += v * v;
        }
        return mag;
    }

    private boolean solve(BiConsumer<DMatrixRMaj, DMatrixRMaj> leftCalculator) {
        leftCalculator.accept(lambda, left);

        CommonOps_DDRM.subtract(right, left, rMatrix);
        if(checkError(rMatrix, right)) {
            iterationCount = 0;
            return false;
        }

        pMatrix.setTo(rMatrix);
        for(int i = 0; i < MAX_ITERATIONS; ++i) {
            iterationCount = i + 1;
            leftCalculator.accept(pMatrix, left);

            final double rkMag = squareMag(rMatrix);
            final double dot = CommonOps_DDRM.dot(pMatrix, left);
            final double alpha = dot == 0 ? 1 : rkMag / dot;
            for(int j = 0; j < lambda.numRows; ++j) {
                lambda.plus(j, pMatrix.unsafe_get(j, 0) * alpha);
                rMatrix.plus(j, left.unsafe_get(j, 0) * -alpha);
            }

            if(checkError(rMatrix, right)) {
                return false;
            }

            final double rk1Mag = squareMag(rMatrix);
            final double beta = rk1Mag / rkMag;
            for(int j = 0; j < rMatrix.numRows; ++j) {
                pMatrix.unsafe_set(j, 0, pMatrix.unsafe_get(j, 0) * beta + rMatrix.unsafe_get(j, 0));
            }
        }

        return true;
    }

    public void step() {
        // Update q dot matrix
        for(final var body : rigidBodies) {
            if(body == null)
                continue;
            final var row = body.index() * 3;
            final var state = body.getState();

            qDot.unsafe_set(row, 0, state.velocity.x);
            qDot.unsafe_set(row + 1, 0, state.velocity.y);
            qDot.unsafe_set(row + 2, 0, state.velocityA);

            extForce.unsafe_set(row, 0, state.extForce.x);
            extForce.unsafe_set(row + 1, 0, state.extForce.y);
            extForce.unsafe_set(row + 2, 0, state.extForceA);
        }

        // Update constraint matrices
        int constraintIndex = 0;
        for(final var constraint : constraints) {
            constraint.calculate(constraintIndex, C, J, JDot);
            constraintIndex += constraint.internalConstraintCount();
        }

        // Equation:
        // J * W * J_T * lambda = - J_dot * q_dot - J * W * F_ext - k_s * C - k_d * C_dot

        rightPrepareTime = -System.nanoTime();
        // sparseReg = -J_dot
        CommonOps_DSCC.changeSign(JDot, sparseReg);
        // reg1CC = -J_dot * q_dot
        CommonOps_DSCC.mult(sparseReg, qDot, denseReg1CC);
        // reg1BC = W * F_ext
        CommonOps_DDRM.elementMult(W, extForce, denseReg1BC);
        // reg2CC = J * W * F_ext
        CommonOps_DSCC.mult(J, denseReg1BC, denseReg2CC);
        // right = -J_dot * q_dot - J * W * F_ext
        CommonOps_DDRM.subtract(denseReg1CC, denseReg2CC, right);
        // C_dot = J * q_dot
        CommonOps_DSCC.mult(J, qDot, CDot);
        // C_dot = C_dot * k_d
        CommonOps_DDRM.scale(K_D, CDot);
        // C = C * k_s
        CommonOps_DDRM.scale(K_S, C);
        // right = -J_dot * q_dot - J * W * F_ext - k_s * C
        CommonOps_DDRM.subtractEquals(right, C);
        // right = -J_dot * q_dot - J * W * F_ext - k_s * C - k_d * C_dot
        CommonOps_DDRM.subtractEquals(right, CDot);

        // Transpose Jacobian matrix
        CommonOps_DSCC.transpose(J, JT, null);
        rightPrepareTime += System.nanoTime();

        solveTime = -System.nanoTime();
        if(solve(this::makeLeft)) {
            System.out.println("Failed to solve");
        }
        CommonOps_DSCC.mult(JT, lambda, cForce);
        solveTime += System.nanoTime();

        for(final var body : rigidBodies) {
            if(body == null)
                continue;
            final var row = body.index() * 3;

            final var fX = cForce.get(row, 0);
            final var fY = cForce.get(row + 1, 0);
            final var fA = cForce.get(row + 2, 0);

            final var bodyState = body.getState();
            bodyState.acceleration.x = (float) ((fX + bodyState.extForce.x) / body.getMass());
            bodyState.acceleration.y = (float) ((fY + bodyState.extForce.y) / body.getMass());
            bodyState.accelerationA = (float) ((fA + bodyState.extForceA) / body.getMass());

            bodyState.cForce.x = fX;
            bodyState.cForce.y = fY;
            bodyState.cForceA = fA;
        }
    }
}
