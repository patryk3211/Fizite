package com.patryk3211.fizite.simulation.physics.simulation;

public interface IForceGenerator {
    /**
     * Executed before constraint solve step, should set external forces affecting bodies
     * @param deltaTime Delta time of the step
     */
    void apply(double deltaTime);
}
