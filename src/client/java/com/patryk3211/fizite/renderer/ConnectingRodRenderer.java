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
        final var body = entity.bodies()[0];

        final var facing = state.get(Properties.HORIZONTAL_FACING).rotateYCounterclockwise();
        final var axisDir = facing.getDirection();
        final var rotationNormal = facing.getUnitVector();

        matrices.push();
        final var p1 = ClientPhysicsStorage.get().lerpPos(body, tickDelta);

        var posY = -p1.y;
        var angle = ClientPhysicsStorage.get().lerpAngle(body, tickDelta);
        if(axisDir == Direction.AxisDirection.NEGATIVE) {
            posY = -posY;
            angle = -angle;
        }

        final var posX = ConnectingRodEntity.ORIGIN_X - p1.x;
        double x = 0, z = 0;
        switch (facing) {
            case EAST ->  z = -posX;
            case WEST ->  z =  posX;
            case SOUTH -> x =  posX;
            case NORTH -> x = -posX;
        }

        final var rot = new Quaternionf();
        rot.setAngleAxis(angle, rotationNormal.x(), rotationNormal.y(), rotationNormal.z());
        matrices.translate(x, posY, z);
        matrices.multiply(rot, 0.5f, 0.5f, 0.5f);

        renderer.render(matrices.peek(), vertexConsumers.getBuffer(RenderLayers.getEntityBlockLayer(state, false)), state, bakedModel, 0, 0, 0, light, overlay);

        matrices.pop();
    }
}
