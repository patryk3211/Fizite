package com.patryk3211.fizite.capability;

import com.patryk3211.fizite.Fizite;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.io.File;
import java.util.LinkedList;
import java.util.Queue;

public class InitialTicker extends PersistentState {
    private static final Type<InitialTicker> TYPE = new Type<>(InitialTicker::new, nbt -> new InitialTicker(), null);
    private static final String ID = Fizite.MOD_ID + ":initial_ticker";

    public static InitialTicker clientTicker;

    private final Queue<CapabilitiesBlockEntity> entities = new LinkedList<>();

    public static void add(World world, CapabilitiesBlockEntity entity) {
        if(!world.isClient) {
            final var serverWorld = (ServerWorld) world;
            final InitialTicker storage = serverWorld.getPersistentStateManager().getOrCreate(TYPE, ID);
            storage.entities.add(entity);
        } else {
            clientTicker.entities.add(entity);
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        return null;
    }

    @Override
    public void save(File file) { }

    public void tickAll() {
        while(!entities.isEmpty()) {
            entities.remove().initialTick();
        }
    }

    public static void onWorldTickStart(ServerWorld world) {
        final InitialTicker ticker = world.getPersistentStateManager().getOrCreate(TYPE, ID);
        ticker.tickAll();
    }
}
