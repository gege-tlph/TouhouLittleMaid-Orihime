package com.github.tartaricacid.touhoulittlemaid.geckolib3.geo;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * 第三方模型系统（目前是 YSM）接管女仆本体渲染时要实现的契约。
 * {@code EntityMaidRenderer.YSM_ENTITY_MAID_RENDERER} 持有它的一个实例，
 * {@code ModelType.YSM} 分支直接把 submit 转发给它，与 {@code ModelType.GECKO}
 * 分支转发给 {@code GeckoEntityMaidRenderer} 同构。
 * <p>
 * 与 {@code port/1.21.11-fabric} 分支同名接口的差异：{@code geoRender(state, entityYaw,
 * partialTick, poseStack, bufferSource, packedLight)} 那套立即渲染签名已经不存在——
 * 本分支的渲染管线是 submit-node-collector 式的，改成与 {@code EntityMaidRenderer#submit}
 * 同签名，接管方直接实现「给我 state/poseStack/collector/camera，我自己画完整个身体」。
 */
public interface IGeoEntityRenderer<S extends EntityRenderState> {
    IGeoEntity getGeoEntity(S state);

    void addGeoLayerRenderer(GeoLayerRenderer<?, ?> layerRenderer);

    void submit(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera);
}
