package com.patryk3211.fizite.simulation.physics;

import com.patryk3211.fizite.capability.WrenchConnectableCapability;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Box;
import org.joml.Vector2f;

import java.util.function.Function;

public abstract class PhysicsZone extends WrenchConnectableCapability.InteractionZone {
    public record ConnectionInfo(Vector2f anchor, MovementType movementType, int bodyIndex) { }

    private static final class Simple extends PhysicsZone {
        private final ConnectionInfo info;

        public Simple(Box boundingBox, ConnectionInfo info) {
            super(boundingBox);
            this.info = info;
        }

        @Override
        public ConnectionInfo getConnectionInfo(PhysicsCapability capability) {
            return info;
        }
    }

    private static final class FromState extends PhysicsZone {
        private final Function<BlockState, ConnectionInfo> transformer;

        public FromState(Box boundingBox, Function<BlockState, ConnectionInfo> transformer) {
            super(boundingBox);
            this.transformer = transformer;
        }

        @Override
        public ConnectionInfo getConnectionInfo(PhysicsCapability capability) {
            return transformer.apply(capability.getEntity().getCachedState());
        }
    }

    public PhysicsZone(Box boundingBox) {
        super(boundingBox);
    }
    public abstract ConnectionInfo getConnectionInfo(PhysicsCapability capability);

    public static PhysicsZone of(Box boundingBox, Vector2f anchor, MovementType movementType, int bodyIndex) {
        return new Simple(boundingBox, new ConnectionInfo(anchor, movementType, bodyIndex));
    }

    public static PhysicsZone of(Box boundingBox, float anchorX, float anchorY, MovementType movementType, int bodyIndex) {
        return new Simple(boundingBox, new ConnectionInfo(new Vector2f(anchorX, anchorY), movementType, bodyIndex));
    }

    public static PhysicsZone of(Box boundingBox, Function<BlockState, ConnectionInfo> infoMaker) {
        return new FromState(boundingBox, infoMaker);
    }
}
