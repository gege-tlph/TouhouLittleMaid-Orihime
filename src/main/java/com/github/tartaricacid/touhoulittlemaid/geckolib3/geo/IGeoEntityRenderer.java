package com.github.tartaricacid.touhoulittlemaid.geckolib3.geo;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public interface IGeoEntityRenderer<S extends EntityRenderState> {
    IGeoEntity getGeoEntity(S state);

    void addGeoLayerRenderer(GeoLayerRenderer<?, ?> layerRenderer);

    void geoRender(S state, float entityYaw, float partialTick, PoseStack poseStack, SubmitNodeCollector bufferSource, int packedLight);
}
