package com.patryk3211.fizite.capability;

import com.patryk3211.fizite.Fizite;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class CapabilitiesBlockEntityTemplate<T extends CapabilitiesBlockEntity> {
    public interface EntitySupplier<T extends CapabilitiesBlockEntity> {
        T create(BlockEntityType<T> type, BlockPos pos, BlockState state);
    }

    private FabricBlockEntityTypeBuilder<T> typeBuilder;

    private final EntitySupplier<T> supplier;
    private List<CapabilityInfo> capabilities;
    private Map<Class<? extends Capability>, List<Class<? extends Capability>>> bakedMappings;
    private CapabilityInfo lastAddedCap;
    private BlockEntityType<T> entityType;
    private boolean initialClientSync;

    private boolean firstEntity;
    private ServerTicker serverTicker;
    private ClientTicker clientTicker;

    public CapabilitiesBlockEntityTemplate(EntitySupplier<T> supplier) {
        capabilities = new LinkedList<>();
        typeBuilder = FabricBlockEntityTypeBuilder.create(this::create);
        initialClientSync = false;
        firstEntity = true;
        this.supplier = supplier;
    }

    public <C extends Capability> CapabilitiesBlockEntityTemplate<T> with(Supplier<C> supplier) {
        final var cap = new CapabilityInfo.Simple(supplier);
        capabilities.add(cap);
        lastAddedCap = cap;
        return this;
    }

    public <C extends Capability> CapabilitiesBlockEntityTemplate<T> with(Function<BlockState, C> supplier) {
        final var cap = new CapabilityInfo.WithBlockState(supplier);
        capabilities.add(cap);
        lastAddedCap = cap;
        return this;
    }

    public <C extends Capability> CapabilitiesBlockEntityTemplate<T> as(Class<C> clazz) {
        assert lastAddedCap != null : "You must first add a capability before defining it's alias classes";
        lastAddedCap.links.add(clazz);
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
        capabilities = Collections.unmodifiableList(capabilities);
        entityType = typeBuilder.build();
        typeBuilder = null;
        return entityType;
    }

    public boolean doInitialSync() {
        return initialClientSync;
    }

    private static boolean isSuperclass(Class<?> clazz, Class<?> superClazz) {
        do {
            if(clazz == superClazz)
                return true;
            clazz = clazz.getSuperclass();
        } while (clazz != null);
        return false;
    }

    public T create(BlockPos position, BlockState state) {
        final var entity = supplier.create(entityType, position, state);
        entity.setTemplate(this);

        final List<Capability> orderedCapabilities = new LinkedList<>();
        final Map<Class<? extends Capability>, Capability> capabilityLookup = new HashMap<>();
        for (final var capability : capabilities) {
            final var capInstance = capability.instance(state);
            if(capability.thisClass == null)
                capability.thisClass = capInstance.getClass();
            capInstance.setEntity(entity);
            orderedCapabilities.add(capInstance);
            capability.links.forEach(link -> {
                if(!isSuperclass(capInstance.getClass(), link))
                    throw new IllegalStateException("A capability was link to a class which is not it's superclass");
                capabilityLookup.put(link, capInstance);
            });
            capabilityLookup.put(capInstance.getClass(), capInstance);
        }
        entity.setCapabilities(orderedCapabilities);

        if(firstEntity) {
            // First time creating a block entity
            firstEntity = false;
            // Create the tickers
            if(!entity.serverTick.isEmpty())
                serverTicker = new ServerTicker();
            if(!entity.clientTick.isEmpty())
                clientTicker = new ClientTicker();
            // Add capability links for non-ambiguous superclasses
            final Map<Class<? extends Capability>, Class<? extends Capability>> potentialMappings = new HashMap<>();
            final Set<Class<? extends Capability>> ambiguousClasses = new HashSet<>();
            for (Capability cap : orderedCapabilities) {
                for(Class<? extends Capability> clazz = cap.getClass(); clazz != Capability.class; clazz = (Class<? extends Capability>) clazz.getSuperclass()) {
                    if(capabilityLookup.containsKey(clazz))
                        // Lookup already has this class, skip it.
                        continue;
                    if(ambiguousClasses.contains(clazz))
                        // This class was already determined to be ambiguous, skip it.
                        continue;
                    if(potentialMappings.containsKey(clazz)) {
                        // Potential mappings already has a class like this,
                        // which means that it is ambiguous and should not
                        // be added as an implicit link.
                        ambiguousClasses.add(clazz);
                        potentialMappings.remove(clazz);
                        continue;
                    }
                    potentialMappings.put(clazz, cap.getClass());
                }
            }
            // Add mappings to main classes
            potentialMappings.forEach((superClazz, mainClazz) -> {
                for (CapabilityInfo capInfo : capabilities) {
                    if(capInfo.thisClass != mainClazz)
                        continue;
                    capInfo.links.add(superClazz);
                    break;
                }
                // Add the new mappings into this instance's lookup table
                capabilityLookup.put(superClazz, capabilityLookup.get(mainClazz));
            });
        }

        entity.setCapabilityLookup(capabilityLookup);
        return entity;
    }

    public @Nullable <E extends BlockEntity> BlockEntityTicker<E> getTicker(World world, BlockState state, BlockEntityType<E> type) {
        if(!type.equals(entityType)) {
            Fizite.LOGGER.warn("Block entity type mismatch in CapabilitiesBlockEntityTemplate::getTicker()");
            return null;
        }
        return (BlockEntityTicker<E>) (world.isClient ? clientTicker : serverTicker);
    }

    public class ServerTicker implements BlockEntityTicker<T> {
        @Override
        public void tick(World world, BlockPos pos, BlockState state, T blockEntity) {
            blockEntity.serverTick.forEach(Capability::tick);
        }
    }

    public class ClientTicker implements BlockEntityTicker<T> {
        @Override
        public void tick(World world, BlockPos pos, BlockState state, T blockEntity) {
            blockEntity.clientTick.forEach(Capability::tick);
        }
    }
}
