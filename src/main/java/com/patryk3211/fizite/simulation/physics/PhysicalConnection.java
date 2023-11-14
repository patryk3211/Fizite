package com.patryk3211.fizite.simulation.physics;

import com.patryk3211.fizite.simulation.physics.simulation.RigidBody;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.BearingConstraint;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.Constraint;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.RotationConstraint;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.WeldConstraint;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.Direction;
import org.joml.Vector2f;

public class PhysicalConnection {
    public enum ConnectionType {
        NONE,
        LINEAR,
        LINEAR_BEARING,
        ROTATIONAL,
        XY
    }

    public static Constraint makeConnection(ConnectionType type1, ConnectionType type2, RigidBody body1, RigidBody body2, Vector2f anchor1, Vector2f anchor2) {
        assert body1.getWorld() == body2.getWorld() : "The connected bodies must reside in the same world";
        assert body1.getWorld() != null : "The connected bodies must have a world assigned to them";

        final ConnectionType type;
        if(type1 == type2) {
            type = type1;
        } else {
            if((type1 == ConnectionType.LINEAR && type2 == ConnectionType.LINEAR_BEARING) ||
               (type1 == ConnectionType.LINEAR_BEARING && type2 == ConnectionType.LINEAR)) {
                type = ConnectionType.LINEAR_BEARING;
            } else {
                // Incompatible connection types
                return null;
            }
        }
        if(type == ConnectionType.NONE) {
            // Cannot connect if connection type is NONE
            return null;
        }

        return switch(type) {
            case LINEAR -> new WeldConstraint(body1, body2, anchor1.x, anchor1.y, anchor2.x, anchor2.y);
            case LINEAR_BEARING, XY -> new BearingConstraint(body1, body2, anchor1.x, anchor1.y, anchor2.x, anchor2.y);
            case ROTATIONAL -> new RotationConstraint(body1, body2);
            default -> throw new UnsupportedOperationException("This connection type is currently unimplemented");
        };
    }

    public static Constraint makeConnection(Direction dir, IPhysicsProvider base, IPhysicsProvider neighbor) {
        final var oDir = dir.getOpposite();
        return makeConnection(
                base.getConnectionType(dir), neighbor.getConnectionType(oDir),
                base.getBody(dir), neighbor.getBody(oDir),
                base.getAnchor(dir), neighbor.getAnchor(oDir));
    }

    public static Constraint makeConnection(BlockEntity entity, Direction direction) {
        assert entity instanceof IPhysicsProvider : "The base block entity is expected to be an IPhysicsProvider";
        assert entity.getWorld() != null : "You should call this method after the block entity has received a world";

        final var provider = (IPhysicsProvider) entity;
        if(provider.getConnectionType(direction) == ConnectionType.NONE)
            return null;

        // Get the neighbor entity
        final var neighbor = entity.getWorld().getBlockEntity(entity.getPos().offset(direction));
        if(!(neighbor instanceof final IPhysicsProvider neighborProvider))
            return null;
        // Connection type must not be NONE
        if(neighborProvider.getConnectionType(direction.getOpposite()) == ConnectionType.NONE)
            return null;

        return makeConnection(
                provider.getConnectionType(direction), neighborProvider.getConnectionType(direction.getOpposite()),
                provider.getBody(direction), neighborProvider.getBody(direction.getOpposite()),
                provider.getAnchor(direction), neighborProvider.getAnchor(direction.getOpposite()));
    }
}
