package com.patryk3211.fizite.item;

import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;

public interface IWrenchInteraction {
    ActionResult interact(ItemUsageContext context);
}
