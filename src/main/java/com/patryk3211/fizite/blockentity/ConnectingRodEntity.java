package com.patryk3211.fizite.blockentity;

import com.patryk3211.fizite.simulation.physics.IPhysicsProvider;
import com.patryk3211.fizite.simulation.physics.PhysicalConnection;
import com.patryk3211.fizite.simulation.physics.PhysicsStorage;
import com.patryk3211.fizite.simulation.physics.simulation.RigidBody;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.Constraint;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

public class ConnectingRodEntity extends BlockEntity implements IPhysicsProvider {
    private static final Vector2f linearAnchor = new Vector2f(0.5f, 0);
    private static final Vector2f xyAnchor = new Vector2f(-0.5f, 0);

    private final RigidBody body;

    public ConnectingRodEntity(BlockPos pos, BlockState state) {
        super(AllBlockEntities.CONNECTING_ROD_ENTITY, pos, state);

        body = new RigidBody();
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
        return switch(dir) {
            case NORTH -> linearAnchor;
            case SOUTH -> xyAnchor;
            default -> null;
        };
    }

    @Override
    public PhysicalConnection.ConnectionType getConnectionType(Direction dir) {
        return switch(dir) {
            case NORTH -> PhysicalConnection.ConnectionType.LINEAR_BEARING;
            case SOUTH -> PhysicalConnection.ConnectionType.XY;
            default -> PhysicalConnection.ConnectionType.NONE;
        };
    }

    @Override
    @NotNull
    public RigidBody[] bodies() {
        return new RigidBody[] { body };
    }

    @Override
    @Nullable
    public Constraint[] internalConstraints() {
        return null;
    }
}
