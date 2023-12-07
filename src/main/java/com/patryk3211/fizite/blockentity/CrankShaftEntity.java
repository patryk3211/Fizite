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

public class CrankShaftEntity extends CapabilitiesBlockEntity {
    private static final List<PhysicsCapability.Zone> ZONES = List.of(
            new PhysicsCapability.Zone(
                    new Box(6.0 / 16, 6.0 / 16, 1.0 / 16, 10.0 / 16, 10.0 / 16, 5.0 / 16),
                    new Vector2f(0.5f, 0.0f), MovementType.XY, 0
            ),
            new PhysicsCapability.Zone(
                    new Box(0, 6.0 / 16, 6.0 / 16, 2.0 / 16, 10.0 / 16, 10.0 / 16),
                    new Vector2f(), MovementType.ROTATION, 0
            ),
            new PhysicsCapability.Zone(
                    new Box(14.0 / 16, 6.0 / 16, 6.0 / 16, 1.0, 10.0 / 16, 10.0 / 16),
                    new Vector2f(), MovementType.ROTATION, 0
            )
    );

    public static final CapabilitiesBlockEntityTemplate<CrankShaftEntity> TEMPLATE
            = new CapabilitiesBlockEntityTemplate<>(CrankShaftEntity::new)
            .forBlock(AllBlocks.CRANK_SHAFT)
            .with(() -> new SimplePhysicsCapability(ZONES, rb -> {
                rb.setMarker("Shaft");
                rb.lockPosition();
                return null;
            })).as(PhysicsCapability.class).as(WrenchInteractionCapability.class);

    protected CrankShaftEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
