package com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.gecko.layer;

import com.github.tartaricacid.touhoulittlemaid.api.backpack.MaidBackpackRenderData;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.gecko.GeckoMaidRenderData;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.state.EntityMaidRenderState;
import com.github.tartaricacid.touhoulittlemaid.compat.gun.common.GunClientUtil;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.GeoLayerRenderer;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.render.built.GeoLocatorType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import static com.github.tartaricacid.touhoulittlemaid.entity.backpack.BackpackManager.RENDER_DATA_CACHE;

public class GeckoLayerMaidBackItem implements GeoLayerRenderer<EntityMaidRenderState, GeckoMaidRenderData> {
    @Override
    public void submit(SubmitNodeCollector submitNode, PoseStack poseStack, EntityMaidRenderState state,
                       GeckoMaidRenderData data, CameraRenderState camera) {
        if (state.backItem.isEmpty() || state.backpack == null) {
            // 背部枪械渲染。基准是「背包展示物不是 TieredItem 就走枪械分支」，
            // 本树等价的判据是 backItem 为空。
            //
            // 枪为什么必然落到这里：TACZ 的枪**不带 TOOL 组件**（2026-08-21 运行期实测，
            // tacz:modern_kinetic_gun 的 12 个组件里没有 minecraft:tool，对照组
            // diamond_pickaxe 有），而 extractBackpackState 只在 has(TOOL) 时填 backItem。
            //
            // 真正画不画得出来，取决于模型作者有没有给 TAC_PISTOL / TAC_RIFLE 挂点骨骼；
            // 没给就什么都不画，不回落到固定变换——那条兜底已于 2026-08-21 按用户裁决砍掉，
            // 见 GunMaidRender 类注释。
            EntityMaid gunMaid = state.maid;
            if (state.backpack != null && gunMaid != null) {
                GunClientUtil.renderBackGun(state.backpackShowItem, data.modelState, gunMaid,
                        poseStack, submitNode, state.lightCoords);
            }
            return;
        }
        if (data.modelState.locatorGroupSize(GeoLocatorType.BACKPACK) > 0) {
            data.modelState.visitLocatorGroup(GeoLocatorType.BACKPACK, poseStack,
                    locator -> renderBackItem(submitNode, locator, state));
        } else {
            renderBackItem(submitNode, poseStack, state);
        }
    }

    public void renderBackItem(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, EntityMaidRenderState state) {
        assert state.backpack != null;

        Identifier id = state.backpack.getId();
        MaidBackpackRenderData renderData = RENDER_DATA_CACHE.apply(id);

        poseStack.translate(0, 1, 0.25);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.translate(0, 0.5, -0.25);
        renderData.offsetBackpackItem(poseStack);
        state.backItem.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
    }
}
