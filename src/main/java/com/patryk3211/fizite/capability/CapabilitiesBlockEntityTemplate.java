package com.patryk3211.fizite.capability;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.Supplier;

public class CapabilitiesBlockEntityTemplate<T extends CapabilitiesBlockEntity> {
    public interface EntitySupplier<T extends CapabilitiesBlockEntity> {
        T create(BlockEntityType<T> type, BlockPos pos, BlockState state);
    }

    private FabricBlockEntityTypeBuilder<T> typeBuilder;

    private final EntitySupplier<T> supplier;
//    private Set<Class<? extends Capability>> capabilities;
    private Set<Supplier<? extends Capability>> capabilities;
    private BlockEntityType<T> entityType;

    public CapabilitiesBlockEntityTemplate(EntitySupplier<T> supplier) {
        capabilities = new HashSet<>();
        typeBuilder = FabricBlockEntityTypeBuilder.create(this::create);
        this.supplier = supplier;
    }

    private static boolean constructorValid(Class<?> clazz) {
        for (Constructor<?> constructor : clazz.getConstructors()) {
            if(constructor.getParameterCount() == 0)
                return true;
        }
        return false;
    }

    public <C extends Capability> CapabilitiesBlockEntityTemplate<T> with(Supplier<C> supplier) {
//        if(!constructorValid(clazz))
//            throw new IllegalArgumentException("Capabilities must provide a 0 argument constructor, " + clazz + " doesn't");
        capabilities.add(supplier);
        return this;
    }

    public CapabilitiesBlockEntityTemplate<T> forBlock(Block block) {
        typeBuilder.addBlock(block);
        return this;
    }

    public CapabilitiesBlockEntityTemplate<T> forBlocks(Block... blocks) {
        typeBuilder.addBlocks(blocks);
        return this;
    }

    public BlockEntityType<T> bake() {
        capabilities = Collections.unmodifiableSet(capabilities);//ImmutableSet.copyOf(capabilities);
        entityType = typeBuilder.build();
        typeBuilder = null;
        return entityType;
    }

    public T create(BlockPos position, BlockState state) {
        final var entity = supplier.create(entityType, position, state);
        entity.setTemplate(this);

        final Map<Class<? extends Capability>, Capability> capabilities = new HashMap<>();
        for (final var capability : this.capabilities) {
            final var capInstance = capability.get();
            capInstance.setEntity(entity);
            capabilities.put(capInstance.getClass(), capInstance);
//            try {
//                final var capInstance = capability.getConstructor().newInstance();
//                capInstance.setEntity(entity);
//                capabilities.put(capability, capInstance);
//            } catch (Exception e) {
//                throw new RuntimeException("Capability doesn't provide the required 0 argument constructor");
//            }
        }
        entity.setCapabilities(capabilities);

        return entity;
    }
//
//    public <C extends Capability> boolean hasCapability(Class<C> clazz) {
//        return capabilities.contains(clazz);
//    }
}
