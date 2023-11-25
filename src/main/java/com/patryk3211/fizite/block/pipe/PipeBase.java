package com.patryk3211.fizite.block.pipe;

import com.patryk3211.fizite.block.ModdedBlock;
import com.patryk3211.fizite.blockentity.CapabilityPipeEntity;
import com.patryk3211.fizite.simulation.gas.GasStorage;
import com.patryk3211.fizite.simulation.gas.IGasCellProvider;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public abstract class PipeBase extends ModdedBlock implements BlockEntityProvider {
    final float volume;

    /*
        Calculate max pressure using the formula 'P = 2 * T * S / D', where:
        P = max pressure
        S = allowable stress
        t = wall thickness
        D = outside diameter
    */

    public PipeBase(float volume) {
        super(FabricBlockSettings.create()
                .strength(3.0f));

        this.volume = volume;

        setDefaultState(getDefaultState()
                .with(Properties.NORTH, false)
                .with(Properties.SOUTH, false)
                .with(Properties.EAST, false)
                .with(Properties.WEST, false)
                .with(Properties.UP, false)
                .with(Properties.DOWN, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.NORTH);
        builder.add(Properties.SOUTH);
        builder.add(Properties.EAST);
        builder.add(Properties.WEST);
        builder.add(Properties.UP);
        builder.add(Properties.DOWN);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState state = getDefaultState();
        for(Direction dir : Direction.values()) {
            var neighborPos = ctx.getBlockPos().add(dir.getVector());
            var neighborEntity = ctx.getWorld().getBlockEntity(neighborPos);
            if(neighborEntity instanceof final IGasCellProvider provider) {
                if(provider.getCell(dir.getOpposite()) == null)
                    continue;
                // Attach to neighbor
                state = state.with(ModdedBlock.propertyFromDirection(dir), true);
            }
        }
        return state;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return CapabilityPipeEntity.TEMPLATE.create(pos, state); //new PipeEntity(pos, state);
    }

//    @Nullable
//    @Override
//    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull World world, BlockState state, BlockEntityType<T> type) {
//        return world.isClient ?
//            null : // Client ticker
//            (w, p, s, t) -> { // Server ticker
//                if(!(t instanceof PipeEntity))
//                    throw new IllegalStateException("Pipe ticker called for non pipe entity");
//                PipeEntity.serverTick((ServerWorld) w, p, s, (PipeEntity) t);
//            };
//    }

    @Override
    protected void onBlockRemoved(BlockState state, World world, BlockPos pos) {
        if(world instanceof final ServerWorld serverWorld) {
            final GasStorage boundaries = GasStorage.get(serverWorld);
            boundaries.clearPosition(pos);
        }
    }

    public float getVolume() {
        return volume;
    }
}
