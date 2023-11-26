package com.patryk3211.fizite.capability;

import com.patryk3211.fizite.Fizite;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;

import java.util.Collection;

public abstract class WrenchConnectableCapability<Z extends WrenchConnectableCapability.InteractionZone, C extends WrenchConnectableCapability<Z, ?>> extends WrenchInteractionCapability {
    public static class InteractionZone {
        public final Box boundingBox;

        public InteractionZone(Box boundingBox) {
            this.boundingBox = boundingBox.expand(VoxelShapes.MIN_SIZE * 2);
        }
    }

    private final Class<C> thisClass;
    private final Collection<Z> zones;

    public WrenchConnectableCapability(String name, Class<C> thisClass, Collection<Z> zones) {
        super(name);
        this.thisClass = thisClass;
        this.zones = zones;
    }

    @Override
    public ActionResult interact(Vec3d localHitPos, PlayerEntity player, ItemStack stack, Direction side) {
        final var localPos = transformLocalPos(localHitPos);
        for(final var zone : zones) {
            if(zone.boundingBox.contains(localPos))
                Fizite.LOGGER.info("Hit zone: " + zone);
        }
        return ActionResult.SUCCESS;
    }

    public Vec3d transformLocalPos(Vec3d worldLocalPos) {
        final var state = entity.getCachedState();
        if(state.contains(Properties.FACING) || state.contains(Properties.HORIZONTAL_FACING)) {
            // Conduct some basic position transformations based on the facing of the block
            Direction facing;
            if(state.contains(Properties.FACING))
                facing = state.get(Properties.FACING);
            else
                facing = state.get(Properties.HORIZONTAL_FACING);
            return switch(facing) {
                case NORTH -> worldLocalPos;
                case SOUTH -> new Vec3d(1 - worldLocalPos.x, worldLocalPos.y, 1 - worldLocalPos.z);
                case WEST -> new Vec3d(1 - worldLocalPos.z, worldLocalPos.y, worldLocalPos.x);
                case EAST -> new Vec3d(worldLocalPos.z, worldLocalPos.y, 1 - worldLocalPos.x);
                case UP -> new Vec3d(worldLocalPos.x, 1 - worldLocalPos.z, worldLocalPos.y);
                case DOWN -> new Vec3d(worldLocalPos.x, worldLocalPos.z, 1 - worldLocalPos.y);
            };
        }
        return worldLocalPos;
    }

    public abstract boolean connect(C connectTo);
    public abstract void disconnect(C disconnectFrom);
}
