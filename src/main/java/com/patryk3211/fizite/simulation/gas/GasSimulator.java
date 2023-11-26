package com.patryk3211.fizite.simulation.gas;

import com.patryk3211.fizite.simulation.Networking;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;

import java.util.*;

public class GasSimulator {
    public static final double GAS_CONSTANT = 8.31446261815324; // J / (K * mol)
    public static final double HEAT_CAPACITY_RATIO = 1.4; // Assumes a 5 dimensional gas
    public static final double CHOKED_FLOW_LIMIT = Math.pow(2 / (HEAT_CAPACITY_RATIO + 1), HEAT_CAPACITY_RATIO / (HEAT_CAPACITY_RATIO - 1));
    public static final double CHOKED_FLOW_RATIO = Math.sqrt(HEAT_CAPACITY_RATIO) * Math.pow(2 / (HEAT_CAPACITY_RATIO + 1), (HEAT_CAPACITY_RATIO + 1) / (2 * (HEAT_CAPACITY_RATIO - 1)));
    public static final double ATMOSPHERIC_PRESSURE = 101325; // Assume atmospheric pressure of 1013.25hPa

    // This method calculates the mass flow rate
    private static double flowRate(double p0, double p1, double t0) {
        assert p0 >= p1 : "Flow rate assumes p0 > p1, please make sure that's the case";
        if(p0 == 0)
            return 0;

        final double pressureRatio = p1 / p0;
        double flowRate;
        if(pressureRatio < CHOKED_FLOW_LIMIT) {
            flowRate = CHOKED_FLOW_RATIO / Math.sqrt(GAS_CONSTANT * t0);
        } else {
            final double pRatioPower = Math.pow(pressureRatio, 1 / HEAT_CAPACITY_RATIO);
            final double HCR_CONST = (2 * HEAT_CAPACITY_RATIO) / (HEAT_CAPACITY_RATIO - 1);
            flowRate = pRatioPower * (pRatioPower - pressureRatio);
            if(flowRate == 0)
                return 0;
            flowRate = Math.sqrt(HCR_CONST * flowRate / (GAS_CONSTANT * t0));
        }

        return flowRate * p0;
    }

