package com.patryk3211.fizite.capability;

import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class Capability {
    public enum TickOn {
        NONE,
        SERVER,
        CLIENT,
        BOTH
    }

    public final String name;
    public TickOn tickOn;
    protected CapabilitiesBlockEntity entity;

    public Capability(String name) {
        this.name = name;
        this.tickOn = TickOn.NONE;
    }

    /**
     * Defines which side this capability should tick on
     * @param tickOn Selected sides
     */
    public void tickOn(TickOn tickOn) {
        this.tickOn = tickOn;
    }

    /**
     * This method gets called during the capability initialization,
     * it is not yet safe to access the entity's capability list.
     * @param entity Owner of this capability
     */
    public void setEntity(CapabilitiesBlockEntity entity) {
        this.entity = entity;
    }

    /**
     * Gets the owner block entity.
     * @return Capability Block Entity
     */
    public final CapabilitiesBlockEntity getEntity() {
        return entity;
    }

    /**
     * Gets called after the owner entity receives is loaded into a world
     * (BlockEntity::setWorld(World) gets called).
     */
    public void onLoad() {

    }

    /**
     * Gets called after the owner entity is removed from a world
     * (World::removeBlockEntity gets called).
     */
    public void onUnload() {

    }

    /**
     * This method gets called at the first world tick after
     * this entity was created.
     */
    public void initialTick() {

    }

    /**
     * This method gets called every tick on the sides
     * defined previously
     */
    public void tick() {

    }

    /**
     * Gets called when a tag with this capability's name is read
     * by the owner entity.
     * @param tag Nbt tag previously encoded by Capability::writeNbt() or Capability::initialSyncNbt()
     */
    public void readNbt(@NotNull NbtElement tag) {

    }

    /**
     * Called after debug data is requested from the owner block entity
     * (most likely via the Debugger item).
     * @param output List of Text objects, each entry represents a line
     *               written to the requester.
     */
    public void debugOutput(List<Text> output) {

    }

    /**
     * Gets called when the owner entity is requested to
     * save it's nbt state.
     * @return Nbt element
     */
    public NbtElement writeNbt() {
        return null;
    }

    /**
     * Gets called when the owner entity is requested to prepare
     * the initial state data for a client.
     * @return Nbt element
     */
    public NbtElement initialSyncNbt() {
        return writeNbt();
    }
}
