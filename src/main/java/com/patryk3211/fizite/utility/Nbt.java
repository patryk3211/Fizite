package com.patryk3211.fizite.utility;

import io.wispforest.owo.nbt.NbtKey;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.BlockPos;

public class Nbt {
    public static class Type {
        public static final NbtKey.Type<BlockPos> BLOCK_POS = NbtKey.Type.of(NbtElement.INT_ARRAY_TYPE, (nbtCompound, key) -> {
            final var array = nbtCompound.getIntArray(key);
            return new BlockPos(array[0], array[1], array[2]);
        }, (nbtCompound, key, pos) -> nbtCompound.putIntArray(key, new int[]{pos.getX(), pos.getY(), pos.getZ()}));
    }
}
