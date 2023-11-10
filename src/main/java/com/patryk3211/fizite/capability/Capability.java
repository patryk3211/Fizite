package com.patryk3211.fizite.capability;

import net.minecraft.nbt.NbtCompound;

public abstract class Capability {
    public enum TickOn {
        NONE,
        SERVER,
        CLIENT,
        BOTH
    }

    private final String name;
    private final TickOn tickOn;

    public Capability(String name, TickOn tickOn) {
        this.name = name;
        this.tickOn = tickOn;
    }

    public void readNbt(NbtCompound tag) {

    }

    public NbtCompound writeNbt() {
        return null;
    }
}
