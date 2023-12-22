package com.patryk3211.fizite.renderer;

import com.patryk3211.fizite.blockentity.ConnectingRodEntity;
import com.patryk3211.fizite.simulation.ClientPhysicsStorage;
import com.patryk3211.fizite.simulation.physics.PhysicsCapability;
import com.patryk3211.fizite.utility.DirectionUtilities;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
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

        final var physicsCap = entity.getCapability(PhysicsCapability.class);
        assert physicsCap != null;
        final var body = physicsCap.body(0);

        final var axis = state.get(Properties.HORIZONTAL_FACING).rotateYClockwise().getAxis();
        final var rotationNormal = DirectionUtilities.getAxisNormal(axis);

        matrices.push();
        final var p1 = ClientPhysicsStorage.get().lerpPos(body, tickDelta);
        var angle = ClientPhysicsStorage.get().lerpAngle(body, tickDelta);

        final double posX; // = 1.0 - Math.abs(p1.x); //ConnectingRodEntity.ORIGIN_X - p1.x;
        if(p1.x > 0) posX =  1.0 - p1.x;
        else         posX = -1.0 - p1.x;

        double x = 0, y = 0, z = 0;
        switch (axis) {
            case X -> {
                z = posX;
                y = p1.y;
            }
            case Z -> {
                x = posX;
                y = -p1.y;
            }
        }

        final var rot = new Quaternionf();
        rot.setAngleAxis(angle, rotationNormal.x(), rotationNormal.y(), rotationNormal.z());
        matrices.translate(x, y, z);
        matrices.multiply(rot, 0.5f, 0.5f, 0.5f);

        renderer.render(matrices.peek(), vertexConsumers.getBuffer(RenderLayers.getEntityBlockLayer(state, false)), state, bakedModel, 0, 0, 0, light, overlay);

        matrices.pop();
    }
}
