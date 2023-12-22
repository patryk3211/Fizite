package com.patryk3211.fizite.blockentity;

import com.patryk3211.fizite.Fizite;
import com.patryk3211.fizite.block.AllBlocks;
import com.patryk3211.fizite.block.cylinder.PneumaticCylinder;
import com.patryk3211.fizite.capability.CapabilitiesBlockEntity;
import com.patryk3211.fizite.capability.CapabilitiesBlockEntityTemplate;
import com.patryk3211.fizite.capability.Capability;
import com.patryk3211.fizite.simulation.gas.ConstantPropertyGasCapability;
import com.patryk3211.fizite.simulation.gas.GasCapability;
import com.patryk3211.fizite.simulation.gas.GasSimulator;
import com.patryk3211.fizite.simulation.physics.*;
import com.patryk3211.fizite.simulation.physics.simulation.FrictionModel;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.Constraint;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.PistonConstraint;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.joml.Vector2f;

import java.util.List;

import static com.patryk3211.fizite.capability.WrenchConnectableCapability.box;

public class CylinderEntity extends CapabilitiesBlockEntity {
    private static final List<PhysicsZone> ZONES_NEGATIVE = List.of(
            PhysicsZone.of(box(6, 6, 15, 10, 10, 16), new Vector2f(-0.5f, 0.0f), MovementType.LINEAR, 0)
    );
    private static final List<PhysicsZone> ZONES_POSITIVE = List.of(
            PhysicsZone.of(box(6, 6, 15, 10, 10, 16), new Vector2f(0.5f, 0.0f), MovementType.LINEAR, 0)
    );

    public static final CapabilitiesBlockEntityTemplate<CylinderEntity> TEMPLATE
            = new CapabilitiesBlockEntityTemplate<>(CylinderEntity::new)
            .forBlock(AllBlocks.COPPER_CYLINDER)
            .with(CylinderEntity::makeGasCapability)
            .with(CylinderEntity::makePhysicsCapability)
            .with(CylinderStepHandler::new)
            .with(CylinderForceGenerator::new)
            .initialClientSync();

    private static Capability makeGasCapability(BlockState state) {
        if(state.getBlock() instanceof PneumaticCylinder cylinder) {
            float volume = cylinder.getPistonTopVolume();
            return new ConstantPropertyGasCapability(volume, 1, 1, state.get(Properties.FACING));
        } else {
            Fizite.LOGGER.error("Blocks using PipeEntity must be an instance of PipeBase");
            return null;
        }
    }

    private static Capability makePhysicsCapability(BlockState state) {
        if(state.get(Properties.FACING).getDirection() == Direction.AxisDirection.POSITIVE) {
            return new SimplePhysicsCapability(ZONES_POSITIVE, rb -> {
                rb.setMarker("Piston");
                rb.getState().position.x = -2;
                return new Constraint[]{new PistonConstraint(rb, 0, 0)};
            }, false);
        } else {
            return new SimplePhysicsCapability(ZONES_NEGATIVE, rb -> {
                rb.setMarker("Piston");
                rb.getState().position.x = 2;
                return new Constraint[]{new PistonConstraint(rb, 0, 0)};
            }, false);
        }
    }

    private static class CylinderStepHandler extends StepHandlerCapability {
        private final float origin;
        private final float tdcVolume;
        private final float pistonArea;

        private GasCapability gas;

        public CylinderStepHandler(BlockState state) {
            assert state.getBlock() instanceof PneumaticCylinder;
            final var cylinder = (PneumaticCylinder) state.getBlock();

            this.origin = state.get(Properties.FACING).getDirection() == Direction.AxisDirection.POSITIVE ? -2 : 2;
            this.pistonArea = cylinder.getPistonArea();
            this.tdcVolume = cylinder.getPistonTopVolume();
        }

        private float calculateVolume() {
            final var body = entity.getCapability(PhysicsCapability.class).body(0);
            final var currentPos = body.getState().position.x;
            final var chamberLength = origin - currentPos;
            return (float) (tdcVolume + Math.max(chamberLength * pistonArea, 0));
        }

        @Override
        public void onLoad() {
            super.onLoad();
            gas = entity.getCapability(GasCapability.class);
        }

        @Override
        public void onStepEnd(double deltaTime) {
            final var cell = gas.cells().get(0);
            cell.changeVolumeTo(calculateVolume());
        }
    }

    private static class CylinderForceGenerator extends ForceGeneratorCapability {
        private final float pistonArea;
        private final float forceDirection;
        private GasCapability gas;

        public CylinderForceGenerator(BlockState state) {
            assert state.getBlock() instanceof PneumaticCylinder;
            final var cylinder = (PneumaticCylinder) state.getBlock();

            this.pistonArea = cylinder.getPistonArea();
            this.forceDirection = state.get(Properties.FACING).getDirection() == Direction.AxisDirection.POSITIVE ? -1 : 1;
        }

        @Override
        public void onLoad() {
            super.onLoad();
            gas = entity.getCapability(GasCapability.class);
        }

        private static double calculateFriction(double velocity) {
            return FrictionModel.DEFAULT.calculate(velocity, 0) * -Math.signum(velocity);
        }

        @Override
        public void apply(double deltaTime) {
            final var cell = gas.cells().get(0);
            final var body = super.body(0);

            double internalPressure = cell.pressure();
            final double pressureDifference = internalPressure - GasSimulator.ATMOSPHERIC_PRESSURE;

            // Pa = N / m²
            final double force = pressureDifference * pistonArea * forceDirection;
            final double frictionForce = calculateFriction(body.getState().velocity.x);
            if(Double.isNaN(force) || Double.isInfinite(force))
                return;
            body.getState().extForce.x = -force + frictionForce;
        }
    }

    protected CylinderEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
