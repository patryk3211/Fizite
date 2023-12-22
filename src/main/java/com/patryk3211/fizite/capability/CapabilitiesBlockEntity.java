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
    private Map<Class<? extends Capability>, Capability> capabilityLookup;
    private List<Capability> orderedCapabilities;
    final List<Capability> serverTick;
    final List<Capability> clientTick;

    protected CapabilitiesBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        serverTick = new LinkedList<>();
        clientTick = new LinkedList<>();
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

    public final void setCapabilities(List<Capability> orderedCapabilities) {
        this.orderedCapabilities = orderedCapabilities;
        for (Capability cap : orderedCapabilities) {
            switch (cap.tickOn) {
                case SERVER -> serverTick.add(cap);
                case CLIENT -> clientTick.add(cap);
                case BOTH -> {
                    serverTick.add(cap);
                    clientTick.add(cap);
                }
            }
        }
    }

    public final void setCapabilityLookup(Map<Class<? extends Capability>, Capability> lookup) {
        this.capabilityLookup = lookup;
    }

    public <C extends Capability> boolean hasCapability(Class<C> clazz) {
        return capabilityLookup.containsKey(clazz);
    }

    public <C extends Capability> C getCapability(Class<C> clazz) {
        return clazz.cast(capabilityLookup.get(clazz));
    }

    @Override
    public void markRemoved() {
        super.markRemoved();
        orderedCapabilities.forEach(Capability::onUnload);
    }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);
        OneTimeTicker.add(world, this);
        orderedCapabilities.forEach(Capability::onLoad);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        for (Capability capability : orderedCapabilities) {
            final var capNbt = nbt.get(capability.name);
            if(capNbt != null)
                capability.readNbt(capNbt);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        for (Capability capability : orderedCapabilities) {
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
        for (Capability capability : orderedCapabilities) {
            final var capNbt = capability.initialSyncNbt();
            if(capNbt != null)
                tag.put(capability.name, capNbt);
        }
        return tag;
    }

    @Override
    public Text[] debugInfo() {
        List<Text> output = new LinkedList<>();
        for (Capability capability : orderedCapabilities) {
            capability.debugOutput(output);
        }
        return output.toArray(new Text[0]);
    }

    public void initialTick() {
        orderedCapabilities.forEach(Capability::initialTick);
    }
}
