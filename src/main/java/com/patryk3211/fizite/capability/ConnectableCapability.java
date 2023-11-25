package com.patryk3211.fizite.capability;

import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public abstract class ConnectableCapability<C extends ConnectableCapability<?>> extends Capability {
    private int connectionMask;
    private int prevConnectionMask;
    private final ConnectableCapability<?>[] connections;
    private final Class<C> thisClass;

    public ConnectableCapability(String name, Class<C> thisClass) {
        super(name);

        this.thisClass = thisClass;
        connections = new ConnectableCapability[6];
        connectionMask = 0;
        prevConnectionMask = 0b111111;
    }

    public void updateConnections() {
        if(connectionMask != prevConnectionMask) {
            // Update boundaries
            final var diff = connectionMask ^ prevConnectionMask;

            for(final var dir : Direction.values()) {
                final var dirMask = 1 << dir.getId();
                if((diff & dirMask) == 0) {
                    // No change, skip this direction
                    continue;
                }

                final var world = entity.getWorld();
                assert world != null;
                if((connectionMask & dirMask) == 0) {
                    // New side mask enables this side for connections
                    final var neighborPos = entity.getPos().offset(dir);
//                    CapabilitiesBlockEntity.getEntity(world, neighborPos, neighbor -> {
//                        final var cap = neighbor.getCapability(this.getClass());
//                        if(cap.canConnect(dir.getOpposite())) {
//                            if(connect(dir, thisClass.cast(cap)))
//                                connections[dir.getId()] = cap;
//                        }
//                    });
//                    if(neighbor == null)
//                        continue;
//                    if(neighbor.hasCapability(this.getClass())) {
//
//                    }
//                    connect(dir, );
//                    IGasCellProvider.connect(blockEntity, dir);
                } else {
                    // New side mask disables this side from connections
                    disconnect(dir, thisClass.cast(connections[dir.getId()]));
                    connections[dir.getId()] = null;
//                    final var boundaries = GasStorage.get(world);
//                    boundaries.removeBoundary(pos, dir);
                }
            }

            prevConnectionMask = connectionMask;
        }
    }

    @Override
    public void onLoad() {
//        updateConnections();
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
        return (connectionMask & (1 << dir.getId())) == 0;
    }

    public abstract boolean connect(Direction dir, C connectTo);
    public abstract void disconnect(Direction dir, C disconnectFrom);
}
