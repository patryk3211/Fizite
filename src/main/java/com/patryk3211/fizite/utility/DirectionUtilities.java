package com.patryk3211.fizite.utility;

import net.minecraft.util.math.Direction;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class DirectionUtilities {
    public static final Vector3f POSITIVE_X = new Vector3f(1, 0, 0);
    public static final Vector3f POSITIVE_Y = new Vector3f(0, 1, 0);
    public static final Vector3f POSITIVE_Z = new Vector3f(0, 0, 1);

    public static int positiveDirectionIndex(Direction direction) {
        return switch(direction) {
            case EAST -> 0;
            case UP -> 1;
            case SOUTH -> 2;
            default -> throw new IllegalArgumentException("Provided direction is negative");
        };
    }

    public static Direction perpendicular(Direction direction) {
        return switch(direction) {
            case NORTH -> Direction.EAST;
            case SOUTH -> Direction.WEST;
            case EAST -> Direction.NORTH;
            case WEST -> Direction.SOUTH;
            case UP -> Direction.NORTH;
            case DOWN -> Direction.SOUTH;
        };
    }

    public static Vector3fc getAxisNormal(Direction.Axis axis) {
        return switch(axis) {
            case X -> POSITIVE_X;
            case Y -> POSITIVE_Y;
            case Z -> POSITIVE_Z;
        };
    }
}
