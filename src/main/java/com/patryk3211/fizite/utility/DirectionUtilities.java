package com.patryk3211.fizite.utility;

import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
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

    public static VoxelShape transformShape(VoxelShape shape, Direction facing) {
        final var bb = shape.getBoundingBox();
        return switch(facing) {
            case NORTH -> VoxelShapes.cuboid(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
            case SOUTH -> VoxelShapes.cuboid(1 - bb.maxX, bb.minY, 1 - bb.maxZ, 1 - bb.minX, bb.maxY, 1 - bb.minZ);
            case EAST -> VoxelShapes.cuboid(1 - bb.maxZ, bb.minY, bb.minX, 1 - bb.minZ, bb.maxY, bb.maxX);
            case WEST -> VoxelShapes.cuboid(bb.minZ, bb.minY, 1 - bb.maxX, bb.maxZ, bb.maxY, 1 - bb.minX);
            case UP -> VoxelShapes.cuboid(bb.minX, 1 - bb.maxZ, bb.minY, bb.maxX, 1 - bb.minZ, bb.maxY);
            case DOWN -> VoxelShapes.cuboid(bb.minX, bb.minZ, 1 - bb.maxY, bb.maxX, bb.maxZ, 1 - bb.minY);
        };
    }

    public static VoxelShape[] makeFacingShapes(VoxelShape... baseCuboids) {
        VoxelShape[] result = new VoxelShape[6];
        for(final var dir : Direction.values()) {
            VoxelShape shape = VoxelShapes.empty();
            for(final var cube : baseCuboids)
                shape = VoxelShapes.union(shape, transformShape(cube, dir));
            result[dir.getId()] = shape;
        }
        return result;
    }
}
