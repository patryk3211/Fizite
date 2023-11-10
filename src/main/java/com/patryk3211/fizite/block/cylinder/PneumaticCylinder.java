package com.patryk3211.fizite.block.cylinder;

import com.patryk3211.fizite.block.ModdedBlock;
import com.patryk3211.fizite.tiers.ITier;
import com.patryk3211.fizite.tiers.ITieredBlock;
import com.patryk3211.fizite.tiers.Material;
import com.patryk3211.fizite.blockentity.AllBlockEntities;
import com.patryk3211.fizite.blockentity.CylinderEntity;
import com.patryk3211.fizite.simulation.gas.GasWorldBoundaries;
import com.patryk3211.fizite.simulation.physics.PhysicsStorage;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public abstract class PneumaticCylinder extends ModdedBlock implements BlockEntityProvider, ITieredBlock {
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
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CylinderEntity(pos, state, material);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ?
                null :
                (w, p, s, t) -> {
                    if(type != AllBlockEntities.CYLINDER_ENTITY)
                        throw new IllegalStateException("Cylinder ticker called for non cylinder entity");
                    CylinderEntity.serverTick(w, p, s, (CylinderEntity) t);
                };
    }

    @Override
    protected void onBlockRemoved(BlockState state, World world, BlockPos pos) {
        if(world instanceof final ServerWorld serverWorld) {
            final GasWorldBoundaries boundaries = GasWorldBoundaries.getBoundaries(serverWorld);
            boundaries.removeBoundaries(pos);
            PhysicsStorage.get(serverWorld).clearPosition(pos);
        }
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
    public <T extends ITier> T getTier(Class<T> clazz) {
        if(clazz == Material.class)
            return (T) Material.COPPER;
        return null;
    }
}
