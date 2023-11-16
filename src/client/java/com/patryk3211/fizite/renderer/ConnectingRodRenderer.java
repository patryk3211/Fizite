package com.patryk3211.fizite.renderer;

import com.patryk3211.fizite.blockentity.ConnectingRodEntity;
import com.patryk3211.fizite.simulation.ClientPhysicsStorage;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.AxisAngle4d;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class ConnectingRodRenderer implements BlockEntityRenderer<ConnectingRodEntity> {
    private final BlockRenderManager manager;
    private final BlockModelRenderer renderer;

    public ConnectingRodRenderer(BlockEntityRendererFactory.Context ctx) {
        this.manager = ctx.getRenderManager();
        this.renderer = ctx.getRenderManager().getModelRenderer();
    }

    @Override
    public void render(ConnectingRodEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        final var state = entity.getCachedState();
        final var bakedModel = manager.getModel(state);
        final var body = entity.bodies()[0];

        matrices.push();
        final var p0 = body.getRestPosition();
        final var a0 = body.getRestAngle();
        final var p1 = ClientPhysicsStorage.getInstance().lerpPos(body, tickDelta);
        final var angle = ClientPhysicsStorage.getInstance().lerpAngle(body, tickDelta);
        final var x = p0.x - p1.x;
        final var y = p0.y - p1.y;

        final var rot = new Quaternionf();
        rot.setAngleAxis(a0 - angle, 1.0f, 0.0f, 0.0f);

        matrices.translate(0, y + 0.5, x + 0.5);
        matrices.multiply(rot);

        renderer.render(matrices.peek(), vertexConsumers.getBuffer(RenderLayers.getEntityBlockLayer(state, false)), state, bakedModel, 0, 0, 0, light, overlay);

        matrices.pop();
    }
}
