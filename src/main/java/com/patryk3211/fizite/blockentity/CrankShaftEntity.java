package com.patryk3211.fizite.blockentity;

import com.patryk3211.fizite.simulation.physics.IPhysicsProvider;
import com.patryk3211.fizite.simulation.physics.PhysicalConnection;
import com.patryk3211.fizite.simulation.physics.PhysicsStorage;
import com.patryk3211.fizite.simulation.physics.simulation.RigidBody;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.Constraint;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.PositionConstraint;
import com.patryk3211.fizite.utility.IDebugOutput;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

public class CrankShaftEntity extends BlockEntity implements IPhysicsProvider, IDebugOutput {
    private static final Vector2f rotationAnchor = new Vector2f(0, 0);
    private static final Vector2f xyAnchorPositive = new Vector2f(0.5f, 0);
    private static final Vector2f xyAnchorNegative = new Vector2f(-0.5f, 0);

    private final RigidBody body;
    private final Constraint positionConstraint;
    private final Direction facing;
    private final Direction.Axis rotationAxis;

    public CrankShaftEntity(BlockPos pos, BlockState state) {
        super(AllBlockEntities.CRANK_SHAFT_ENTITY, pos, state);

        body = new RigidBody();
        body.setMass(5);
        body.setMarker("Shaft");
        positionConstraint = new PositionConstraint(body, 0, 0);

        facing = state.get(Properties.HORIZONTAL_FACING);
        rotationAxis = facing.rotateYClockwise().getAxis();
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
            case XY -> dir == facing ? xyAnchorPositive : xyAnchorNegative;
            case ROTATIONAL -> rotationAnchor;
            default -> null;
        };
    }

    @Override
    @NotNull
    public PhysicalConnection.ConnectionType getConnectionType(Direction dir) {
        if(dir == facing || dir == facing.getOpposite()) return PhysicalConnection.ConnectionType.XY;
        else if(dir.getAxis() == rotationAxis) return PhysicalConnection.ConnectionType.ROTATIONAL;
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
        return new Constraint[] { positionConstraint };
    }

    @Override
    public Text[] debugInfo() {
        final var state = body.getState();
        return new Text[] {
                Text.of(String.format("Angle = %.3f", state.positionA)),
                Text.of(String.format("Angular Velocity = %.3f", state.velocityA))
        };
    }
}
