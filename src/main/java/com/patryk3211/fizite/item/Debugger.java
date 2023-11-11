package com.patryk3211.fizite.item;

import com.patryk3211.fizite.utility.IDebugOutput;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

public class Debugger extends Item {
    public Debugger() {
        super(new FabricItemSettings());
    }

    private static void print(boolean client, PlayerEntity receiver, String[] lines, String sender) {
        for(final var line : lines) {
            final var changedLine = "[Debugger@" + (client ? "Client" : "Server") + "/" + sender + "] " + line;
            receiver.sendMessage(Text.of(changedLine));
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
