package com.patryk3211.fizite.item;

import com.patryk3211.fizite.capability.CapabilitiesBlockEntity;
import com.patryk3211.fizite.capability.WrenchInteractionCapability;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;

public class Wrench extends Item {
    public Wrench() {
        super(new FabricItemSettings());
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        final var world = context.getWorld();
        final var blockPos = context.getBlockPos();
        final var entity = CapabilitiesBlockEntity.getEntity(world, blockPos);
        if(entity != null) {
            final var cap = entity.getCapability(WrenchInteractionCapability.class);
            if(cap != null) {
                final var localPos = context.getHitPos().subtract(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                return cap.interact(localPos, context.getPlayer(), context.getStack(), context.getSide());
            }
        }
        return super.useOnBlock(context);
    }
}
