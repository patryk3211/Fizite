package com.patryk3211.fizite.renderer;

import com.patryk3211.fizite.block.cylinder.PneumaticCylinder;
import com.patryk3211.fizite.blockentity.CylinderEntity;
import com.patryk3211.fizite.simulation.ClientPhysicsStorage;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;

public class CylinderRenderer implements BlockEntityRenderer<CylinderEntity> {
    private final BlockRenderManager renderer;

    public CylinderRenderer(BlockEntityRendererFactory.Context ctx) {
        this.renderer = ctx.getRenderManager();
    }

    @Override
    public void render(CylinderEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        final var state = entity.getCachedState().with(PneumaticCylinder.MODEL_PART_PROPERTY, PneumaticCylinder.ModelPart.PISTON);
        final var body = entity.bodies()[0];

//        final var p0 = body.getRestPosition().x;
        final var p1 = ClientPhysicsStorage.get().lerpPos(body, tickDelta).x;

        matrices.push();
        matrices.translate(0, 0, CylinderEntity.ORIGIN_X - p1);
        renderer.renderBlockAsEntity(state, matrices, vertexConsumers, light, overlay);
        matrices.pop();
    }
}
