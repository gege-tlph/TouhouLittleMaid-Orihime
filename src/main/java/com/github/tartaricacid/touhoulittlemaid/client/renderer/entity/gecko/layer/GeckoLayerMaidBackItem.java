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
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import static com.github.tartaricacid.touhoulittlemaid.entity.backpack.BackpackManager.RENDER_DATA_CACHE;

public class GeckoLayerMaidBackItem implements GeoLayerRenderer<EntityMaidRenderState, GeckoMaidRenderData> {
    @Override
    public void submit(SubmitNodeCollector submitNode, PoseStack poseStack, EntityMaidRenderState state,
                       GeckoMaidRenderData data, CameraRenderState camera) {
        if (state.backItem.isEmpty() || state.backpack == null) {
            // 背部枪械渲染。基准是「背包展示物不是 TieredItem 就走枪械分支」，
            // 本树等价的判据是 backItem 为空——它只在物品带 TOOL 组件时才被填充，枪不带。
            EntityMaid gunMaid = state.maid;
            if (state.backpack != null && gunMaid != null) {
                GunClientUtil.renderBackGun(state.backpackShowItem, data.locators(), gunMaid,
                        poseStack, submitNode, state.lightCoords);
            }
            return;
        }
        if (data.locators().locatorGroupSize(GeoLocatorType.BACKPACK) > 0) {
            data.locators().visitLocatorGroup(GeoLocatorType.BACKPACK, poseStack,
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
