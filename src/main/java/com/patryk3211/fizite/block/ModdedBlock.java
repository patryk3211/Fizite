package com.patryk3211.fizite.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public class ModdedBlock extends Block {
    public ModdedBlock(Settings settings) {
        super(settings);
    }

    protected void onBlockRemoved(BlockState state, World world, BlockPos pos) { }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if(!state.isOf(newState.getBlock())) {
            onBlockRemoved(state, world, pos);

            // Remove block entity if one is provided
            if(state.hasBlockEntity()) {
                world.removeBlockEntity(pos);
            }
        }
    }

    public static BooleanProperty propertyFromDirection(@NotNull Direction dir) {
        return switch(dir) {
            case NORTH -> Properties.NORTH;
            case SOUTH -> Properties.SOUTH;
            case EAST -> Properties.EAST;
            case WEST -> Properties.WEST;
            case UP -> Properties.UP;
            case DOWN -> Properties.DOWN;
            default -> throw new IllegalArgumentException("Unknown direction, cannot get property");
        };
    }
}
