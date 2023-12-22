package com.patryk3211.fizite.blockentity;

import com.patryk3211.fizite.Fizite;
import com.patryk3211.fizite.block.AllBlocks;
import com.patryk3211.fizite.block.pipe.PipeBase;
import com.patryk3211.fizite.capability.CapabilitiesBlockEntity;
import com.patryk3211.fizite.capability.CapabilitiesBlockEntityTemplate;
import com.patryk3211.fizite.capability.Capability;
import com.patryk3211.fizite.simulation.gas.ConstantPropertyGasCapability;
import com.patryk3211.fizite.simulation.gas.GasCapability;
import com.patryk3211.fizite.capability.PipeCapability;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

public class PipeEntity extends CapabilitiesBlockEntity {
    public static final CapabilitiesBlockEntityTemplate<PipeEntity> TEMPLATE
            = new CapabilitiesBlockEntityTemplate<>(PipeEntity::new)
            .forBlock(AllBlocks.COPPER_PIPE)
            .with(PipeEntity::makeGasCapability) //.as(GasCapability.class)
            .with(() -> new PipeCapability(GasCapability.class))
//            .with(ConstantPropertyGasCapability.class, GasCapability.class)
            .initialClientSync();

    private static Capability makeGasCapability(BlockState state) {
        if(state.getBlock() instanceof PipeBase pipe) {
            float volume = pipe.getVolume();
            return new ConstantPropertyGasCapability(volume, 1, 1);
        } else {
            Fizite.LOGGER.error("Blocks using PipeEntity must be an instance of PipeBase");
            return null;
        }
    }

    protected PipeEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
