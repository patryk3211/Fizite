package com.patryk3211.fizite.simulation.physics.simulation;

import com.patryk3211.fizite.simulation.Simulator;
import com.patryk3211.fizite.simulation.physics.SimulationTuner;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.Constraint;
import org.joml.Vector2d;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;

public class PhysicsWorld {
    // Default parameters
    public static final double DELTA_TIME = 1.0f / 20.0f;
    public static final int STEPS = 100;

    private final double deltaTime;
    private int steps;
    private double stepTime;

    private final PhysicsSolver physicsSolver;
    private final ConstraintSolver constraintSolver;
    private final PhysicalSystem system;
    public SimulationTuner tuner;

    private final List<RigidBody> rigidBodies;
    private final LinkedList<Integer> freeIndices;
    private final List<Constraint> constraints;
    private int totalConstraintCount;
    private boolean parametersChanged;
    private int bodyCount;

    private final List<IPhysicsStepHandler> stepHandlers;
    private final List<IForceGenerator> forceGenerators;

    // Profiling info
    public long startTime;
    public long[] constraintSolveTime;
    public long[] forceGeneratorTime;
    public long[] physicsStepTime;
    public long[] physicsSolveTime;
    public long[] stepHandlersTime;
    public int[] iterationCount;
    public long totalTime;

    public PhysicsWorld() {
        this(DELTA_TIME, STEPS);
    }

    public PhysicsWorld(double deltaTime, int steps) {
        physicsSolver = new PhysicsSolver();
        system = new PhysicalSystem();

        rigidBodies = new ArrayList<>();
        freeIndices = new LinkedList<>();
        constraints = new LinkedList<>();
        stepHandlers = new LinkedList<>();
        forceGenerators = new LinkedList<>();

        constraintSolver = new ConstraintSolver(rigidBodies, constraints);
        totalConstraintCount = 0;
        parametersChanged = true;
        bodyCount = 0;

        this.deltaTime = deltaTime;
        this.steps = steps;
        this.stepTime = deltaTime / steps;

        constraintSolveTime = new long[steps * 4];
        forceGeneratorTime = new long[steps * 4];
        physicsStepTime = new long[steps * 4];
        physicsSolveTime = new long[steps * 4];
        iterationCount = new int[steps * 4];
        stepHandlersTime = new long[steps];
    }

    public void adjustSteps(int newSteps) {
        this.steps = newSteps;
        this.stepTime = deltaTime / newSteps;

        // Adjust debug data size
        constraintSolveTime = new long[steps * 4];
        forceGeneratorTime = new long[steps * 4];
        physicsStepTime = new long[steps * 4];
        physicsSolveTime = new long[steps * 4];
        iterationCount = new int[steps * 4];
        stepHandlersTime = new long[steps];
    }

