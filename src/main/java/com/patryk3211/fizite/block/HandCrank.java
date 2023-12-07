package com.patryk3211.fizite.block;

import com.patryk3211.fizite.blockentity.HandCrankEntity;
import com.patryk3211.fizite.capability.CapabilitiesBlockEntity;
import com.patryk3211.fizite.item.AllItems;
import com.patryk3211.fizite.simulation.physics.DeferredForceGenerator;
import com.patryk3211.fizite.simulation.physics.PhysicsCapability;
import com.patryk3211.fizite.utility.DirectionUtilities;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class HandCrank extends ModdedBlock implements BlockEntityProvider {
    private static final VoxelShape[] SHAPES = DirectionUtilities.makeFacingShapes(
            createCuboidShape(6, 6, 0, 10, 10, 2),
            createCuboidShape(1, 1, 2, 15, 15, 4)
    );

    public HandCrank() {
        super(FabricBlockSettings.create().strength(3.0f));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.FACING);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(Properties.FACING, ctx.getSide().getOpposite());
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        final var itemStack = player.getStackInHand(hand);
        if(itemStack.getItem() == AllItems.WRENCH)
            return ActionResult.PASS;

        final var entity = CapabilitiesBlockEntity.getEntity(world, pos);
        if(entity == null)
            return ActionResult.PASS;
        // Check if player has hunger
        if(player.getHungerManager().getFoodLevel() == 0 && !player.isCreative())
            return ActionResult.PASS;
        // Apply force if desired speed is not yet achieved
        final var forceGenerator = entity.getCapability(DeferredForceGenerator.class);
        final var bodyState = entity.getCapability(PhysicsCapability.class).body(0).getState();

        float applyForce = 0;
        if(player.isSneaking()) {
            if(bodyState.velocityA > -6) {
                applyForce = -150;
            }
        } else {
            if(bodyState.velocityA < 6) {
                applyForce = 150;
            }
        }
        if(applyForce != 0) {
            forceGenerator.forceA = applyForce;
            if(!player.isCreative())
                player.getHungerManager().addExhaustion(0.1f);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES[state.get(Properties.FACING).getId()];
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return HandCrankEntity.TEMPLATE.create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return HandCrankEntity.TEMPLATE.getTicker(world, state, type);
    }
}
