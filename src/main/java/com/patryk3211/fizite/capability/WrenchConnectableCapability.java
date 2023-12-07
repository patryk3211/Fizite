package com.patryk3211.fizite.capability;

import com.patryk3211.fizite.Fizite;
import com.patryk3211.fizite.item.Wrench;
import com.patryk3211.fizite.utility.CommonStyles;
import com.patryk3211.fizite.utility.Nbt;
import io.wispforest.owo.nbt.NbtKey;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;

import java.util.List;
import java.util.Objects;

public abstract class WrenchConnectableCapability<Z extends WrenchConnectableCapability.InteractionZone, C extends WrenchConnectableCapability<Z, ?>> extends WrenchInteractionCapability {
    private static final NbtKey<String> NBT_CAPABILITY = new NbtKey<>("for", NbtKey.Type.STRING);
    private static final NbtKey<Integer> NBT_ZONE = new NbtKey<>("zone", NbtKey.Type.INT);
    private static final NbtKey<BlockPos> NBT_POSITION = new NbtKey<>("pos", Nbt.Type.BLOCK_POS);

    public static final String MESSAGE_START = "wrench.fizite.connect.start";
    public static final String MESSAGE_SUCCESS = "wrench.fizite.connect.success";
    public static final String MESSAGE_SUCCESS_DISCONNECT = "wrench.fizite.connect.success_disconnect";
    public static final String MESSAGE_FAIL_GENERIC = "wrench.fizite.connect.fail";
    public static final String MESSAGE_FAIL_INCOMPATIBLE = "wrench.fizite.connect.fail_incompatible";
    public static final String MESSAGE_FAIL_ALREADY_CONNECTED = "wrench.fizite.connect.fail_connected";

    public static class InteractionZone {
        public final Box boundingBox;

        public InteractionZone(Box boundingBox) {
            this.boundingBox = boundingBox.expand(VoxelShapes.MIN_SIZE * 2);
        }
    }

    protected static class Connection {
        public WrenchConnectableCapability<InteractionZone, ?> otherCapability;
        public int otherZoneIndex;

        public Connection(WrenchConnectableCapability<InteractionZone, ?> otherCapability, int otherZoneIndex) {
            this.otherCapability = otherCapability;
            this.otherZoneIndex = otherZoneIndex;
        }

        public static <Z extends InteractionZone> Connection of(WrenchConnectableCapability<Z, ?> capability, int zoneIndex) {
            return new Connection((WrenchConnectableCapability<InteractionZone, ?>) capability, zoneIndex);
        }
    }

    private final Class<C> thisClass;
    private final List<Z> zones;
    // Array of connections where the connection index is equivalent to the zone index.
    protected final Connection[] connections;

    public WrenchConnectableCapability(String name, Class<C> thisClass, List<Z> zones) {
        super(name);
        this.thisClass = thisClass;
        this.zones = zones;
        this.connections = new Connection[zones.size()];
    }

    public int zoneIndex(Z zone) {
        for(int i = 0; i < zones.size(); ++i) {
            if(zones.get(i).equals(zone))
                return i;
        }
        return -1;
    }

