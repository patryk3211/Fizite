package com.patryk3211.fizite.blockentity;

import com.patryk3211.fizite.block.AllBlocks;
import com.patryk3211.fizite.capability.CapabilitiesBlockEntity;
import com.patryk3211.fizite.capability.CapabilitiesBlockEntityTemplate;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

public class CapableConnectingRodEntity extends CapabilitiesBlockEntity {
    public static final CapabilitiesBlockEntityTemplate<CapableConnectingRodEntity> TEMPLATE
            = new CapabilitiesBlockEntityTemplate<>(CapableConnectingRodEntity::new)
            .forBlock(AllBlocks.CONNECTING_ROD);

    public CapableConnectingRodEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
