package com.patryk3211.fizite.simulation.physics.simulation;

import com.patryk3211.fizite.simulation.physics.simulation.constraints.Constraint;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;

public class PhysicsWorld {
    // Default parameters
    public static final double DELTA_TIME = 1.0f / 20.0f;
    public static final int STEPS = 100;
//    public static final double STEP_TIME = DELTA_TIME / STEPS;

    private final double deltaTime;
    private final int steps;
    private final double stepTime;

    private final PhysicsSolver physicsSolver;
    private final ConstraintSolver constraintSolver;
    private final PhysicalSystem system;

    private final List<RigidBody> rigidBodies;
    private final LinkedList<Integer> freeIndices;
    private final List<Constraint> constraints;
    private int totalConstraintCount;
    private boolean parametersChanged;
    private int bodyCount;

    // Profiling info
    public long constraintSolveTime;
    public long forceApplyTime;
    public long physicsStepTime;
    public long physicsSolveTime;
    public long restPositionSolveTime;

    public PhysicsWorld() {
        this(DELTA_TIME, STEPS);
    }

    public PhysicsWorld(double deltaTime, int steps) {
        physicsSolver = new PhysicsSolver();
        system = new PhysicalSystem();

        rigidBodies = new ArrayList<>();
        freeIndices = new LinkedList<>();
        constraints = new LinkedList<>();

        constraintSolver = new ConstraintSolver(rigidBodies, constraints);
        totalConstraintCount = 0;
        parametersChanged = true;
        bodyCount = 0;

        this.deltaTime = deltaTime;
        this.steps = steps;
        this.stepTime = deltaTime / steps;
    }

    public void addRigidBody(RigidBody body) {
        int index;
        if(freeIndices.isEmpty()) {
            index = rigidBodies.size();
            rigidBodies.add(body);
            system.resize(index + 1);
        } else {
            index = freeIndices.removeFirst();
            rigidBodies.set(index, body);
        }
        system.setState(index, body.getState());
        body.assign(index, this);
        ++bodyCount;
        parametersChanged = true;
    }

    public void removeRigidBody(RigidBody body) {
        rigidBodies.set(body.index(), null);
        freeIndices.addLast(body.index());
        system.setState(body.index(), null);
        --bodyCount;
        parametersChanged = true;
    }

    public void addConstraint(Constraint constraint) {
        constraints.add(constraint);
        totalConstraintCount += constraint.internalConstraintCount();
        parametersChanged = true;
    }

    public void removeConstraint(Constraint constraint) {
        if(constraints.remove(constraint)) {
            totalConstraintCount -= constraint.internalConstraintCount();
            parametersChanged = true;
        }
    }

    public void simulate() {
        if(bodyCount == 0)
            return;

        if(parametersChanged) {
            constraintSolver.resizeMatrices(totalConstraintCount);
            parametersChanged = false;
            restPositions();
        }

        physicsSolver.start(stepTime, system);

        for(int i = 0; i < steps * 4; ++i) {
            // Physics part I
            physicsStepTime = -System.nanoTime();
            physicsSolver.step();
            physicsStepTime += System.nanoTime();

            // Calculate constraint forces
            constraintSolveTime = -System.nanoTime();
            if(totalConstraintCount != 0)
                constraintSolver.step();
            constraintSolveTime += System.nanoTime();

            // Physics part II
            physicsSolveTime = -System.nanoTime();
            physicsSolver.solve();
            physicsSolveTime += System.nanoTime();
        }

        debugWrite();
    }

    public void restPositions() {
        if(parametersChanged) {
            constraintSolver.resizeMatrices(totalConstraintCount);
            parametersChanged = false;
        }

        restPositionSolveTime = -System.nanoTime();
        constraintSolver.restPositions();
        restPositionSolveTime += System.nanoTime();
    }

    public Collection<Constraint> constraints() {
        return constraints;
    }

    public double totalKineticEnergy() {
        double totalEnergy = 0;
        for(final var body : rigidBodies) {
            totalEnergy += body.kineticEnergy();
        }
        return totalEnergy;
    }

    public void updateMass(int rigidBodyIndex, float mass) {
        if(!parametersChanged) {
            // If parameters have changed then we have to rebuild
            // the entire mass matrix anyway
            constraintSolver.updateMassMatrix(rigidBodyIndex, mass);
        }
    }

    // --------======== Debugging stuff ========--------

    private OutputStreamWriter writer = null;
    private int writerFrameCount = 0;
    private Runnable finishCallback = null;

    private static void dumpBody(OutputStreamWriter fileWriter, RigidBody body) throws IOException {
        final var state = body.getState();
        fileWriter.write("Body" + body.index() + "\t");
        fileWriter.write(state.position.x + "," + state.position.y + "," + state.positionA + "\t");
        fileWriter.write(state.velocity.x + "," + state.velocity.y + "," + state.velocityA + "\n");
    }

    public void addOutputWriter(int frameCount, Runnable finishCallback) {
        try {
            writer = new OutputStreamWriter(new FileOutputStream("output.phys"));
            writerFrameCount = frameCount;
            this.finishCallback = finishCallback;
        } catch (FileNotFoundException e) {
            e.printStackTrace(System.err);
        }
    }

    private void debugWrite() {
        try {
            if(writer != null) {
                for (final var body : rigidBodies) {
                    dumpBody(writer, body);
                }
                writer.write("%\n");
                writer.flush();
                if (writerFrameCount-- <= 0) {
                    writer.close();
                    writer = null;
                    if(finishCallback != null)
                        finishCallback.run();
                    finishCallback = null;
                }
            }
        } catch(IOException e) {
            e.printStackTrace(System.err);
        }
    }
}
