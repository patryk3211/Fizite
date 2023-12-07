package com.patryk3211.fizite.item;

import com.patryk3211.fizite.capability.CapabilitiesBlockEntity;
import com.patryk3211.fizite.capability.WrenchInteractionCapability;
import io.wispforest.owo.nbt.NbtKey;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtString;
import net.minecraft.nbt.NbtTypes;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class Wrench extends Item {
    public enum InteractionType implements StringIdentifiable {
        WrenchConnect("connect");

        public final String name;

        InteractionType(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return name;
        }

        public static InteractionType fromString(String value) {
            return switch(value) {
                case "connect" -> WrenchConnect;
                default -> throw new IllegalArgumentException("String value '" + value + "' doesn't correspond to any interaction type");
            };
        }
    }

    public static final NbtKey.Type<InteractionType> NBT_TYPE_INTERACTION = NbtKey.Type.of(NbtElement.STRING_TYPE, (tag, key) -> {
        final var value = tag.getString(key);
        return InteractionType.fromString(value);
    }, (tag, key, value) -> {
        tag.put(key, NbtString.of(value.asString()));
    });
    public static final NbtKey<InteractionType> NBT_INTERACTION = new NbtKey<>("interaction_type", NBT_TYPE_INTERACTION);

    public Wrench() {
        super(new FabricItemSettings()
                .maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if(user.isSneaking()) {
            final var stack = user.getStackInHand(hand).copy();
            if(stack.hasNbt()) {
                stack.setNbt(null);
                return TypedActionResult.success(stack);
            }
        }
        return super.use(world, user, hand);
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
                final var result = cap.interact(localPos, context.getPlayer(), context.getStack(), context.getSide());
                if(context.getPlayer() != null && result.message != null)
                    context.getPlayer().sendMessage(result.message, true);
                return result.result;
            }
        }
        return super.useOnBlock(context);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return stack.hasNbt();
    }
}
