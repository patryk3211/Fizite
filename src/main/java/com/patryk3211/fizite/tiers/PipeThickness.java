package com.patryk3211.fizite.tiers;

public enum PipeThickness {
    ;

    final float diameter;
    final float wallThickness;
    final float flowConstant;

    PipeThickness(float diameter, float wallThickness, float flowConstant) {
        this.diameter = diameter;
        this.wallThickness = wallThickness;
        this.flowConstant = flowConstant;
    }
}
