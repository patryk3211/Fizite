package com.patryk3211.fizite.blockentity;

import com.patryk3211.fizite.block.cylinder.PneumaticCylinder;
import com.patryk3211.fizite.simulation.physics.*;
import com.patryk3211.fizite.simulation.physics.simulation.FrictionModel;
import com.patryk3211.fizite.simulation.physics.simulation.IForceGenerator;
import com.patryk3211.fizite.simulation.physics.simulation.IPhysicsStepHandler;
import com.patryk3211.fizite.tiers.ITieredBlock;
import com.patryk3211.fizite.tiers.Material;
import com.patryk3211.fizite.simulation.gas.GasCell;
import com.patryk3211.fizite.simulation.gas.GasSimulator;
import com.patryk3211.fizite.simulation.gas.GasWorldBoundaries;
import com.patryk3211.fizite.simulation.gas.IGasCellProvider;
import com.patryk3211.fizite.simulation.physics.simulation.RigidBody;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.Constraint;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.LockYConstraint;
import com.patryk3211.fizite.utility.IDebugOutput;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3d;

import java.util.function.Function;

public class CylinderEntity extends BlockEntity implements IGasCellProvider, IPhysicsProvider, IDebugOutput, IPhysicsStepHandler, IForceGenerator {
//    private static final NbtKey<Float> NBT_EXTENSION = new NbtKey<>("extension", NbtKey.Type.FLOAT);
    private static final Vector2f PISTON_ANCHOR = new Vector2f(-0.5f, 0);

    // TODO: Temporary values, may change later
    private static float STATIC_FRICTION_COEFFICIENT = 0.5f;
    private static float DYNAMIC_FRICTION_COEFFICIENT = 0.36f;
    private static float BREAKAWAY_VELOCITY = 0.1f;

    private final Material material;
    private final GasCell gasStateCell;

    private final float pistonArea;
    private final float tdcVolume;
    private final float strokeLength;

    private final RigidBody body;
    private final Constraint linearConstraint;

    private static Material getMaterial(BlockState state) {
        final var block = state.getBlock();
        assert block instanceof ITieredBlock;
        final Material mat = ((ITieredBlock) block).getTier(Material.class);
        assert mat != null;
        return mat;
    }

    public CylinderEntity(BlockPos pos, BlockState state) {
        this(pos, state, getMaterial(state));
    }

    public CylinderEntity(BlockPos pos, BlockState state, Material material) {
        super(AllBlockEntities.CYLINDER_ENTITY, pos, state);
        this.material = material;

        assert state.getBlock() instanceof PneumaticCylinder : "A Block using CylinderEntity must be an instance of PneumaticCylinder";

        final PneumaticCylinder cylinder = (PneumaticCylinder) state.getBlock();
        pistonArea = cylinder.getPistonArea();
        strokeLength = cylinder.getStrokeLength();

        final float volume = cylinder.getPistonTopVolume();
        tdcVolume = volume;
        gasStateCell = new GasCell(volume);

        body = new RigidBody();
        linearConstraint = new LockYConstraint(body, 0);

        gasStateCell.set(20000, 5, new Vector3d());
    }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);

        if(!world.isClient) {
            // Connect to the neighbor
            // TODO: Use facing property instead of NORTH
            GasWorldBoundaries.getBoundaries((ServerWorld) world).addBlockEntity(this);
            PhysicsStorage.get(world).addBlockEntity(this);
        } else {
            Networking.sendBlockEntityRequest(pos, world.getRegistryKey());
        }
    }

    private float calculateVolume() {
        final var restPos = body.getRestPosition().x;
        final var currentPos = body.getState().position.x;
        final var chamberLength = restPos - currentPos;
        return (float) (tdcVolume + chamberLength * pistonArea);
    }

    private static double calculateFriction(double deltaTime, double velocity) {
        return -FrictionModel.DEFAULT.calculate(velocity, 0) * Math.signum(velocity);
    }

    @Override
    public void onStepEnd(double deltaTime) {
        gasStateCell.changeVolumeTo(calculateVolume());
    }

    @Override
    public void apply(double deltaTime) {
        double internalPressure = gasStateCell.pressure();
        final double pressureDifference = internalPressure - GasSimulator.ATMOSPHERIC_PRESSURE;

        // Pa = N / m²
        final double force = pressureDifference * pistonArea;
        final double frictionForce = calculateFriction(deltaTime, body.getState().velocity.x);
        if(Double.isNaN(force) || Double.isInfinite(force))
            return;
        body.getState().extForce.x = -force + frictionForce;
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        gasStateCell.deserialize(nbt.get(GasCell.NBT_KEY));
//        extension = nbt.get(NBT_EXTENSION);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.put(GasCell.NBT_KEY, gasStateCell.serialize());
//        nbt.put(NBT_EXTENSION, extension);
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    @Override
    public GasCell getCell(@NotNull Direction dir) {
        // TODO: Use facing property instead of NORTH
        return dir == Direction.NORTH ? gasStateCell : null;
    }

    @Override
    public double getCrossSection(@NotNull Direction dir) {
        return 1;
    }

    @Override
    public double getFlowConstant(@NotNull Direction dir) {
        return 1;
    }

    @Override
    public RigidBody getBody(Direction dir) {
        return body;
    }

    @Override
    public Vector2f getAnchor(Direction dir) {
        return PISTON_ANCHOR;
    }

    @Override
    public PhysicalConnection.ConnectionType getConnectionType(Direction dir) {
        return dir == Direction.SOUTH ? PhysicalConnection.ConnectionType.LINEAR : PhysicalConnection.ConnectionType.NONE;
    }

    @Override
    @NotNull
    public RigidBody[] bodies() {
        return new RigidBody[] { body };
    }

    @Override
    @Nullable
    public Constraint[] internalConstraints() {
        return new Constraint[] { linearConstraint };
    }

//    @Override
//    public void setExternalConstraint(@NotNull Direction dir, @Nullable Constraint constraint) {
//        if(dir == Direction.SOUTH)
//            externalConstraint = constraint;
//    }

    @Override
    public String[] debugInfo() {
        final var rbOrigin = body.getRestPosition();
        final var rbPos = body.getState().position;
        final var rbVel = body.getState().velocity;
        return new String[] {
                "Origin = (" + rbOrigin.x + ", " + rbOrigin.y + ")",
                "Position = (" + rbPos.x + ", " + rbPos.y + ")",
                "Velocity = (" + rbVel.x + ", " + rbVel.y + ")",
                "Pressure = " + gasStateCell.pressure() + "Pa",
                "Temperature = " + gasStateCell.temperature() + "K"
        };
    }
}
