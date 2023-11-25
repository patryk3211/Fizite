package com.patryk3211.fizite.capability;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.function.Function;

public class CapabilitiesBlockEntityTemplate<T extends CapabilitiesBlockEntity> {
    public interface EntitySupplier<T extends CapabilitiesBlockEntity> {
        T create(BlockEntityType<T> type, BlockPos pos, BlockState state);
    }

    private FabricBlockEntityTypeBuilder<T> typeBuilder;

    private final EntitySupplier<T> supplier;
    private Set<Function<BlockState, ? extends Capability>> capabilities;
    private BlockEntityType<T> entityType;
    private boolean initialClientSync;

    public CapabilitiesBlockEntityTemplate(EntitySupplier<T> supplier) {
        capabilities = new HashSet<>();
        typeBuilder = FabricBlockEntityTypeBuilder.create(this::create);
        initialClientSync = false;
        this.supplier = supplier;
    }

    public <C extends Capability> CapabilitiesBlockEntityTemplate<T> with(Function<BlockState, C> supplier) {
        capabilities.add(supplier);
        return this;
    }

    public CapabilitiesBlockEntityTemplate<T> initialClientSync() {
        initialClientSync = true;
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
        capabilities = Collections.unmodifiableSet(capabilities);
        entityType = typeBuilder.build();
        typeBuilder = null;
        return entityType;
    }

    public boolean doInitialSync() {
        return initialClientSync;
    }

    public T create(BlockPos position, BlockState state) {
        final var entity = supplier.create(entityType, position, state);
        entity.setTemplate(this);

        final Map<Class<? extends Capability>, Capability> capabilities = new HashMap<>();
        for (final var capability : this.capabilities) {
            final var capInstance = capability.apply(state);
            capInstance.setEntity(entity);
            var capClass = capInstance.getClass();
            int i = capInstance.deriveSuperclasses();
            do {
                capabilities.put(capClass, capInstance);
                var superClass = capClass.getSuperclass();
                if(superClass == Capability.class)
                    break;
                capClass = (Class<? extends Capability>) superClass;
            } while (i-- > 0);
        }
        entity.setCapabilities(capabilities);

        return entity;
    }
}
