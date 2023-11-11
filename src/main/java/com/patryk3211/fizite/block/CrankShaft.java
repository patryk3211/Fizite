package com.patryk3211.fizite.block;

import com.patryk3211.fizite.blockentity.AllBlockEntities;
import com.patryk3211.fizite.blockentity.CrankShaftEntity;
import com.patryk3211.fizite.simulation.physics.PhysicsStorage;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class CrankShaft extends ModdedBlock implements BlockEntityProvider {
    public CrankShaft() {
        super(FabricBlockSettings.create().strength(5.0f));
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CrankShaftEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ?
                null :
                (w, p, s, e) -> {
                    assert e.getType() == AllBlockEntities.CRANK_SHAFT_ENTITY;
                    CrankShaftEntity.serverTick(w, p, s, (CrankShaftEntity) e);
                };
//        return BlockEntityProvider.super.getTicker(world, state, type);
    }

    @Override
    protected void onBlockRemoved(BlockState state, World world, BlockPos pos) {
        if(world instanceof final ServerWorld serverWorld) {
            PhysicsStorage.get(serverWorld).clearPosition(pos);
        }
    }
}
