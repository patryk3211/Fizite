package com.patryk3211.fizite.tiers;

import com.patryk3211.fizite.utility.Units;

public enum Material implements ITier {
    COPPER("copper", 41.368f * Units.MEGA, 8960, 1084);

    final String name;
    // TODO: I will probably want max pressure to change in response to temperature
    final float maxPressure;
    final float density;
    final float meltingPoint;

    Material(String name, float maxPressure, float density, float meltingPoint) {
        this.name = name;
        this.maxPressure = maxPressure;
        this.density = density;
        this.meltingPoint = meltingPoint;
    }

    @Override
    public String getName() {
        return name;
    }
}
