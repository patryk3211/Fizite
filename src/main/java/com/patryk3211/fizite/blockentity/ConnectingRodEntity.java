package com.patryk3211.fizite.blockentity;

import com.patryk3211.fizite.Fizite;
import com.patryk3211.fizite.simulation.physics.IPhysicsProvider;
import com.patryk3211.fizite.simulation.physics.PhysicalConnection;
import com.patryk3211.fizite.simulation.physics.PhysicsStorage;
import com.patryk3211.fizite.simulation.physics.simulation.RigidBody;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.Constraint;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

public class ConnectingRodEntity extends BlockEntity implements IPhysicsProvider {
    private static final Vector2f linearAnchor = new Vector2f(0.5f, 0);
    private static final Vector2f xyAnchor = new Vector2f(-0.5f, 0);

    public static final float ORIGIN_X = 1.0f;

    private final RigidBody body;
    private final Direction facing;

    public ConnectingRodEntity(BlockPos pos, BlockState state) {
        super(AllBlockEntities.CONNECTING_ROD_ENTITY, pos, state);

        body = new RigidBody();
        body.setMarker("Rod");
        body.getState().position.x = ORIGIN_X;
        facing = state.get(Properties.HORIZONTAL_FACING);
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
        return switch (getConnectionType(dir)) {
            case LINEAR_BEARING -> linearAnchor;
            case XY -> xyAnchor;
            default -> null;
        };
    }

    @Override
    @NotNull
    public PhysicalConnection.ConnectionType getConnectionType(Direction dir) {
        if(dir == facing) return PhysicalConnection.ConnectionType.LINEAR_BEARING;
        else if(dir == facing.getOpposite()) return PhysicalConnection.ConnectionType.XY;
        else return PhysicalConnection.ConnectionType.NONE;
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
