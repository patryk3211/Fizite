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

    public abstract void calculate(int index, DMatrixRMaj C, DMatrixSparseCSC J, DMatrixSparseCSC JDot);

    public int internalConstraintCount() {
        return constraintCount;
    }
}
