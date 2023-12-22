package com.patryk3211.fizite.block.cylinder;

import com.patryk3211.fizite.block.ModdedBlock;
import com.patryk3211.fizite.blockentity.CylinderEntity;
import com.patryk3211.fizite.simulation.gas.GasStorage;
import com.patryk3211.fizite.tiers.ITier;
import com.patryk3211.fizite.tiers.ITieredBlock;
import com.patryk3211.fizite.tiers.Material;
import com.patryk3211.fizite.simulation.physics.PhysicsStorage;
import com.patryk3211.fizite.utility.DirectionUtilities;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public abstract class PneumaticCylinder extends ModdedBlock implements BlockEntityProvider, ITieredBlock {
    private static final VoxelShape[] SHAPES = DirectionUtilities.makeFacingShapes(
            createCuboidShape(4, 4, 0, 12, 12, 15),
            createCuboidShape(6, 6, 15, 10, 10, 16)
    );

    private final Material material;
    private final float pistonArea;
    private final float strokeLength;
    private final float pistonTopVolume;

    public PneumaticCylinder(Material material, float pistonArea, float strokeLength, float pistonTopVolume) {
        super(FabricBlockSettings.create().strength(7.0f));
        this.material = material;
        this.pistonArea = pistonArea;
        this.strokeLength = strokeLength;
        this.pistonTopVolume = pistonTopVolume;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.FACING);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return CylinderEntity.TEMPLATE.create(pos, state);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(Properties.FACING, ctx.getPlayerLookDirection());
    }

    public float getPistonArea() {
        return pistonArea;
    }

    public float getStrokeLength() {
        return strokeLength;
    }

    public float getPistonTopVolume() {
        return pistonTopVolume;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES[state.get(Properties.FACING).getId()];
    }

    @Override
    public <T extends ITier> T getTier(Class<T> clazz) {
        Object out = null;
        if(clazz == Material.class)
            out = Material.COPPER;
        return clazz.cast(out);
    }
}