    public static void flow(double dT, double flowConstant,
                            @NotNull GasCell cell1, @NotNull GasCell cell2,
                            double crossSection1, double crossSection2,
                            Vector3d direction) {
        final double c1p = cell1.pressure() + cell1.dynamicPressure(direction);
        final double c2p = cell2.pressure() + cell2.dynamicPressure(direction.negate());
        final double c1t = cell1.temperature();
        final double c2t = cell2.temperature();

        if(Double.isNaN(c1p) || Double.isNaN(c2p))
            // Abort if NaN is found
            return;

        // Determine the flow direction
        final GasCell source, sink;
        final double p0, p1, t0, c0, c1;
        final Vector3d actualDir;
        if(c1p >= c2p) {
            source = cell1;
            sink = cell2;
            p0 = c1p;
            p1 = c2p;
            t0 = c1t;
            c0 = crossSection1;
            c1 = crossSection2;
            actualDir = direction;
        } else {
            source = cell2;
            sink = cell1;
            p0 = c2p;
            p1 = c1p;
            t0 = c2t;
            c0 = crossSection2;
            c1 = crossSection1;
            actualDir = direction.negate();
        }

        double flow = dT * flowRate(p0, p1, t0) * flowConstant;
        // Clamp flow at 90% of total source moles
        flow = Math.min(flow, 0.9 * source.getTotalMoles());
        final double flowFraction = flow == 0 ? 0 : flow / source.getTotalMoles();
        final double fractionMass = flowFraction * source.mass();
        final double fractionVolume = flowFraction * source.getVolume();

        if(flow > 0) {
            final double EkMomentum0 = source.momentumKineticEnergy() + sink.momentumKineticEnergy();

            final double EkMole = source.getMoleculeKineticEnergy() / source.getTotalMoles();
            // Take flow out of source...
            source.changeMoles(-flow);
            source.changeEnergy(-EkMole * flow);
            // ...and put it into the sink
            sink.changeMoles(flow);
            sink.changeEnergy(EkMole * flow);

            final Vector3d momentumDelta = source.getMomentum().mul(flowFraction);
            sink.changeMomentum(momentumDelta);
            source.changeMomentum(momentumDelta.negate());

            final double EkMomentum1 = source.momentumKineticEnergy() + sink.momentumKineticEnergy();
            sink.changeEnergy(EkMomentum0 - EkMomentum1);
        }

        final double sinkMass = sink.mass();
        final double sourceMass = source.mass();

        final double sinkSpeedOfSound = sink.speedOfSound();
        final double sourceSpeedOfSound = source.speedOfSound();

        final Vector3d sinkInitialMomentum = new Vector3d(sink.getMomentum());
        final Vector3d sourceInitialMomentum = new Vector3d(source.getMomentum());

        if(c1 > 0) {
            final double sinkFractionVelocity = Math.min(fractionVolume / c1 / dT, sinkSpeedOfSound);
            final double sinkFractionMomentum = sinkFractionVelocity * fractionMass;
            Vector3d sinkMomentum = new Vector3d(sinkFractionMomentum);
            sinkMomentum.mul(actualDir);
            sink.changeMomentum(sinkMomentum);
        }

        if(c0 > 0) {
            final double sourceFractionVelocity = Math.min(fractionVolume / c0 / dT, sourceSpeedOfSound);
            final double sourceFractionMomentum = sourceFractionVelocity * fractionMass;
            Vector3d sourceMomentum = new Vector3d(sourceFractionMomentum);
            sourceMomentum.mul(actualDir);
            source.changeMomentum(sourceMomentum);
        }

        if(sourceMass > 0) {
            Vector3d sourceVelocity0 = new Vector3d();
            Vector3d sourceVelocity1 = new Vector3d();
            sourceInitialMomentum.div(sourceMass, sourceVelocity0);
            source.getMomentum().div(sourceMass, sourceVelocity1);

            final double v0Sum = sourceVelocity0.x * sourceVelocity0.x + sourceVelocity0.y * sourceVelocity0.y + sourceVelocity0.z * sourceVelocity0.z;
            final double v1Sum = sourceVelocity1.x * sourceVelocity1.x + sourceVelocity1.y * sourceVelocity1.y + sourceVelocity1.z * sourceVelocity1.z;

            source.changeEnergy(-0.5 * sourceMass * (v1Sum - v0Sum));
//            source.changeEnergy(-(v1Sum - v0Sum) / (2 * sourceMass));
        }

        if(sinkMass > 0) {
            Vector3d sinkVelocity0 = new Vector3d();
            Vector3d sinkVelocity1 = new Vector3d();
            sinkInitialMomentum.div(sinkMass, sinkVelocity0);
            sink.getMomentum().div(sinkMass, sinkVelocity1);

            final double v0Sum = sinkVelocity0.x * sinkVelocity0.x + sinkVelocity0.y * sinkVelocity0.y + sinkVelocity0.z * sinkVelocity0.z;
            final double v1Sum = sinkVelocity1.x * sinkVelocity1.x + sinkVelocity1.y * sinkVelocity1.y + sinkVelocity1.z * sinkVelocity1.z;

            sink.changeEnergy(-0.5 * sinkMass * (v1Sum - v0Sum));
//            sink.changeEnergy(-(v1Sum - v0Sum) / (2 * sinkMass));
        }

        if(source.getMoleculeKineticEnergy() < 0)
            source.setMoleculeKineticEnergy(0);
        if(sink.getMoleculeKineticEnergy() < 0)
            sink.setMoleculeKineticEnergy(0);
    }
}
