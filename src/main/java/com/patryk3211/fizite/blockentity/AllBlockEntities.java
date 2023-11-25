package com.patryk3211.fizite.blockentity;

import com.patryk3211.fizite.block.AllBlocks;
import io.wispforest.owo.registration.reflect.AutoRegistryContainer;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class AllBlockEntities implements AutoRegistryContainer<BlockEntityType<?>> {
    public static final BlockEntityType<PipeEntity> PIPE_ENTITY = PipeEntity.TEMPLATE.bake();
//    public static final BlockEntityType<PipeEntity> PIPE_ENTITY = FabricBlockEntityTypeBuilder.create(PipeEntity::new, AllBlocks.COPPER_PIPE).build();

    //public static final BlockEntityType<CylinderEntity> CYLINDER_ENTITY = FabricBlockEntityTypeBuilder.create(CylinderEntity::new, AllBlocks.COPPER_CYLINDER).build();

    public static final BlockEntityType<ConnectingRodEntity> CONNECTING_ROD_ENTITY = FabricBlockEntityTypeBuilder.create(ConnectingRodEntity::new, AllBlocks.CONNECTING_ROD).build();

    public static final BlockEntityType<CrankShaftEntity> CRANK_SHAFT_ENTITY = FabricBlockEntityTypeBuilder.create(CrankShaftEntity::new, AllBlocks.CRANK_SHAFT).build();

    public static final BlockEntityType<HandCrankEntity> HAND_CRANK_ENTITY = FabricBlockEntityTypeBuilder.create(HandCrankEntity::new, AllBlocks.HAND_CRANK).build();


    @Override
    public Registry<BlockEntityType<?>> getRegistry() {
        return Registries.BLOCK_ENTITY_TYPE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<BlockEntityType<?>> getTargetFieldType() {
        return (Class<BlockEntityType<?>>) (Object) BlockEntityType.class;
    }
}
