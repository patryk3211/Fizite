package com.patryk3211.fizite.item;

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
        final var entity = world.getBlockEntity(context.getBlockPos());
        if(entity instanceof final IWrenchInteraction interaction) {
            return interaction.interact(context);
        }
        return super.useOnBlock(context);
    }
}
