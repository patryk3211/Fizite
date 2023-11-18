package com.patryk3211.fizite.simulation.physics;

import com.patryk3211.fizite.simulation.physics.simulation.RigidBody;
import com.patryk3211.fizite.simulation.physics.simulation.constraints.Constraint;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

public interface IPhysicsProvider {
    /* Directional properties */

    /**
     * Get the rigid body for connecting to this side
     * @param dir Direction for connection
     * @return Rigid body or null if connection type on this direction is `NONE`
     */
    RigidBody getBody(Direction dir);

    /**
     * Get the anchor (local position relative to the given body) for connecting with this side
     * @param dir Direction for connection
     * @return Vector or null if connection type on this direction is `NONE`
     */
    Vector2f getAnchor(Direction dir);

    /**
     * Get the connection type to be created on this side
     * @param dir Direction for connection
     * @return Connection type (when connection type is `NONE`, other directional properties can return null)
     */
    @NotNull
    PhysicalConnection.ConnectionType getConnectionType(Direction dir);

    /* General properties */

    /**
     * Get all bodies (internal and external) defined by this physics entity
     * @return Array of rigid bodies
     */
    @NotNull
    RigidBody[] bodies();

    /**
     * Get all internal constraints defined by this physics entity
     * @return Array of constraints or null
     */
    @Nullable
    Constraint[] internalConstraints();

    /* External data hooks (optional) */

    default void setExternalConstraint(@NotNull Direction dir, @Nullable Constraint constraint) { }
}
