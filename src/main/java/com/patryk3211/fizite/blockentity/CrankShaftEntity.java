package com.patryk3211.fizite.blockentity;

import com.patryk3211.fizite.simulation.physics.IPhysicsProvider;
import com.patryk3211.fizite.simulation.Networking;
import com.patryk3211.fizite.simulation.physics.PhysicalConnection;
import com.patryk3211.fizite.simulation.physics.PhysicsStorage;
import com.patryk3211.fizite.simulation.physics.simulation.RigidBody;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.Constraint;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.PositionConstraint;
import com.patryk3211.fizite.utility.IDebugOutput;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

public class CrankShaftEntity extends BlockEntity implements IPhysicsProvider, IDebugOutput {
    private static final Vector2f rotationAnchor = new Vector2f(0, 0);
    private static final Vector2f xyAnchor = new Vector2f(0.5f, 0);

    private final RigidBody body;
    private final Constraint positionConstraint;

    public CrankShaftEntity(BlockPos pos, BlockState state) {
        super(AllBlockEntities.CRANK_SHAFT_ENTITY, pos, state);

        body = new RigidBody();
        body.setMass(5);
        positionConstraint = new PositionConstraint(body, 0, 0);
    }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);

        PhysicsStorage.get(world).addBlockEntity(this);
    }

    @Override
    public RigidBody getBody(Direction dir) {
        return body;
    }

    @Override
    public Vector2f getAnchor(Direction dir) {
        return switch(getConnectionType(dir)) {
            case XY -> xyAnchor;
            case ROTATIONAL -> rotationAnchor;
            case NONE -> null;
            default -> throw new IllegalStateException("Unknown connection type");
        };
    }

    @Override
    public PhysicalConnection.ConnectionType getConnectionType(Direction dir) {
        return switch(dir) {
            case NORTH -> PhysicalConnection.ConnectionType.XY;
            case EAST, WEST -> PhysicalConnection.ConnectionType.ROTATIONAL;
            default -> PhysicalConnection.ConnectionType.NONE;
        };
    }

    @Override
    public @NotNull RigidBody[] bodies() {
        return new RigidBody[] { body };
    }

    @Override
    public @Nullable Constraint[] internalConstraints() {
        return new Constraint[] { positionConstraint };
    }

    @Override
    public String[] debugInfo() {
        final var state = body.getState();
        return new String[] {
                String.format("Angle = %.3f", state.positionA),
                String.format("Angular Velocity = %.3f", state.velocityA)
        };
    }
}
