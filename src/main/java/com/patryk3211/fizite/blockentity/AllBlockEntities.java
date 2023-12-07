package com.patryk3211.fizite.blockentity;

import com.patryk3211.fizite.block.AllBlocks;
import io.wispforest.owo.registration.reflect.AutoRegistryContainer;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class AllBlockEntities implements AutoRegistryContainer<BlockEntityType<?>> {
    public static final BlockEntityType<PipeEntity> PIPE_ENTITY = PipeEntity.TEMPLATE.bake();

    public static final BlockEntityType<CylinderEntity> CYLINDER_ENTITY = CylinderEntity.TEMPLATE.bake();

    public static final BlockEntityType<ConnectingRodEntity> CONNECTING_ROD_ENTITY = ConnectingRodEntity.TEMPLATE.bake();

    public static final BlockEntityType<CrankShaftEntity> CRANK_SHAFT_ENTITY = CrankShaftEntity.TEMPLATE.bake();

    public static final BlockEntityType<HandCrankEntity> HAND_CRANK_ENTITY = HandCrankEntity.TEMPLATE.bake();


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
