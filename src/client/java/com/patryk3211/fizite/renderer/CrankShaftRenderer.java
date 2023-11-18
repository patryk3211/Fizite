package com.patryk3211.fizite.renderer;

import com.patryk3211.fizite.block.CrankShaft;
import com.patryk3211.fizite.blockentity.CrankShaftEntity;
import com.patryk3211.fizite.simulation.ClientPhysicsStorage;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Quaternionf;

public class CrankShaftRenderer implements BlockEntityRenderer<CrankShaftEntity> {
    private final BlockRenderManager renderer;

    public CrankShaftRenderer(BlockEntityRendererFactory.Context ctx) {
        this.renderer = ctx.getRenderManager();
    }

    @Override
    public void render(CrankShaftEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        final var state = entity.getCachedState().with(CrankShaft.MODEL_PART_PROPERTY, CrankShaft.ModelPart.DYNAMIC);
        final var body = entity.bodies()[0];

        final var angle = ClientPhysicsStorage.get().lerpAngle(body, tickDelta);

        matrices.push();
        final var rotation = new Quaternionf();
        rotation.setAngleAxis(angle, -1, 0, 0);
        matrices.multiply(rotation, 0.5f, 0.5f, 0.5f);
        renderer.renderBlockAsEntity(state, matrices, vertexConsumers, light, overlay);
        matrices.pop();
    }
}
