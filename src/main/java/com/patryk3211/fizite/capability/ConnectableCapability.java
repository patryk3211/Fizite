package com.patryk3211.fizite.capability;

import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Objects;

public abstract class ConnectableCapability<C extends ConnectableCapability<?>> extends Capability {
    private final int constantMask;
    private int connectionMask;
    private int prevConnectionMask;
    protected final ConnectableCapability<?>[] connections;
    private final Class<C> thisClass;

    private ConnectableCapability(String name, Class<C> thisClass, int constantMask) {
        super(name);
        this.thisClass = thisClass;
        this.constantMask = constantMask;

        connectionMask = 0;
        prevConnectionMask = 0b111111;
        connections = new ConnectableCapability[6];
    }

    private static int computeMask(Direction... allowedDirections) {
        int mask = 0b111111;
        for (Direction allowedDirection : allowedDirections)
            mask &= ~(1 << allowedDirection.getId());
        return mask;
    }

    public ConnectableCapability(String name, Class<C> thisClass) {
        this(name, thisClass, 0);
    }

    public ConnectableCapability(String name, Class<C> thisClass, Direction... allowedDirections) {
        this(name, thisClass, computeMask(allowedDirections));
    }

    private int currentMask() {
        return connectionMask | constantMask;
    }

    public void updateConnections() {
        final var mask = currentMask();
        if(mask != prevConnectionMask) {
            // Update boundaries
            final var diff = mask ^ prevConnectionMask;

            for(final var dir : Direction.values()) {
                final var dirMask = 1 << dir.getId();
                if((diff & dirMask) == 0) {
                    // No change, skip this direction
                    continue;
                }

                final var world = entity.getWorld();
                Objects.requireNonNull(world);

                final var oDir = dir.getOpposite();
                if((mask & dirMask) == 0) {
                    // New side mask enables this side for connections
                    final var neighborPos = entity.getPos().offset(dir);
                    final var neighborCap = getNeighbor(neighborPos);
                    if(neighborCap != null && neighborCap.canConnect(oDir)) {
                        connect(dir, neighborCap);
                        connections[dir.getId()] = neighborCap;
                        neighborCap.connections[oDir.getId()] = this;
                    }
                } else {
                    // New side mask disables this side from connections
                    disconnect(dir, thisClass.cast(connections[dir.getId()]));
                    connections[dir.getId()].connections[oDir.getId()] = null;
                    connections[dir.getId()] = null;
                }
            }

            prevConnectionMask = mask;
        }
    }

    @Override
    public void onLoad() {
        updateConnections();
    }

    @Override
    public void onUnload() {
        for(final var dir : Direction.values()) {
            final var conn = connections[dir.getId()];
            if(conn != null)
                disconnect(dir, thisClass.cast(conn));
        }
    }

    @Override
    public void readNbt(NbtElement tag) {
        assert tag instanceof NbtByte;
        connectionMask = ((NbtByte) tag).byteValue();
    }

    @Override
    public NbtElement writeNbt() {
        return NbtByte.of((byte) connectionMask);
    }

    public boolean canConnect(Direction dir) {
        return (currentMask() & (1 << dir.getId())) == 0;
    }

    public abstract void connect(Direction dir, C connectTo);
    public abstract void disconnect(Direction dir, C disconnectFrom);

    public abstract C getNeighbor(BlockPos pos);
}
