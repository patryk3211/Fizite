package com.patryk3211.fizite.capability;

import com.patryk3211.fizite.utility.IDebugOutput;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class CapabilitiesBlockEntity extends BlockEntity implements IDebugOutput {
    private CapabilitiesBlockEntityTemplate<?> template;
    private Map<Class<? extends Capability>, Capability> capabilities;

    protected CapabilitiesBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static CapabilitiesBlockEntity getEntity(World world, BlockPos pos) {
        final var entity = world.getBlockEntity(pos);
        if(entity instanceof final CapabilitiesBlockEntity capEntity)
            return capEntity;
        return null;
    }

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

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return template.doInitialSync() ? BlockEntityUpdateS2CPacket.create(this) : null;
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        final var tag = new NbtCompound();
        super.writeNbt(tag);
        for (Capability capability : capabilities.values()) {
            final var capNbt = capability.initialSyncNbt();
            if(capNbt != null)
                tag.put(capability.name, capNbt);
        }
        return tag;
    }

    @Override
    public Text[] debugInfo() {
        List<Text> output = new LinkedList<>();
        Set<Capability> logged = new HashSet<>();
        for (Capability capability : capabilities.values()) {
            if(logged.add(capability))
                capability.debugOutput(output);
        }
        return output.toArray(new Text[0]);
    }
}
