package com.patryk3211.fizite.simulation.physics.simulation.constraints;

import com.patryk3211.fizite.simulation.physics.simulation.RigidBody;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.joml.Vector2d;

public abstract class Constraint {
    protected final RigidBody[] bodies;
    private final int constraintCount;

    protected Constraint(int constraintCount, RigidBody[] bodies) {
        this.bodies = bodies.clone();
        this.constraintCount = constraintCount;
    }

    /**
     * Calculates constraint matrices used for constraint solving
     * @param index Index of constraint, first row allocated in matrices for this constraint
     * @param C Position constraint matrix
     * @param J Jacobian matrix
     * @param JDot Jacobian dot matrix
     */
    public abstract void calculate(int index, DMatrixRMaj C, DMatrixSparseCSC J, DMatrixSparseCSC JDot);

    /**
     * Calculates just the position matrix
     * Used for determining position 0 of bodies
     * @param index Index of constraint, first row allocated in matrices for this constraint
     * @param C Position constraint matrix
     * @param J Jacobian matrix
     */
    public abstract void restMatrix(int index, DMatrixRMaj C, DMatrixSparseCSC J);

    public int internalConstraintCount() {
        return constraintCount;
    }
}