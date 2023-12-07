package com.patryk3211.fizite.block;

import com.patryk3211.fizite.capability.CapabilitiesBlockEntity;
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
//                final var entity = world.getBlockEntity(pos);
//                if(entity instanceof final CapabilitiesBlockEntity capEntity) {
////                    entity.
//                }
                world.removeBlockEntity(pos);
//                world.removeBlockEntity(pos);
            }
        }
    }
}
