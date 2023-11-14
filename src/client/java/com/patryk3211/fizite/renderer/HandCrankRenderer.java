package com.patryk3211.fizite.renderer;

import com.patryk3211.fizite.blockentity.HandCrankEntity;
import com.patryk3211.fizite.simulation.ClientPhysicsStorage;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import org.joml.Quaternionf;

public class HandCrankRenderer implements BlockEntityRenderer<HandCrankEntity> {
    private final BlockRenderManager manager;
    private final BlockModelRenderer renderer;

    public HandCrankRenderer(BlockEntityRendererFactory.Context ctx) {
        this.manager = ctx.getRenderManager();
        this.renderer = ctx.getRenderManager().getModelRenderer();
    }

    @Override
    public void render(HandCrankEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        final var state = entity.getCachedState();
        final var bakedModel = manager.getModel(state);
        final var body = entity.bodies()[0];

        final var normal = state.get(Properties.FACING).getUnitVector();

        matrices.push();
        final var angle = ClientPhysicsStorage.getInstance().lerpPos(body, tickDelta, true).z;

        final var rot = new Quaternionf();
        rot.setAngleAxis(angle, normal.x, normal.y, normal.z);
        matrices.multiply(rot, 0.5f, 0.5f, 0.5f);

        renderer.render(matrices.peek(), vertexConsumers.getBuffer(RenderLayers.getEntityBlockLayer(state, false)), state, bakedModel, 0, 0, 0, light, overlay);

        matrices.pop();
    }
}
