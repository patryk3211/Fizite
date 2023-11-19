package com.patryk3211.fizite.blockentity;

import com.patryk3211.fizite.simulation.physics.IPhysicsProvider;
import com.patryk3211.fizite.simulation.physics.PhysicalConnection;
import com.patryk3211.fizite.simulation.physics.PhysicsStorage;
import com.patryk3211.fizite.simulation.physics.simulation.RigidBody;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.Constraint;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.PositionConstraint;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

public class HandCrankEntity extends BlockEntity implements IPhysicsProvider {
    private static final Vector2f ANCHOR = new Vector2f(0, 0);

    private final Direction facing;

    private final RigidBody body;
    private final Constraint positionConstraint;

    public HandCrankEntity(BlockPos pos, BlockState state) {
        super(AllBlockEntities.HAND_CRANK_ENTITY, pos, state);

        // Facing is determined at placement and cannot change (at least for now)
        facing = state.get(Properties.FACING);

        body = new RigidBody();
        body.setMarker("Shaft");
        body.externalForceReset = true;
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
        return ANCHOR;
    }

    @Override
    @NotNull
    public PhysicalConnection.ConnectionType getConnectionType(Direction dir) {
        return dir == facing ? PhysicalConnection.ConnectionType.ROTATIONAL : PhysicalConnection.ConnectionType.NONE;
    }

    @Override
    @NotNull
    public RigidBody[] bodies() {
        return new RigidBody[] { body };
    }

    @Override
    @Nullable
    public Constraint[] internalConstraints() {
        return new Constraint[] { positionConstraint };
    }
}
