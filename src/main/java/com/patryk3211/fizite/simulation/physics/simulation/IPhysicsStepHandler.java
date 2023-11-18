package com.patryk3211.fizite.simulation.physics.simulation;

public interface IPhysicsStepHandler {
    /**
     * Executed at the end of every physics simulation step (after the physics step was solved)
     * @param deltaTime Delta time of the step
     */
    void onStepEnd(double deltaTime);
}
