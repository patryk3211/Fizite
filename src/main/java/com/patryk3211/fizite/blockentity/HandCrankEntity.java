package com.patryk3211.fizite.blockentity;

import com.patryk3211.fizite.block.AllBlocks;
import com.patryk3211.fizite.capability.CapabilitiesBlockEntity;
import com.patryk3211.fizite.capability.CapabilitiesBlockEntityTemplate;
import com.patryk3211.fizite.capability.WrenchInteractionCapability;
import com.patryk3211.fizite.simulation.physics.*;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.Constraint;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.PositionConstraint;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.joml.Vector2f;

import java.util.List;

public class HandCrankEntity extends CapabilitiesBlockEntity {
    private static final List<PhysicsCapability.Zone> ZONES = List.of(
            new PhysicsCapability.Zone(
                    new Box(6.0 / 16.0, 6.0 / 16.0, 0, 12.0 / 16.0, 12.0 / 16.0, 2.0 / 16.0),
                    new Vector2f(), MovementType.ROTATION, 0)
    );

    public static final CapabilitiesBlockEntityTemplate<HandCrankEntity> TEMPLATE
            = new CapabilitiesBlockEntityTemplate<>(HandCrankEntity::new)
            .forBlock(AllBlocks.HAND_CRANK)
            .with(() -> new SimplePhysicsCapability(ZONES, rb -> {
                rb.setMarker("Shaft");
//                rb.lockPosition();
                return new Constraint[] { new PositionConstraint(rb, 0, 0) };
            })).as(PhysicsCapability.class).as(WrenchInteractionCapability.class)
            .with(() -> new DeferredForceGenerator(0, true)).as(ForceGeneratorCapability.class);

    protected HandCrankEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
