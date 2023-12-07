package com.patryk3211.fizite.capability;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public abstract class WrenchInteractionCapability extends Capability {
    public static class InteractionResult {
        public ActionResult result;
        public Text message;

        public InteractionResult(ActionResult result, Text message) {
            this.result = result;
            this.message = message;
        }
    }

    public WrenchInteractionCapability(String name) {
        super(name);
    }

    public abstract InteractionResult interact(Vec3d localHitPos, PlayerEntity player, ItemStack stack, Direction side);
}
