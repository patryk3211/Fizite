package com.patryk3211.fizite.simulation.physics;

import com.patryk3211.fizite.simulation.physics.simulation.RigidBody;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.Constraint;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

public interface IPhysicsProvider {
    /* Directional properties */

    RigidBody getBody(Direction dir);

    Vector2f getAnchor(Direction dir);

    PhysicalConnection.ConnectionType getConnectionType(Direction dir);

    /* General properties */

    @NotNull
    RigidBody[] bodies();

    @Nullable
    Constraint[] internalConstraints();

//    default boolean externalForceReset() {
//        return false;
//    }

    /* External data hooks (optional) */

    default void setExternalConstraint(@NotNull Direction dir, @Nullable Constraint constraint) { }
}
