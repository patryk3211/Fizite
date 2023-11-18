package com.patryk3211.fizite.simulation.physics.simulation;

public class FrictionModel {
    private static final double Sqrt2 = Math.sqrt(2);
    private static final double Sqrt2E = Sqrt2 * Math.E;

    public static final FrictionModel DEFAULT = new FrictionModel(0.1, 25, 100);

    private final double breakawayFriction;
    private final double viscousFrictionCoefficient;

    private final double v_stribeck;
    private final double v_coulomb;

    public FrictionModel(double breakawayVelocity, double breakawayFriction, double viscousFrictionCoefficient) {
        this.breakawayFriction = breakawayFriction;
        this.viscousFrictionCoefficient = viscousFrictionCoefficient;

        v_stribeck = breakawayVelocity * Sqrt2;
        v_coulomb = breakawayVelocity / 10;
    }

    // Equation borrowed from: https://www.mathworks.com/help/simscape/ref/translationalfriction.html
    public double calculate(double velocity, double coulombFriction) {
        velocity = Math.abs(velocity);
        final var F_0 = Sqrt2E * (breakawayFriction - coulombFriction);
        final var VstV = velocity / v_stribeck;
        final var F_1 = Math.exp(-VstV * VstV) * VstV;
        final var F_2 = coulombFriction * Math.tanh(velocity / v_coulomb);
        final var F_3 = viscousFrictionCoefficient * velocity;
        return F_0 + F_1 + F_2 + F_3;
    }
}
