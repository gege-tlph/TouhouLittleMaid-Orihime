package com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.layer;

import com.github.tartaricacid.simplebedrockmodel.client.bedrock.model.BedrockPart;
import com.github.tartaricacid.touhoulittlemaid.api.backpack.MaidBackpackRenderData;
import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.EntityMaidModel;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.EntityMaidRenderer;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.state.EntityMaidRenderState;
import com.github.tartaricacid.touhoulittlemaid.compat.gun.common.GunClientUtil;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import static com.github.tartaricacid.touhoulittlemaid.entity.backpack.BackpackManager.RENDER_DATA_CACHE;

public class LayerMaidBackItem extends RenderLayer<EntityMaidRenderState, EntityMaidModel> {
    public LayerMaidBackItem(EntityMaidRenderer renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNode, int light, EntityMaidRenderState state, float yRot, float xRot) {
        if (!state.backItem.isEmpty() && state.backpack != null) {
            poseStack.pushPose();
            EntityMaidModel parentModel = this.getParentModel();

            // 依据 root 模型的位移对整体进行物品进行偏移、旋转和缩放
            if (parentModel.root() instanceof BedrockPart part) {
                part.translateAndRotateBedrock(poseStack);
            }

            // 如果有背包，那么和背包适配位移
            if (parentModel.hasBackpackPositioningModel()) {
                BedrockPart renderer = parentModel.getBackpackPositioningModel();
                poseStack.translate(renderer.x * 0.0625, 0.0625 * (renderer.y - 23 + 8), 0.0625 * (renderer.z + 4));
            } else {
                poseStack.translate(0, -0.5, 0.25);
            }

            Identifier id = state.backpack.getId();
            MaidBackpackRenderData renderData = RENDER_DATA_CACHE.apply(id);

            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            poseStack.translate(0, 0.5, -0.25);
            renderData.offsetBackpackItem(poseStack);
            state.backItem.submit(poseStack, submitNode, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);

            poseStack.popPose();
            return;
        }

        // 枪械额外渲染兼容。取的是抽取期存下的原始 ItemStack。
        //
        // ⚠️ 原注释写「枪不带 TOOL 组件，所以 backItem 必然为空」，**那句是错的**：
        // 枪确实带 TOOL，于是 backItem 被填充、上面那支提前 return，
        // 这条枪械分支**一直不可达**——玩家看到的是通用物品渲染，枪模型穿进女仆模型里
        // （2026-08-20 用户实机报出）。抽取期现在显式排除枪械，本分支才真正生效。
        EntityMaid backMaid = state.maid;
        if (backMaid != null) {
            GunClientUtil.renderBackGun(poseStack, submitNode, state.lightCoords,
                    state.backpackShowItem, backMaid);
        }
    }
}
