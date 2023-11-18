package com.patryk3211.fizite.blockentity;

import com.patryk3211.fizite.block.pipe.PipeBase;
import com.patryk3211.fizite.simulation.gas.GasCell;
import com.patryk3211.fizite.simulation.gas.GasStorage;
import com.patryk3211.fizite.simulation.gas.IGasCellProvider;
import io.wispforest.owo.nbt.NbtKey;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public class PipeEntity extends BlockEntity implements IGasCellProvider {
    private static final NbtKey<Integer> NBT_MASK = new NbtKey<>("mask", NbtKey.Type.INT);

    private final GasCell gasStateCell;
    // Bitmask of sides which are disabled from connecting to other gas cell providers
    private int sideMask;
    private int prevSideMask;

    public PipeEntity(BlockPos pos, BlockState state) {
        super(AllBlockEntities.PIPE_ENTITY, pos, state);
        assert state.getBlock() instanceof PipeBase : "A Block using PipeEntity must be an instance of PipeBase";

        final float volume = ((PipeBase) state.getBlock()).getVolume();
        gasStateCell = new GasCell(volume);
        sideMask = 0;
        prevSideMask = 0b111111;
    }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);
        GasStorage.get(world).addBlockEntity(this);

        if(!world.isClient) {
            prevSideMask = sideMask;
        }
    }

    public static void serverTick(ServerWorld world, BlockPos pos, BlockState state, @NotNull PipeEntity blockEntity) {
        if(blockEntity.sideMask != blockEntity.prevSideMask) {
            // Update boundaries
            final var diff = blockEntity.sideMask ^ blockEntity.prevSideMask;

            for(final var dir : Direction.values()) {
                final var dirMask = 1 << dir.getId();
                if((diff & dirMask) == 0) {
                    // No change, skip this direction
                    continue;
                }
                if((blockEntity.sideMask & dirMask) == 0) {
                    // New side mask enables this side for connections
                    IGasCellProvider.connect(blockEntity, dir);
                } else {
                    // New side mask disables this side from connections
                    final var boundaries = GasStorage.get(world);
                    boundaries.removeBoundary(pos, dir);
                }
            }

            blockEntity.prevSideMask = blockEntity.sideMask;
        }

        // Unfortunately there isn't really any other way to handle this,
        // the block entity will be perpetually dirty since even the
        // slightest pressure change needs to be saved and the
        // simulation is always running.
        blockEntity.markDirty();
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        gasStateCell.deserialize(nbt.get(GasCell.NBT_KEY));
        sideMask = nbt.get(NBT_MASK);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.put(GasCell.NBT_KEY, gasStateCell.serialize());
        nbt.put(NBT_MASK, sideMask);
    }

    @Override
    public GasCell getCell(@NotNull Direction dir) {
        if((sideMask & (1 << dir.getId())) != 0)
            return null;
        return gasStateCell;
    }

    @Override
    public GasCell getCell(int i) {
        return gasStateCell;
    }

    @Override
    public double getCrossSection(@NotNull Direction dir) {
        return 1;
    }

    @Override
    public double getFlowConstant(@NotNull Direction dir) {
        return 1;
    }
}
