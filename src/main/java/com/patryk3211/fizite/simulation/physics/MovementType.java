package com.patryk3211.fizite.simulation.physics;

import net.minecraft.util.StringIdentifiable;

public enum MovementType implements StringIdentifiable {
    LINEAR,
    ROTATION,
    XY;

    @Override
    public String asString() {
        return switch(this) {
            case LINEAR -> "linear";
            case ROTATION -> "rotational";
            case XY -> "translational";
//            case ANY -> "any";
        };
    }
}
