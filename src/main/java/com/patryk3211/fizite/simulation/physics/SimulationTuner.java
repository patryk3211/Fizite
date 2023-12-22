package com.patryk3211.fizite.simulation.physics;

import com.patryk3211.fizite.simulation.Simulator;
import com.patryk3211.fizite.simulation.physics.simulation.PhysicsWorld;

import java.util.function.Consumer;

public class SimulationTuner {
    public static final int MIN_STEP_COUNT = 10;
    public static final int MAX_STEP_COUNT = 100;
    public static final int STEP_DELTA = 5;

    private final PhysicsWorld physics;

    public SimulationTuner(PhysicsStorage physics) {
        this.physics = physics.physicsSimulation();
    }

    public void tune(Consumer<Integer> stepApplicator) {
        final var currentSteps = physics.stepCount();
        final var currentStepTime = Simulator.TICK_RATE / currentSteps;
        final var maxDelta = Math.max(Math.abs(physics.maxVelocity()), Math.abs(physics.maxAngularVelocity())) * currentStepTime;

        var newSteps = currentSteps;
        if(maxDelta > 2) {
            newSteps += STEP_DELTA;
        } else if(maxDelta < 0.1) {
            newSteps -= STEP_DELTA;
        }

        if(currentSteps != newSteps && newSteps >= MIN_STEP_COUNT && newSteps <= MAX_STEP_COUNT)
            stepApplicator.accept(newSteps);
    }
}
