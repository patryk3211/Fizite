package com.patryk3211.fizite.block;

import com.patryk3211.fizite.blockentity.ConnectingRodEntity;
import com.patryk3211.fizite.simulation.physics.PhysicsStorage;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

// TODO: Temporary class, will be adjusted
public class ConnectingRod extends ModdedBlock implements BlockEntityProvider {
    private static final VoxelShape BB_Z = createCuboidShape(7, 6, 0, 9, 10, 16);
    private static final VoxelShape BB_X = createCuboidShape(0, 6, 7, 16, 10, 9);

    public ConnectingRod() {
        super(FabricBlockSettings.create().strength(5.0f));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.HORIZONTAL_FACING);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(Properties.HORIZONTAL_FACING, ctx.getHorizontalPlayerFacing().rotateYClockwise());//ctx.getSide().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return ConnectingRodEntity.TEMPLATE.create(pos, state);
    }
//
//    @Override
//    protected void onBlockRemoved(BlockState state, World world, BlockPos pos) {
//        PhysicsStorage.get(world).clearPosition(pos);
//    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        final var axis = state.get(Properties.HORIZONTAL_FACING).getAxis();
        return switch(axis) {
            case X -> BB_X;
            case Z -> BB_Z;
            case Y -> throw new IllegalStateException("Horizontal facing cannot have a Y axis direction");
        };
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }
}