    public void clear() {
        system.resize(0);
        physicsSolver.resize(0);
        constraintSolver.clear();
        rigidBodies.clear();
        freeIndices.clear();
        constraints.clear();
        forceGenerators.clear();
        stepHandlers.clear();
        totalConstraintCount = 0;
        bodyCount = 0;
        parametersChanged = true;
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

    public void addRigidBody(RigidBody body, int index) {
        if(freeIndices.contains(index)) {
            freeIndices.remove((Object) index);
            rigidBodies.set(index, body);
        } else if(rigidBodies.size() == index) {
            rigidBodies.add(body);
            system.resize(index + 1);
        } else if(rigidBodies.size() < index) {
            while(rigidBodies.size() < index)
                rigidBodies.add(null);
            rigidBodies.add(body);
            system.resize(index + 1);
        } else if(rigidBodies.get(index) == null) {
            rigidBodies.set(index, body);
        } else {
            throw new IllegalArgumentException("Given index is already in use");
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

    public void addStepHandler(IPhysicsStepHandler handler) {
        stepHandlers.add(handler);
    }

    public void removeStepHandler(IPhysicsStepHandler handler) {
        stepHandlers.remove(handler);
    }

    public void addForceGenerator(IForceGenerator generator) {
        forceGenerators.add(generator);
    }

    public void removeForceGenerator(IForceGenerator generator) {
        forceGenerators.remove(generator);
    }

    public void simulate() {
        totalTime = -System.nanoTime();

        startTime = -System.nanoTime();
        if(parametersChanged) {
            constraintSolver.resizeMatrices(totalConstraintCount);
            parametersChanged = false;
        }

        physicsSolver.start(stepTime, system);
        startTime += System.nanoTime();

        int debugFrame = 0;
        for(int i = 0; i < steps; ++i) {
            if(bodyCount != 0) {
                do {
                    // Physics part I
                    physicsStepTime[debugFrame] = -System.nanoTime();
                    physicsSolver.step();
                    physicsStepTime[debugFrame] += System.nanoTime();

                    // Generate external forces
                    forceGeneratorTime[debugFrame] = -System.nanoTime();
                    forceGenerators.forEach(g -> g.apply(stepTime));
                    forceGeneratorTime[debugFrame] += System.nanoTime();

                    // Calculate constraint forces
                    constraintSolveTime[debugFrame] = -System.nanoTime();
                    if (totalConstraintCount != 0)
                        constraintSolver.step();
                    constraintSolveTime[debugFrame] += System.nanoTime();
                    iterationCount[debugFrame] = constraintSolver.iterationCount;

                    // Physics part II
                    physicsSolveTime[debugFrame] = -System.nanoTime();
                    physicsSolver.solve();
                    physicsSolveTime[debugFrame] += System.nanoTime();
                    ++debugFrame;
                } while (!physicsSolver.stepFinished());
            }

            // Post-step
            stepHandlersTime[i] = -System.nanoTime();
            stepHandlers.forEach(h -> h.onStepEnd(stepTime));
            stepHandlersTime[i] += System.nanoTime();
        }
        totalTime += System.nanoTime();

//        for(final var body : rigidBodies) {
//            if(body == null)
//                continue;
//            if(body.externalForceReset) {
//                final var state = body.getState();
//                if(state.extForceA != 0) System.out.println("Reset angular force to 0");
//                state.extForce.x = 0;
//                state.extForce.y = 0;
//                state.extForceA = 0;
//            }
//        }

        if(tuner != null)
            tuner.tune(this::adjustSteps);

        debugWrite();
    }

    public List<RigidBody> bodies() {
        return rigidBodies;
    }

    public Collection<Constraint> constraints() {
        return constraints;
    }

    public PhysicalSystem system() {
        return system;
    }

    public int bodyCount() {
        return bodyCount;
    }

    public double totalKineticEnergy() {
        double totalEnergy = 0;
        for(final var body : rigidBodies) {
            totalEnergy += body.kineticEnergy();
        }
        return totalEnergy;
    }

    public double maxVelocity() {
        return physicsSolver.maxVelocity();
    }

    public double maxAngularVelocity() {
        return physicsSolver.maxAngularVelocity();
    }

    public void updateMass(int rigidBodyIndex, float invMass1, float invMass2, float invMass3) {
        if(!parametersChanged) {
            // If parameters have changed then we have to rebuild
            // the entire mass matrix anyway
            constraintSolver.updateMassMatrix(rigidBodyIndex, invMass1, invMass2, invMass3);
        }
    }

    public int stepCount() {
        return steps;
    }

    public void fireStepHandler() {
        stepHandlers.forEach(handler -> handler.onStepEnd(0));
    }

    // --------======== Debugging stuff ========--------

    private OutputStreamWriter writer = null;
    private int writerFrameCount = 0;
    private Runnable finishCallback = null;

    private static void dumpBody(OutputStreamWriter writer, RigidBody body) throws IOException {
        final var state = body.getState();
        writer.write("Body" + body.getMarker() + body.index() + "\t");
        writer.write(state.position.x + "," + state.position.y + "," + state.positionA + "\t");
        writer.write(state.velocity.x + "," + state.velocity.y + "," + state.velocityA + "\n");
        dumpVector(writer, body.index() + 100, state.position, state.extForce);
        dumpVector(writer, body.index() + 200, state.position, state.cForce);
    }

    private static void dumpVector(OutputStreamWriter writer, int index, Vector2d origin, Vector2d direction) throws IOException {
        writer.write("Vector" + index + "\t");
        writer.write(origin.x + "," + origin.y + "\t");
        writer.write(direction.x + "," + direction.y + "\n");
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
                    if(body == null)
                        continue;
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
