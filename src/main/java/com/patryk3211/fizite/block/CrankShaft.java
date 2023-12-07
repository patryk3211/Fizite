package com.patryk3211.fizite.block;

import com.patryk3211.fizite.blockentity.CrankShaftEntity;
import com.patryk3211.fizite.simulation.physics.PhysicsStorage;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class CrankShaft extends ModdedBlock implements BlockEntityProvider {
    public enum ModelPart implements StringIdentifiable {
        STATIC, DYNAMIC;

        @Override
        public String asString() {
            return switch(this) {
                case STATIC -> "static";
                case DYNAMIC -> "dynamic";
            };
        }
    }
    public static EnumProperty<ModelPart> MODEL_PART_PROPERTY = EnumProperty.of("part", ModelPart.class);

    private static final VoxelShape BB_Z =
            VoxelShapes.union(
                    createCuboidShape(1, 0, 2, 15, 12, 14),
                    createCuboidShape(0, 6, 6, 1, 10, 10),
                    createCuboidShape(15, 6, 6, 16, 10, 10)
            ).simplify();

    public CrankShaft() {
        super(FabricBlockSettings.create().strength(5.0f));

        setDefaultState(getDefaultState()
                .with(MODEL_PART_PROPERTY, ModelPart.STATIC));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(MODEL_PART_PROPERTY);
        builder.add(Properties.HORIZONTAL_FACING);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(Properties.HORIZONTAL_FACING, ctx.getHorizontalPlayerFacing().rotateYClockwise());
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return CrankShaftEntity.TEMPLATE.create(pos, state);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return BB_Z;
    }
//
//    @Override
//    protected void onBlockRemoved(BlockState state, World world, BlockPos pos) {
//        PhysicsStorage.get(world).clearPosition(pos);
//    }
}
