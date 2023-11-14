package com.patryk3211.fizite.simulation.gas;

import io.wispforest.owo.nbt.NbtKey;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import org.joml.Vector3d;

// Currently we assume a D = 3 (degrees of freedom of particles),
// I'm not sure what kind of effect this will have on our
// simulation but for future me I am leaving this message
// to not forget that.
public class GasCell {
    public static final NbtKey<NbtCompound> NBT_KEY = new NbtKey<>("gas", NbtKey.Type.COMPOUND);

    private static final NbtKey<Double> NBT_TOTAL_MOLE_COUNT = new NbtKey<>("n", NbtKey.Type.DOUBLE);
    private static final NbtKey<Double> NBT_KINETIC_ENERGY = new NbtKey<>("Ek", NbtKey.Type.DOUBLE);
    private static final NbtKey<Double> NBT_MOMENTUM_X = new NbtKey<>("Vx", NbtKey.Type.DOUBLE);
    private static final NbtKey<Double> NBT_MOMENTUM_Y = new NbtKey<>("Vy", NbtKey.Type.DOUBLE);
    private static final NbtKey<Double> NBT_MOMENTUM_Z = new NbtKey<>("Vz", NbtKey.Type.DOUBLE);

    private double kineticEnergy;
    private double totalMoles;
    private double volume;
    private final Vector3d momentum;

    private double molarMass;

    public GasCell(double volume) {
        this.volume = volume;
        this.totalMoles = 0;
        this.kineticEnergy = 0;
        this.momentum = new Vector3d(0);
        this.molarMass = 1;
    }

    public GasCell(double volume, double Ek, double n, Vector3d momentum) {
        this.volume = volume;
        this.kineticEnergy = Ek;
        this.totalMoles = n;
        this.momentum = momentum;
        this.molarMass = 1;
    }

    public void set(double Ek, double n, Vector3d momentum) {
        this.kineticEnergy = Ek;
        this.totalMoles = n;
        this.momentum.set(momentum);
    }

    public NbtCompound serialize() {
        NbtCompound tag = new NbtCompound();
        tag.put(NBT_KINETIC_ENERGY, kineticEnergy);
        tag.put(NBT_TOTAL_MOLE_COUNT, totalMoles);
        tag.put(NBT_MOMENTUM_X, momentum.x);
        tag.put(NBT_MOMENTUM_Y, momentum.y);
        tag.put(NBT_MOMENTUM_Z, momentum.z);
        return tag;
    }

    public void deserialize(NbtCompound tag) {
        momentum.x = tag.get(NBT_MOMENTUM_X);
        momentum.y = tag.get(NBT_MOMENTUM_Y);
        momentum.z = tag.get(NBT_MOMENTUM_Z);
        kineticEnergy = tag.get(NBT_KINETIC_ENERGY);
        totalMoles = tag.get(NBT_TOTAL_MOLE_COUNT);
    }

    public double pressure() {
        if(volume == 0) return 0;
        else return kineticEnergy / (0.5 * 3 * volume);
    }

    public double dynamicPressure(Vector3d direction) {
        if(totalMoles == 0 || kineticEnergy == 0 || volume == 0) return 0;
        final Vector3d dirMmt = momentum.mul(direction);
        final double mass = mass();
        final double v = (dirMmt.x + dirMmt.y + dirMmt.z) / mass;
        final double density = mass / volume;
        final double staticPressure = pressure();

        final double speedOfSoundSqr = staticPressure * GasSimulator.HEAT_CAPACITY_RATIO / density;
        final double machNumberSqr = v * v / speedOfSoundSqr;

        final double x = 1 + ((GasSimulator.HEAT_CAPACITY_RATIO - 1) / 2) * machNumberSqr;
        return staticPressure * Math.sqrt(x * x * x * x * x) - staticPressure;
    }

    public double temperature() {
        if(totalMoles == 0) return 0;
        else return kineticEnergy / (0.5 * 3 * totalMoles * GasSimulator.GAS_CONSTANT);
    }

    public double mass() {
        return totalMoles * molarMass;
    }

    public double speedOfSound() {
        final double density = mass() / volume;
        if(density == 0)
            return 0;
        return Math.sqrt(pressure() * GasSimulator.HEAT_CAPACITY_RATIO / density);
    }

    public double getVolume() {
        return volume;
    }

    public double getMoleculeKineticEnergy() {
        return kineticEnergy;
    }

    public double getTotalMoles() {
        return totalMoles;
    }

    public Vector3d getMomentum() {
        return momentum;
    }

    public double getMolarMass() {
        return molarMass;
    }

    public double momentumKineticEnergy() {
        final double mass = mass();
        if(mass == 0)
            return 0;

        final double vX = momentum.x / mass;
        final double vY = momentum.y / mass;
        final double vZ = momentum.z / mass;
        final double vSqr = vX * vX + vY * vY + vZ * vZ;

        return 0.5 * vSqr * mass;
    }

    public void changeEnergy(double deltaEk) {
        kineticEnergy += deltaEk;
    }

    public void changeMoles(double deltaN) {
        totalMoles += deltaN;
    }

    public double changeVolume(double deltaV) {
        final double edgeLength = Math.cbrt(volume + deltaV);
        final double area = edgeLength * edgeLength; // m²
        final double lengthDelta = deltaV / area; // m³ -> m
        final double work = pressure() * area * -lengthDelta; // Pa * m² -> N * m -> J

        volume += deltaV;
        kineticEnergy += work;
        if(kineticEnergy < 0)
            kineticEnergy = 0;
        return work;
    }

    public double changeVolume(double area, double lengthDelta) {
        final double work = pressure() * area * -lengthDelta; // Pa * m² -> N * m -> J

        volume += area * lengthDelta;
        kineticEnergy += work;
        if(kineticEnergy < 0)
            kineticEnergy = 0;
        return work;
    }

    public void changeTemperature(double deltaT) {
        final double deltaE = 0.5 * 3 * totalMoles * GasSimulator.GAS_CONSTANT * deltaT;
        kineticEnergy += deltaE;
    }

    public void changeMomentum(Vector3d deltaM) {
        momentum.add(deltaM);
    }
}
