package com.patryk3211.fizite.renderer;

import com.patryk3211.fizite.Fizite;
import com.patryk3211.fizite.FiziteClient;
import com.patryk3211.fizite.block.cylinder.PneumaticCylinder;
import com.patryk3211.fizite.blockentity.CylinderEntity;
import com.patryk3211.fizite.simulation.ClientPhysicsStorage;
import com.patryk3211.fizite.simulation.physics.PhysicsCapability;
import net.fabricmc.fabric.impl.client.model.loading.ModelLoaderHooks;
import net.fabricmc.fabric.impl.client.model.loading.ModelLoadingPluginManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.ResourceManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;

public class CylinderRenderer implements BlockEntityRenderer<CylinderEntity> {
    public static final Identifier PISTON_MODEL = new Identifier(Fizite.MOD_ID, "block/template/piston");

    private final BlockRenderManager manager;
    private final BlockModelRenderer renderer;
    private final BakedModel pistonModel;

    public CylinderRenderer(BlockEntityRendererFactory.Context ctx) {
        this.manager = ctx.getRenderManager();
        this.renderer = manager.getModelRenderer();
        this.pistonModel = manager.getModels().getModelManager().getModel(PISTON_MODEL);
    }

    @Override
    public void render(CylinderEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        final var state = entity.getCachedState();
        final var body = entity.getCapability(PhysicsCapability.class).body(0);
        final var facing = state.get(Properties.FACING);

        final float p1 = (float) ClientPhysicsStorage.get().lerpPos(body, tickDelta).x;

        matrices.push();
        final var quat = new Quaternionf();
        switch(facing) {
            case NORTH -> {}
            case SOUTH -> quat.setAngleAxis(Math.PI, 0, 1, 0);
            case EAST -> quat.setAngleAxis(-Math.PI / 2, 0, 1, 0);
            case WEST -> quat.setAngleAxis(Math.PI / 2, 0, 1, 0);
            case UP -> quat.setAngleAxis(Math.PI / 2, 1, 0, 0);
            case DOWN -> quat.setAngleAxis(-Math.PI / 2, 1, 0, 0);
        }
        matrices.multiply(quat, 0.5f, 0.5f, 0.5f);

        float displacement = 0;
        switch(facing.getDirection()) {
            case NEGATIVE -> displacement =  2.0f - p1;
            case POSITIVE -> displacement =  2.0f + p1;
        }

        matrices.translate(0, 0, displacement);
        renderer.render(matrices.peek(), vertexConsumers.getBuffer(RenderLayers.getEntityBlockLayer(state, false)), state, pistonModel, 0, 0, 0, light, overlay);
        matrices.pop();
    }
}
