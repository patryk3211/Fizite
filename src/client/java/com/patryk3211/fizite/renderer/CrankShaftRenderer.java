package com.patryk3211.fizite.renderer;

import com.patryk3211.fizite.block.CrankShaft;
import com.patryk3211.fizite.blockentity.CrankShaftEntity;
import com.patryk3211.fizite.simulation.ClientPhysicsStorage;
import com.patryk3211.fizite.utility.DirectionUtilities;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import org.joml.Quaternionf;

public class CrankShaftRenderer implements BlockEntityRenderer<CrankShaftEntity> {
    private final BlockRenderManager renderer;

    public CrankShaftRenderer(BlockEntityRendererFactory.Context ctx) {
        this.renderer = ctx.getRenderManager();
    }

    @Override
    public void render(CrankShaftEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        matrices.push();
        final var state = entity.getCachedState().with(CrankShaft.MODEL_PART_PROPERTY, CrankShaft.ModelPart.DYNAMIC);
        final var body = entity.bodies()[0];

        final var rotation = new Quaternionf();
        final var normal = DirectionUtilities.getAxisNormal(state.get(Properties.HORIZONTAL_FACING).rotateYClockwise().getAxis());
        final var angle = ClientPhysicsStorage.get().lerpAngle(body, tickDelta);
        rotation.setAngleAxis(angle, normal.x(), normal.y(), normal.z());
        matrices.multiply(rotation, 0.5f, 0.5f, 0.5f);

        renderer.renderBlockAsEntity(state, matrices, vertexConsumers, light, overlay);
        matrices.pop();
    }
}
