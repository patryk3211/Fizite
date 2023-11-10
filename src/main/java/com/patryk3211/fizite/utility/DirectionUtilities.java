package com.patryk3211.fizite.utility;

import net.minecraft.util.math.Direction;

public class DirectionUtilities {
    public static int positiveDirectionIndex(Direction direction) {
        return switch(direction) {
            case EAST -> 0;
            case UP -> 1;
            case SOUTH -> 2;
            default -> throw new IllegalArgumentException("Provided direction is negative");
        };
    }
}
