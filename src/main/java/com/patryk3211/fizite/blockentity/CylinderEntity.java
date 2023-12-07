package com.patryk3211.fizite.blockentity;

import com.patryk3211.fizite.Fizite;
import com.patryk3211.fizite.block.AllBlocks;
import com.patryk3211.fizite.block.cylinder.PneumaticCylinder;
import com.patryk3211.fizite.capability.CapabilitiesBlockEntity;
import com.patryk3211.fizite.capability.CapabilitiesBlockEntityTemplate;
import com.patryk3211.fizite.capability.Capability;
import com.patryk3211.fizite.capability.WrenchInteractionCapability;
import com.patryk3211.fizite.simulation.gas.ConstantPropertyGasCapability;
import com.patryk3211.fizite.simulation.gas.GasCapability;
import com.patryk3211.fizite.simulation.physics.MovementType;
import com.patryk3211.fizite.simulation.physics.PhysicsCapability;
import com.patryk3211.fizite.simulation.physics.SimplePhysicsCapability;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.Constraint;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.PistonConstraint;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.joml.Vector2f;

import java.util.Collection;
import java.util.List;

public class CylinderEntity extends CapabilitiesBlockEntity {
    private static final List<PhysicsCapability.Zone> ZONES = List.of(
            new PhysicsCapability.Zone(
                    new Box(6.0 / 16, 6.0 / 16, 15.0 / 16, 10.0 / 16, 10.0 / 16, 1.0),
                    new Vector2f(-0.5f, 0.0f), MovementType.LINEAR, 0
            )
    );

    public static final CapabilitiesBlockEntityTemplate<CylinderEntity> TEMPLATE
            = new CapabilitiesBlockEntityTemplate<>(CylinderEntity::new)
            .forBlock(AllBlocks.COPPER_CYLINDER)
            .with(CylinderEntity::makeGasCapability).as(GasCapability.class)
            .with(() -> new SimplePhysicsCapability(ZONES, rb -> {
                rb.setMarker("Piston");
                rb.getState().position.x = 2;
                return new Constraint[] { new PistonConstraint(rb, 0, 0) };
            })).as(PhysicsCapability.class).as(WrenchInteractionCapability.class);

    private static Capability makeGasCapability(BlockState state) {
        if(state.getBlock() instanceof PneumaticCylinder cylinder) {
            float volume = cylinder.getPistonTopVolume();
            return new ConstantPropertyGasCapability(volume, 1, 1, state.get(Properties.FACING));
        } else {
            Fizite.LOGGER.error("Blocks using PipeEntity must be an instance of PipeBase");
            return null;
        }
    }

    protected CylinderEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
