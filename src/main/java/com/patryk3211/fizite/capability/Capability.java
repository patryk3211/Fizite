package com.patryk3211.fizite.capability;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtElement;
import net.minecraft.world.World;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class Capability {
//    public enum TickOn {
//        NONE,
//        SERVER,
//        CLIENT,
//        BOTH
//    }

    public final String name;
    protected CapabilitiesBlockEntity entity;

    public Capability(String name) {
        this.name = name;
    }

    public final void setEntity(CapabilitiesBlockEntity entity) {
        this.entity = entity;
    }

    public void onLoad() {

    }

    public void onUnload() {

    }

    public void readNbt(NbtElement tag) {

    }

    public void debugOutput(List<String> output) {

    }

    public NbtElement writeNbt() {
        return null;
    }
}
