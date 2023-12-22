package com.patryk3211.fizite.utility;

import io.wispforest.owo.nbt.NbtKey;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector2d;
import org.joml.Vector2f;

public class Nbt {
    public static class Type {
        public static final NbtKey.Type<BlockPos> BLOCK_POS = NbtKey.Type.of(NbtElement.INT_ARRAY_TYPE, (nbtCompound, key) -> {
            final var array = nbtCompound.getIntArray(key);
            return new BlockPos(array[0], array[1], array[2]);
        }, (nbtCompound, key, pos) -> nbtCompound.putIntArray(key, new int[]{pos.getX(), pos.getY(), pos.getZ()}));

        public static final NbtKey.Type<Vector2d> VECTOR_2D = NbtKey.Type.of(NbtElement.COMPOUND_TYPE, (nbt, key) -> {
            final var tag = nbt.getCompound(key);
            return new Vector2d(tag.getDouble("x"), tag.getDouble("y"));
        }, (nbt, key, vec) -> {
            final var tag = new NbtCompound();
            tag.put("x", NbtDouble.of(vec.x));
            tag.put("y", NbtDouble.of(vec.y));
            nbt.put(key, tag);
        });
    }
}
