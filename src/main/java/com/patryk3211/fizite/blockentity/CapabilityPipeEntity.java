package com.patryk3211.fizite.blockentity;

import com.patryk3211.fizite.block.AllBlocks;
import com.patryk3211.fizite.capability.CapabilitiesBlockEntity;
import com.patryk3211.fizite.capability.CapabilitiesBlockEntityTemplate;
import com.patryk3211.fizite.simulation.gas.GasCapability;
import com.patryk3211.fizite.utility.IDebugOutput;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

public class CapabilityPipeEntity extends CapabilitiesBlockEntity {
    public static final CapabilitiesBlockEntityTemplate<CapabilityPipeEntity> TEMPLATE
            = new CapabilitiesBlockEntityTemplate<>(CapabilityPipeEntity::new)
            .forBlock(AllBlocks.COPPER_PIPE)
            .with(() -> new GasCapability(1));

    protected CapabilityPipeEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
