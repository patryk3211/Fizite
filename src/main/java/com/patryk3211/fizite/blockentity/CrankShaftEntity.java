package com.patryk3211.fizite.blockentity;

import com.patryk3211.fizite.block.AllBlocks;
import com.patryk3211.fizite.capability.CapabilitiesBlockEntity;
import com.patryk3211.fizite.capability.CapabilitiesBlockEntityTemplate;
import com.patryk3211.fizite.simulation.physics.MovementType;
import com.patryk3211.fizite.simulation.physics.PhysicsCapability;
import com.patryk3211.fizite.simulation.physics.PhysicsZone;
import com.patryk3211.fizite.simulation.physics.SimplePhysicsCapability;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector2f;

import java.util.List;

import static com.patryk3211.fizite.capability.WrenchConnectableCapability.box;

public class CrankShaftEntity extends CapabilitiesBlockEntity {
    private static final List<PhysicsZone> ZONES = List.of(
            PhysicsZone.of(box(6, 6, 1, 10, 10, 5), CrankShaftEntity::facingZoneInfo),//0.5f, 0.0f, MovementType.XY, 0),
            PhysicsZone.of(box(0, 6, 6, 2, 10, 10), 0, 0, MovementType.ROTATION, 0),
            PhysicsZone.of(box(14, 6, 6, 16, 10, 10), 0, 0, MovementType.ROTATION, 0)
    );

    public static final CapabilitiesBlockEntityTemplate<CrankShaftEntity> TEMPLATE
            = new CapabilitiesBlockEntityTemplate<>(CrankShaftEntity::new)
            .forBlock(AllBlocks.CRANK_SHAFT)
            .with(() -> new SimplePhysicsCapability(ZONES, rb -> {
                rb.setMarker("Shaft");
                rb.lockPosition();
                return null;
            }, false));

    private static PhysicsZone.ConnectionInfo facingZoneInfo(BlockState state) {
        return new PhysicsZone.ConnectionInfo(switch(state.get(Properties.HORIZONTAL_FACING).getDirection()) {
            case POSITIVE -> new Vector2f(-0.5f, 0.0f);
            case NEGATIVE -> new Vector2f(0.5f, 0.0f);
            }, MovementType.XY, 0);
    }

    protected CrankShaftEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
