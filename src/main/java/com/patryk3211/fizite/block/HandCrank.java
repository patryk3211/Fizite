package com.patryk3211.fizite.block;

import com.patryk3211.fizite.blockentity.AllBlockEntities;
import com.patryk3211.fizite.blockentity.HandCrankEntity;
import com.patryk3211.fizite.simulation.physics.PhysicsStorage;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class HandCrank extends ModdedBlock implements BlockEntityProvider {
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
        final var entity = world.getBlockEntity(pos, AllBlockEntities.HAND_CRANK_ENTITY);
        if(entity.isEmpty())
            return ActionResult.PASS;
        // Check if player has hunger
        if(player.getHungerManager().getFoodLevel() == 0)
            return ActionResult.PASS;
        // Apply force if desired speed is not yet achieved
        final var body = entity.get().bodies()[0];
        final var physState = body.getState();
        double applyForce = 0;
        if(player.isSneaking()) {
            if(physState.velocityA > -6) {
                applyForce = -150;
            }
        } else {
            if(physState.velocityA < 6) {
                applyForce = 150;
            }
        }
        if(applyForce != 0) {
            physState.extForceA = applyForce;
            player.getHungerManager().addExhaustion(0.1f);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new HandCrankEntity(pos, state);
    }

    @Override
    protected void onBlockRemoved(BlockState state, World world, BlockPos pos) {
        PhysicsStorage.get(world).clearPosition(pos);
    }
}
