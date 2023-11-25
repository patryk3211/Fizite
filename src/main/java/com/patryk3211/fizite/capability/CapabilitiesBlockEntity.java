package com.patryk3211.fizite.capability;

import com.patryk3211.fizite.utility.IDebugOutput;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntFunction;

public abstract class CapabilitiesBlockEntity extends BlockEntity implements IDebugOutput {
    private CapabilitiesBlockEntityTemplate<?> template;
    private Map<Class<? extends Capability>, Capability> capabilities;

    protected CapabilitiesBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
//    public static void getEntity(World world, BlockPos pos, Consumer<CapabilitiesBlockEntity> consumer) {
//        if(world.isClient) {
//            consumer.accept(getEntity(world, pos));
//        } else {
//            final var serverWorld = (ServerWorld) world;
////            serverWorld.getChunkManager();
//            //serverWorld.getChunkManager()//.getChunkFutureSyncOnMainThread()
////            serverWorld.getServer()
////            final var chunk = serverWorld.getChunkManager().addTicket();//getWorldChunk(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()));
////            final var entity = chunk.getBlockEntity(pos);
////            if(entity instanceof final CapabilitiesBlockEntity capEntity)
////                consumer.accept(capEntity);
//            final var future = serverWorld.getChunkManager().getChunkFutureSyncOnMainThread(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()), ChunkStatus.FULL, true);
//            future.thenAccept(result -> {
//                final var entity = result.orThrow().getBlockEntity(pos);
//                if(entity instanceof final CapabilitiesBlockEntity capEntity)
//                    consumer.accept(capEntity);
//            });
////            serverWorld.getChunk()
//        }
//    }
//
//    public static CapabilitiesBlockEntity getEntity(World world, BlockPos pos) {
//        final var entity = world.getBlockEntity(pos);
//        if(entity instanceof final CapabilitiesBlockEntity capEntity)
//            return capEntity;
//        return null;
//    }

    public final void setTemplate(CapabilitiesBlockEntityTemplate<?> template) {
        this.template = template;
    }

    public final void setCapabilities(Map<Class<? extends Capability>, Capability> capabilities) {
        this.capabilities = capabilities;
    }

    public <C extends Capability> boolean hasCapability(Class<C> clazz) {
        return capabilities.containsKey(clazz);
    }

    public <C extends Capability> C getCapability(Class<C> clazz) {
        return clazz.cast(capabilities.get(clazz));
    }

    public void onUnload() {
        for (Capability capability : capabilities.values()) {
            capability.onUnload();
        }
    }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);
        for (Capability capability : capabilities.values()) {
            capability.onLoad();
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        for (Capability capability : capabilities.values()) {
            final var capNbt = nbt.get(capability.name);
            if(capNbt != null)
                capability.readNbt(capNbt);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        for (Capability capability : capabilities.values()) {
            final var capNbt = capability.writeNbt();
            if(capNbt != null)
                nbt.put(capability.name, capNbt);
        }
    }

    @Override
    public String[] debugInfo() {
        List<String> output = new LinkedList<>();
        for (Capability capability : capabilities.values()) {
            capability.debugOutput(output);
        }
        return output.toArray(new String[0]);
    }
}
