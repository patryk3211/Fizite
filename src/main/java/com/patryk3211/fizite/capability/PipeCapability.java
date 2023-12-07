package com.patryk3211.fizite.capability;

import com.patryk3211.fizite.utility.DirectionUtilities;
import net.minecraft.block.Block;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PipeCapability extends Capability implements ConnectableCapability.ConnectionHandler {
    private final Direction[] directions;
    private final Class<? extends ConnectableCapability<?>> connectableClass;
    private ConnectableCapability<?> connectableCapability;

    public PipeCapability(Class<? extends ConnectableCapability<?>> connectableClass) {
        super("pipe");
        directions = Direction.values();
        this.connectableClass = connectableClass;
    }

    public PipeCapability(Class<? extends ConnectableCapability<?>> connectableClass, Direction... directions) {
        super("pipe");
        this.directions = directions;
        this.connectableClass = connectableClass;
    }

    public void updateState(Direction dir, boolean value) {
        final var world = entity.getWorld();
        Objects.requireNonNull(world);
        final var prop = DirectionUtilities.asProperty(dir);
        if (!entity.isRemoved() && entity.getCachedState().get(prop) != value) {
            final var newState = entity.getCachedState().with(prop, value);
            entity.getWorld().setBlockState(entity.getPos(), newState, Block.NOTIFY_ALL);
        }
    }

    @Override
    public void initialTick() {
        connectableCapability = entity.getCapability(connectableClass);
        connectableCapability.addHandler(this);

        final var world = entity.getWorld();
        Objects.requireNonNull(world);

        var state = entity.getCachedState();
        for (final var dir : directions) {
            boolean connected = connectableCapability.isConnected(dir);
            state = state.with(DirectionUtilities.asProperty(dir), connected);
            if(connected) {
                final var neighbor = CapabilitiesBlockEntity.getEntity(world, entity.getPos().offset(dir));
                // Since we are connected to this side it is probably safe to assume that the entity is not null
                assert neighbor != null;
                final var pipeCap = neighbor.getCapability(PipeCapability.class);
                if(pipeCap == null)
                    continue;
                pipeCap.updateState(dir.getOpposite(), true);
            }
        }
        world.setBlockState(entity.getPos(), state, Block.NOTIFY_ALL);
    }

    @Override
    public void onConnect(@NotNull Direction dir) {
        if(!entity.isRemoved())
            updateState(dir, true);

        final var world = entity.getWorld();
        Objects.requireNonNull(world);

        final var neighbor = CapabilitiesBlockEntity.getEntity(world, entity.getPos().offset(dir));
        // Since we are connected to this side it is probably safe to assume that the entity is not null
        assert neighbor != null;
        final var pipeCap = neighbor.getCapability(PipeCapability.class);
        if(pipeCap == null || pipeCap.connectableClass.equals(connectableClass))
            return;
        pipeCap.updateState(dir.getOpposite(), true);
    }

    @Override
    public void onDisconnect(@NotNull Direction dir) {
        if(!entity.isRemoved())
            updateState(dir, false);

        final var world = entity.getWorld();
        Objects.requireNonNull(world);

        final var neighbor = CapabilitiesBlockEntity.getEntity(world, entity.getPos().offset(dir));
        // Since we are connected to this side it is probably safe to assume that the entity is not null
        assert neighbor != null;
        final var pipeCap = neighbor.getCapability(PipeCapability.class);
        if(pipeCap == null || !pipeCap.connectableClass.equals(connectableClass))
            return;
        pipeCap.updateState(dir.getOpposite(), false);
    }
}
