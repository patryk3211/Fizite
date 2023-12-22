package com.patryk3211.fizite;

import com.patryk3211.fizite.renderer.CylinderRenderer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;

public class AdditionalModelLoader implements ModelLoadingPlugin {
    @Override
    public void onInitializeModelLoader(Context ctx) {
        ctx.addModels(CylinderRenderer.PISTON_MODEL);
    }
}
