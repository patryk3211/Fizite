package com.patryk3211.fizite.renderer;

import com.patryk3211.fizite.blockentity.ConnectingRodEntity;
import com.patryk3211.fizite.simulation.ClientPhysicsStorage;
import com.patryk3211.fizite.utility.DirectionUtilities;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
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

        final var facing = state.get(Properties.HORIZONTAL_FACING).rotateYCounterclockwise();//DirectionUtilities.perpendicular(state.get(Properties.HORIZONTAL_FACING));
        final var rotationNormal = facing.getUnitVector();//DirectionUtilities.getAxisNormal(facing.getAxis());

        matrices.push();
        final var p0 = body.getRestPosition();
        final var p1 = ClientPhysicsStorage.get().lerpPos(body, tickDelta);
        final var relativeX = p0.x - p1.x;
        final var relativeY = p0.y - p1.y;

        final var rot = new Quaternionf();
        final var a0 = body.getRestAngle();
        final var angle = ClientPhysicsStorage.get().lerpAngle(body, tickDelta);
        final var relativeA = a0 - angle;

        double x = 0, y = 0, z = 0, a = 0;
        switch (facing) {
            case EAST -> {
                a = -relativeA;
                y =  relativeY;
                z = -relativeX;
            }
            case WEST -> {
                a =  relativeA;
                y = -relativeY;
                z =  relativeX;
            }
            case SOUTH -> {
                a = -relativeA;
                y =  relativeY;
                x =  relativeX;
            }
            case NORTH -> {
                a =  relativeA;
                y = -relativeY;
                x = -relativeX;
            }
        }
        rot.setAngleAxis(a, rotationNormal.x(), rotationNormal.y(), rotationNormal.z());
        matrices.translate(x, y, z);
        matrices.multiply(rot, 0.5f, 0.5f, 0.5f);

        renderer.render(matrices.peek(), vertexConsumers.getBuffer(RenderLayers.getEntityBlockLayer(state, false)), state, bakedModel, 0, 0, 0, light, overlay);

        matrices.pop();
    }
}