    @Override
    public InteractionResult interact(Vec3d localHitPos, PlayerEntity player, ItemStack stack, Direction side) {
        Objects.requireNonNull(entity.getWorld());
        final var localPos = transformLocalPos(localHitPos);
        for (int i = 0; i < zones.size(); ++i) {
            final var zone = zones.get(i);
            if (zone.boundingBox.contains(localPos)) {
                // Hit a zone
                if (!player.isSneaking()) {
                    // Player not sneaking, make connections
                    if(connections[i] != null) {
                        // Zone already in use, fail connection
                        return new InteractionResult(ActionResult.CONSUME, Text.translatable(MESSAGE_FAIL_ALREADY_CONNECTED).setStyle(CommonStyles.WHITE));
                    }
                    if (stack.hasNbt()) {
                        // Check if NBT is the start of a connection, if it is,
                        // finish the connection, otherwise display a message
                        // or do something else.
                        final var tag = stack.getNbt();
                        Objects.requireNonNull(tag);
                        if (tag.has(Wrench.NBT_INTERACTION)) {
                            if (tag.get(Wrench.NBT_INTERACTION) != Wrench.InteractionType.WrenchConnect)
                                // Other interaction type active TODO: Print a message informing the player of this
                                return new InteractionResult(ActionResult.FAIL, null);
                            if (!tag.has(NBT_CAPABILITY)) {
                                // No capability name for connection interaction, corrupted state?
                                stack.setNbt(null);
                                Fizite.LOGGER.error("Wrench NBT missing the capability name");
                                return new InteractionResult(ActionResult.FAIL, Text.translatable(MESSAGE_FAIL_GENERIC).setStyle(CommonStyles.RED));
                            }
                            if (!tag.get(NBT_CAPABILITY).equals(name)) {
                                // Capability names don't match, incompatible capabilities
                                return new InteractionResult(ActionResult.FAIL, Text.translatable(MESSAGE_FAIL_INCOMPATIBLE, name, tag.get(NBT_CAPABILITY)).setStyle(CommonStyles.RED));
                            }
                            final var connectPos = tag.get(NBT_POSITION);
                            final var connectEntity = CapabilitiesBlockEntity.getEntity(entity.getWorld(), connectPos);
                            if (connectEntity == null) {
                                // Encoded position doesn't have a block entity, corrupted state?
                                stack.setNbt(null);
                                Fizite.LOGGER.error("Encoded position doesn't have a block entity");
                                return new InteractionResult(ActionResult.FAIL, Text.translatable(MESSAGE_FAIL_GENERIC).setStyle(CommonStyles.RED));
                            }
                            final var connectCap = thisClass.cast(connectEntity.getCapability(thisClass));
                            if (connectCap == null) {
                                // Encoded position doesn't have a capability of this type,
                                // it should because the capability names matched, corrupted state?
                                stack.setNbt(null);
                                Fizite.LOGGER.error("Encoded position doesn't have the specified capability");
                                return new InteractionResult(ActionResult.FAIL, Text.translatable(MESSAGE_FAIL_GENERIC).setStyle(CommonStyles.RED));
                            }
                            // Valid connection state, get zone and fire connection handler
                            final var otherZoneIndex = stack.get(NBT_ZONE);
                            final var otherZone = connectCap.getZone(otherZoneIndex);
                            final var result = connect(zone, connectCap, otherZone);
                            if (result.result == ActionResult.SUCCESS) {
                                // Connection has been made, store it for later use
                                connections[i] = Connection.of(connectCap, otherZoneIndex);
                                connectCap.connections[otherZoneIndex] = Connection.of(this, i);
                                stack.setNbt(null);
                                return new InteractionResult(ActionResult.SUCCESS, Text.translatable(MESSAGE_SUCCESS).setStyle(CommonStyles.GREEN));
                            } else {
                                // Connection was not made,
                                // player should be informed by the connection handler
                                stack.setNbt(null);
                            }
                            return result;
                        } else {
                            // No wrench interaction type, corrupted state?
                            stack.setNbt(null);
                            Fizite.LOGGER.error("Wrench NBT missing interaction type");
                            return new InteractionResult(ActionResult.FAIL, Text.translatable(MESSAGE_FAIL_GENERIC).setStyle(CommonStyles.RED));
                        }
                    } else {
                        // Begin a new connection
                        final var tag = new NbtCompound();
                        // Used for determining the connection interaction for different capabilities
                        tag.put(Wrench.NBT_INTERACTION, Wrench.InteractionType.WrenchConnect);
                        tag.put(NBT_CAPABILITY, name);
                        tag.put(NBT_POSITION, entity.getPos());
                        tag.put(NBT_ZONE, i);
                        stack.setNbt(tag);
                        return new InteractionResult(ActionResult.SUCCESS, Text.translatable(MESSAGE_START).setStyle(CommonStyles.WHITE));
                    }
                } else {
                    // Player sneaking, break connections
                    final var conn = connections[i];
                    if(conn != null) {
                        final var disconnectCap = thisClass.cast(conn.otherCapability);
                        disconnect(zone, disconnectCap, disconnectCap.getZone(conn.otherZoneIndex));
                        disconnectCap.connections[conn.otherZoneIndex] = null;
                        connections[i] = null;
                        return new InteractionResult(ActionResult.SUCCESS, Text.translatable(MESSAGE_SUCCESS_DISCONNECT).setStyle(CommonStyles.GREEN));
                    }
                }
                // Zone didn't do anything
                return new InteractionResult(ActionResult.PASS, null);
            }
        }
        // No zone was hit
        return new InteractionResult(ActionResult.PASS, null);
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

    public Z getZone(int zoneIndex) {
        return zones.get(zoneIndex);
    }

    @Override
    public void onUnload() {
        super.onUnload();
        for(int i = 0; i < zones.size(); ++i) {
            final var conn = connections[i];
            if(conn != null) {
                final var otherCap = thisClass.cast(conn.otherCapability);
                disconnect(zones.get(i), otherCap, otherCap.getZone(conn.otherZoneIndex));
            }
        }
    }

    public abstract InteractionResult connect(Z thisZone, C connectTo, Z otherZone);
    public abstract void disconnect(Z thisZone, C disconnectFrom, Z otherZone);
}
