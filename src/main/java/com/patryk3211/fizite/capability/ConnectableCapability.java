package com.patryk3211.fizite.capability;

import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class ConnectableCapability<C extends ConnectableCapability<?>> extends Capability {
    public interface ConnectionHandler {
        void onConnect(@NotNull Direction dir);
        void onDisconnect(@NotNull Direction dir);
    }

    private final int constantMask;
    private int connectionMask;
    private int prevConnectionMask;
    protected final ConnectableCapability<?>[] connections;
    protected int connectionCount;
    private final Class<C> thisClass;
    protected final List<ConnectionHandler> handlers;

    private ConnectableCapability(String name, Class<C> thisClass, int constantMask) {
        super(name);
        this.thisClass = thisClass;
        this.constantMask = constantMask;
        this.handlers = new LinkedList<>();

        connectionMask = 0;
        prevConnectionMask = 0b111111;
        connections = new ConnectableCapability[6];
        connectionCount = 0;
    }

    private static int computeMask(Direction... allowedDirections) {
        int mask = 0b111111;
        for (Direction allowedDirection : allowedDirections)
            mask &= ~(1 << allowedDirection.getId());
        return mask;
    }

    /**
     * Create a connectable capability which allows all sides to be connected.
     * @param name Capability name
     * @param thisClass Class used by the handlers
     */
    public ConnectableCapability(String name, Class<C> thisClass) {
        this(name, thisClass, 0);
    }

    /**
     * Create a connectable capability which allows only the given sides to be connected.
     * @param name Capability name
     * @param thisClass Class used by the handlers
     * @param allowedDirections Directions allowed to connect
     */
    public ConnectableCapability(String name, Class<C> thisClass, Direction... allowedDirections) {
        this(name, thisClass, computeMask(allowedDirections));
    }

    private int currentMask() {
        return connectionMask | constantMask;
    }

    public void addHandler(ConnectionHandler handler) {
        handlers.add(handler);
    }

    public void removeHandler(ConnectionHandler handler) {
        handlers.remove(handler);
    }

    /**
     * Update all connections by checking the mask difference,
     * this can result in new connections being formed as well
     * as current connections being broken.
     */
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
                        if(connect(dir, neighborCap)) {
                            connections[dir.getId()] = neighborCap;
                            neighborCap.connections[oDir.getId()] = this;
                            ++connectionCount;
                            ++neighborCap.connectionCount;
                            handlers.forEach(h -> h.onConnect(dir));
                            neighborCap.handlers.forEach(h -> h.onConnect(oDir));
                        }
                    }
                } else {
                    // New side mask disables this side from connections
                    final var neighbor = connections[dir.getId()];
                    disconnect(dir, thisClass.cast(neighbor));
                    handlers.forEach(h -> h.onDisconnect(dir));
                    neighbor.handlers.forEach(h -> h.onDisconnect(oDir));
                    --neighbor.connectionCount;
                    --connectionCount;
                    neighbor.connections[oDir.getId()] = null;
                    connections[dir.getId()] = null;
                }
            }

            prevConnectionMask = mask;
        }
    }

    @Override
    public void initialTick() {
        updateConnections();
    }

    @Override
    public void onUnload() {
        for(final var dir : Direction.values()) {
            final var conn = connections[dir.getId()];
            if(conn != null) {
                final var oDir = dir.getOpposite();
                disconnect(dir, thisClass.cast(conn));
                handlers.forEach(h -> h.onDisconnect(dir));
                conn.handlers.forEach(h -> h.onDisconnect(oDir));
                --conn.connectionCount;
                --connectionCount;
                connections[dir.getId()] = null;
                conn.connections[oDir.getId()] = null;
            }
        }
    }

    @Override
    public void readNbt(@NotNull NbtElement tag) {
        assert tag instanceof NbtByte;
        connectionMask = ((NbtByte) tag).byteValue();
    }

    @Override
    public NbtElement writeNbt() {
        return NbtByte.of((byte) connectionMask);
    }

    /**
     * Check if the given direction is connectable.
     * (It was defined as connectable and it was not masked)
     * @param dir Direction
     * @return true if connectable, false if not
     */
    public boolean canConnect(Direction dir) {
        return (currentMask() & (1 << dir.getId())) == 0;
    }

    /**
     * Check if the given direction is currently
     * connected to something
     * @param dir Direction
     * @return true if connected, false if not
     */
    public boolean isConnected(Direction dir) {
        return connections[dir.getId()] != null;
    }

    private static class ConnectedIterator implements Iterator<Direction> {
        private final Direction[] directions;
        private int index;

        public ConnectedIterator(ConnectableCapability<?> capability) {
            directions = new Direction[capability.connectionCount];
            int index = 0;
            for(final var dir : Direction.values()) {
                if(capability.connections[dir.getId()] == null)
                    continue;
                directions[index++] = dir;
            }
        }

        @Override
        public boolean hasNext() {
            return index < directions.length;
        }

        @Override
        public Direction next() {
            return directions[index++];
        }
    }

    public Iterator<Direction> connectedDirections() {
        return new ConnectedIterator(this);
    }

    /**
     * Connect handler, called when a new connection is supposed to be formed.
     * @param dir Direction of connection
     * @param connectTo The capability which will is supposed to be connected
     * @return true if the connection was formed, false otherwise
     */
    public abstract boolean connect(Direction dir, C connectTo);

    /**
     * Disconnect handler, called when a connection is supposed to be broken.
     * @param dir Direction to disconnect
     * @param disconnectFrom The capability which has disconnected
     */
    public abstract void disconnect(Direction dir, C disconnectFrom);

    /**
     * Method used to get neighbors of the same capability.
     * @param pos Neighbor position.
     * @return A connectable capability subclass
     */
    public abstract C getNeighbor(BlockPos pos);
}
