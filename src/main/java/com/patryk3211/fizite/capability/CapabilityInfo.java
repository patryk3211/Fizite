package com.patryk3211.fizite.capability;

import net.minecraft.block.BlockState;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class CapabilityInfo {
    public Class<? extends Capability> thisClass;
    public final Set<Class<? extends Capability>> links;
    protected CapabilityInfo() {
        this.links = new HashSet<>();
    }

    public abstract Capability instance(BlockState state);

    public static class Simple extends CapabilityInfo {
        private final Supplier<? extends Capability> supplier;
        public Simple(Supplier<? extends Capability> supplier) {
            this.supplier = supplier;
        }

        @Override
        public Capability instance(BlockState state) {
            return supplier.get();
        }
    }

    public static class WithBlockState extends CapabilityInfo {
        private final Function<net.minecraft.block.BlockState, ? extends Capability> supplier;
        public WithBlockState(Function<net.minecraft.block.BlockState, ? extends Capability> supplier) {
            this.supplier = supplier;
        }

        @Override
        public Capability instance(BlockState state) {
            return supplier.apply(state);
        }
    }
}
