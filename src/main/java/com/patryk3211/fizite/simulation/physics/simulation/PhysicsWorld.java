package com.patryk3211.fizite.simulation.physics.simulation;

import com.patryk3211.fizite.simulation.Simulator;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.Constraint;
import org.joml.Vector2d;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;

public class PhysicsWorld {
    // Default parameters
    public static final double DELTA_TIME = Simulator.TICK_RATE; //1.0f / 20.0f;
    public static final int STEPS = 150;

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

    private final List<IPhysicsStepHandler> stepHandlers;
    private final List<IForceGenerator> forceGenerators;

    // Profiling info
    public long startTime;
    public long constraintSolveTime;
    public long physicsStepTime;
    public long physicsSolveTime;
    public long restPositionSolveTime;
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

        this.steps = steps;
        this.stepTime = deltaTime / steps;
    }

    public void clear() {
        system.resize(0);
        physicsSolver.resize(0);
        constraintSolver.clear();
        rigidBodies.clear();
        freeIndices.clear();
        constraints.clear();
    }

    public int addRigidBody(RigidBody body) {
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
        return index;
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
        if(bodyCount == 0)
            return;
        totalTime = -System.nanoTime();

        startTime = -System.nanoTime();
        if(parametersChanged) {
            constraintSolver.resizeMatrices(totalConstraintCount);
            parametersChanged = false;
            restPositions();
        }

        physicsSolver.start(stepTime, system);
        startTime += System.nanoTime();

        for(int i = 0; i < steps; ++i) {
            do {
                // Physics part I
                physicsStepTime = -System.nanoTime();
                physicsSolver.step();
                physicsStepTime += System.nanoTime();

                forceGenerators.forEach(g -> g.apply(stepTime));
//            if(i % 4 == 0) {
//                for (IPhysicsStepHandler handler : stepHandlers)
//                    handler.onStep(stepTime);
//            }

                // Calculate constraint forces
                constraintSolveTime = -System.nanoTime();
                if (totalConstraintCount != 0)
                    constraintSolver.step();
                constraintSolveTime += System.nanoTime();

                // Physics part II
                physicsSolveTime = -System.nanoTime();
                physicsSolver.solve();
                physicsSolveTime += System.nanoTime();
            } while(!physicsSolver.stepFinished());

            // Post-step
            stepHandlers.forEach(h -> h.onStepEnd(stepTime));
        }
        totalTime += System.nanoTime();

        for(final var body : rigidBodies) {
            if(body == null)
                continue;
            if(body.externalForceReset) {
                final var state = body.getState();
                state.extForce.x = 0;
                state.extForce.y = 0;
                state.extForceA = 0;
            }
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

    private static void dumpBody(OutputStreamWriter writer, RigidBody body) throws IOException {
        final var state = body.getState();
        writer.write("Body" + body.index() + "\t");
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
