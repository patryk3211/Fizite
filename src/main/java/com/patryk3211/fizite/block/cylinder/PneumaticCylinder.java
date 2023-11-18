package com.patryk3211.fizite.block.cylinder;

import com.patryk3211.fizite.block.ModdedBlock;
import com.patryk3211.fizite.simulation.gas.GasStorage;
import com.patryk3211.fizite.tiers.ITier;
import com.patryk3211.fizite.tiers.ITieredBlock;
import com.patryk3211.fizite.tiers.Material;
import com.patryk3211.fizite.blockentity.CylinderEntity;
import com.patryk3211.fizite.simulation.physics.PhysicsStorage;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public abstract class PneumaticCylinder extends ModdedBlock implements BlockEntityProvider, ITieredBlock {
    public enum ModelPart implements StringIdentifiable {
        CYLINDER, PISTON;

        @Override
        public String asString() {
            return switch(this) {
                case CYLINDER -> "cylinder";
                case PISTON -> "piston";
            };
        }
    }
    public static final EnumProperty<ModelPart> MODEL_PART_PROPERTY = EnumProperty.of("part", ModelPart.class);

    private static final VoxelShape BB_NORTH = createCuboidShape(4, 4, 0, 12, 12, 15);

    private final Material material;
    private final float pistonArea;
    private final float strokeLength;
    private final float pistonTopVolume;

    public PneumaticCylinder(Material material, float pistonArea, float strokeLength, float pistonTopVolume) {
        super(FabricBlockSettings.create().strength(7.0f));
        this.material = material;
//        this.maxPressure = maxPressure;
        this.pistonArea = pistonArea;
        this.strokeLength = strokeLength;
        this.pistonTopVolume = pistonTopVolume;

        setDefaultState(getDefaultState()
                .with(MODEL_PART_PROPERTY, ModelPart.CYLINDER));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(MODEL_PART_PROPERTY);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CylinderEntity(pos, state, material);
    }

//    @Nullable
//    @Override
//    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
//        return world.isClient ?
//                null :
//                (w, p, s, t) -> {
//                    assert type == AllBlockEntities.CYLINDER_ENTITY : "Cylinder ticker called for non cylinder entity";
//                    CylinderEntity.serverTick(w, p, s, (CylinderEntity) t);
//                };
//    }

    @Override
    protected void onBlockRemoved(BlockState state, World world, BlockPos pos) {
        if(world instanceof final ServerWorld serverWorld) {
            final GasStorage boundaries = GasStorage.get(serverWorld);
            boundaries.removeBoundaries(pos);
        }
        PhysicsStorage.get(world).clearPosition(pos);
    }

    public float getPistonArea() {
        return pistonArea;
    }

    public float getStrokeLength() {
        return strokeLength;
    }

    public float getPistonTopVolume() {
        return pistonTopVolume;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return BB_NORTH;
    }

    @Override
    public <T extends ITier> T getTier(Class<T> clazz) {
        Object out = null;
        if(clazz == Material.class)
            out = Material.COPPER;
        return clazz.cast(out);
    }
}
