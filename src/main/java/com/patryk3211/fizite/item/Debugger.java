package com.patryk3211.fizite.item;

import com.patryk3211.fizite.utility.IDebugOutput;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class Debugger extends Item {
    public Debugger() {
        super(new FabricItemSettings()
                .maxCount(1));
    }

    private static void print(boolean client, PlayerEntity receiver, Text[] lines, String sender) {
        for(final var line : lines) {
            var result = Text.empty().setStyle(Style.EMPTY.withColor(Formatting.GRAY));
            var header = Text.empty().setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY));
            header.append("[Dbg@");
            header.append(client ?
                    Text.literal("C").setStyle(Style.EMPTY.withColor(Formatting.GREEN)) :
                    Text.literal("S").setStyle(Style.EMPTY.withColor(Formatting.YELLOW)));
            header.append("/");
            header.append(Text.literal(sender).setStyle(Style.EMPTY.withColor(Formatting.BLUE)));
            header.append("] ");
            result.append(header);
            result.append(line);
            receiver.sendMessage(result);
        }
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        final var world = context.getWorld();
        final var state = world.getBlockState(context.getBlockPos());
        final var block = state.getBlock();
        if(block instanceof final IDebugOutput printer) {
            final var lines = printer.debugInfo();
            print(world.isClient, context.getPlayer(), lines, "Block");
        }
        final var blockEntity = world.getBlockEntity(context.getBlockPos());
        if(blockEntity instanceof final IDebugOutput printer) {
            final var lines = printer.debugInfo();
            print(world.isClient, context.getPlayer(), lines, "BlockEntity");
        }
        return super.useOnBlock(context);
    }
}
