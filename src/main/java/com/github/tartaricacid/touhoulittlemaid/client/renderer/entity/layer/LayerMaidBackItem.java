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
                part.translateAndRotate(poseStack);
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
        // ⚠️ 别再写「枪不带 TOOL 组件所以走不到上面那支」——**枪是带 TOOL 的**。
        // 这条假的事实断言让本分支整整一个版本不可达：枪满足上面的判据 → 被通用物品渲染
        // 画在背上（穿模）→ 那一支 return，这里永远轮不到。抽取期现在显式排除了枪
        // （见 EntityMaidRenderState#extractBackpackState），backItem 才会为空并落到这里。
        EntityMaid backMaid = state.maid;
        if (backMaid != null) {
            GunClientUtil.renderBackGun(poseStack, submitNode, state.lightCoords,
                    state.backpackShowItem, backMaid);
        }
    }
}