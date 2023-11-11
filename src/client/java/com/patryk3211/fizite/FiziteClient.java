package com.patryk3211.fizite;

import com.patryk3211.fizite.blockentity.AllBlockEntities;
import com.patryk3211.fizite.renderer.CylinderRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public class FiziteClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		BlockEntityRendererFactories.register(AllBlockEntities.CYLINDER_ENTITY, CylinderRenderer::new);
	}
}