package com.patryk3211.fizite.blockentity;

import com.patryk3211.fizite.block.AllBlocks;
import com.patryk3211.fizite.capability.CapabilitiesBlockEntity;
import com.patryk3211.fizite.capability.CapabilitiesBlockEntityTemplate;
import com.patryk3211.fizite.simulation.physics.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

import java.util.List;

import static com.patryk3211.fizite.capability.WrenchConnectableCapability.box;

public class HandCrankEntity extends CapabilitiesBlockEntity {
    private static final List<PhysicsZone> ZONES = List.of(
            PhysicsZone.of(box(6, 6, 0, 12, 12, 2), 0, 0, MovementType.ROTATION, 0)
    );

    public static final CapabilitiesBlockEntityTemplate<HandCrankEntity> TEMPLATE
            = new CapabilitiesBlockEntityTemplate<>(HandCrankEntity::new)
            .forBlock(AllBlocks.HAND_CRANK)
            .with(() -> new SimplePhysicsCapability(ZONES, rb -> {
                rb.setMarker("Shaft");
                rb.lockPosition();
                return null;
            }, false))
            .with(() -> new DeferredForceGenerator(0, true));

    protected HandCrankEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
