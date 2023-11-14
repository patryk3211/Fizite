package com.patryk3211.fizite.block;

import com.patryk3211.fizite.blockentity.ConnectingRodEntity;
import com.patryk3211.fizite.simulation.physics.PhysicsStorage;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

// TODO: Temporary class, will be adjusted
public class ConnectingRod extends ModdedBlock implements BlockEntityProvider {
    private static final VoxelShape BB_Z = createCuboidShape(7, 6, 0, 9, 10, 16);

    public ConnectingRod() {
        super(FabricBlockSettings.create().strength(5.0f));
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ConnectingRodEntity(pos, state);
    }

    @Override
    protected void onBlockRemoved(BlockState state, World world, BlockPos pos) {
        PhysicsStorage.get(world).clearPosition(pos);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return BB_Z;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED; //super.getRenderType(state);
    }
}
