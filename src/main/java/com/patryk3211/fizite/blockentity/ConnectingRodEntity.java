package com.patryk3211.fizite.blockentity;

import com.patryk3211.fizite.block.AllBlocks;
import com.patryk3211.fizite.capability.CapabilitiesBlockEntity;
import com.patryk3211.fizite.capability.CapabilitiesBlockEntityTemplate;
import com.patryk3211.fizite.capability.WrenchInteractionCapability;
import com.patryk3211.fizite.simulation.physics.MovementType;
import com.patryk3211.fizite.simulation.physics.PhysicsCapability;
import com.patryk3211.fizite.simulation.physics.SimplePhysicsCapability;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.joml.Vector2f;

import java.util.List;

public class ConnectingRodEntity extends CapabilitiesBlockEntity {
    private static final List<PhysicsCapability.Zone> ZONES = List.of(
            new PhysicsCapability.Zone(
                    new Box(7.0 / 16, 6.0 / 16, 0.0, 9.0 / 16, 10.0 / 16, 10.0 / 16),
                    new Vector2f(0.5f, 0.0f), MovementType.XY, 0
            ),
            new PhysicsCapability.Zone(
                    new Box(7.0 / 16, 6.0 / 16, 12.0 / 16, 9.0 / 16, 10.0 / 16, 1.0),
                    new Vector2f(-0.5f, 0.0f), MovementType.XY, 0
            )
    );

    public static final CapabilitiesBlockEntityTemplate<ConnectingRodEntity> TEMPLATE
            = new CapabilitiesBlockEntityTemplate<>(ConnectingRodEntity::new)
            .forBlock(AllBlocks.CONNECTING_ROD)
            .with(() -> new SimplePhysicsCapability(ZONES, rb -> {
                rb.setMarker("Rod");
                rb.getState().position.x = 1;
                return null;
            }));

    public ConnectingRodEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
