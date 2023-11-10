package com.patryk3211.fizite.blockentity;

import com.patryk3211.fizite.simulation.physics.IPhysicsProvider;
import com.patryk3211.fizite.simulation.physics.PhysicalConnection;
import com.patryk3211.fizite.simulation.physics.PhysicsStorage;
import com.patryk3211.fizite.simulation.physics.simulation.RigidBody;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.Constraint;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.PositionConstraint;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

public class CrankShaftEntity extends BlockEntity implements IPhysicsProvider {
    private static final Vector2f rotationAnchor = new Vector2f(0, 0);
    private static final Vector2f xyAnchor = new Vector2f(0.5f, 0);
    private static final Vector2f offset = new Vector2f(1, 0);
    private static final Vector2f zOffset = new Vector2f(0, 0);

    private final RigidBody body;
    private final Constraint positionConstraint;

    public CrankShaftEntity(BlockPos pos, BlockState state) {
        super(AllBlockEntities.CRANK_SHAFT_ENTITY, pos, state);

        body = new RigidBody();
        positionConstraint = new PositionConstraint(body, 0, 0);
    }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);

        if(!world.isClient) {
            PhysicsStorage.get((ServerWorld) world).addBlockEntity(this);
        }
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
    public Vector2f getOffset(Direction dir) {
        return switch(dir) {
            case NORTH -> offset;
            case EAST, WEST -> zOffset;
            default -> null;
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
    public boolean setOrigin(Vector2f newOrigin) {
        return false;
    }
}
