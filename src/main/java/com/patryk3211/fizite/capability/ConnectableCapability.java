package com.patryk3211.fizite.capability;

import net.minecraft.util.math.Direction;

public abstract class ConnectableCapability extends Capability {
    private int connectionMask;

    public ConnectableCapability(String name, TickOn tickOn) {
        super(name, tickOn);

        connectionMask = 0;
    }

    public abstract void connect(Direction dir, ConnectableCapability connectTo);
    public abstract void disconnect(Direction dir, ConnectableCapability disconnectFrom);
}
