package com.patryk3211.fizite.blockentity;

import com.patryk3211.fizite.block.AllBlocks;
import com.patryk3211.fizite.capability.CapabilitiesBlockEntity;
import com.patryk3211.fizite.capability.CapabilitiesBlockEntityTemplate;
import com.patryk3211.fizite.simulation.physics.MovementType;
import com.patryk3211.fizite.simulation.physics.PhysicsZone;
import com.patryk3211.fizite.simulation.physics.SimplePhysicsCapability;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

import java.util.List;

import static com.patryk3211.fizite.capability.WrenchConnectableCapability.box;

public class ConnectingRodEntity extends CapabilitiesBlockEntity {
    private static final List<PhysicsZone> ZONES = List.of(
            PhysicsZone.of(box(7, 6, 0, 9, 10, 10), 0.5f, 0.0f, MovementType.XY, 0),
            PhysicsZone.of(box(7, 6, 12, 9, 10, 16), -0.5f, 0.0f, MovementType.XY, 0)
    );

    public static final CapabilitiesBlockEntityTemplate<ConnectingRodEntity> TEMPLATE
            = new CapabilitiesBlockEntityTemplate<>(ConnectingRodEntity::new)
            .forBlock(AllBlocks.CONNECTING_ROD)
            .with(() -> new SimplePhysicsCapability(ZONES, rb -> {
                rb.setMarker("Rod");
                rb.getState().position.x = 1;
                return null;
            }, true));

    public ConnectingRodEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
