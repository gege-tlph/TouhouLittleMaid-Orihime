package com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.layer;

import com.github.tartaricacid.simplebedrockmodel.client.bedrock.model.BedrockPart;
import com.github.tartaricacid.touhoulittlemaid.api.backpack.MaidBackpackRenderData;
import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.EntityMaidModel;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.EntityMaidRenderer;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.state.EntityMaidRenderState;
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
        }

        // ⚠️ 这里**没有**枪械分支，是有意的。
        //
        // 事实（2026-08-21 运行期实测）：TACZ 的枪**不带 TOOL 组件**
        // （tacz:modern_kinetic_gun 只有 12 个组件，minecraft:tool 不在其中；
        // 对照组 diamond_pickaxe 有），所以上面那个 has(TOOL) 判据对枪恒为 false，
        // state.backItem 对枪必然为空——枪从来就没走过通用物品渲染这条路。
        //
        // 上游在这里有一条固定变换的兜底（GunMaidRender 的五参 renderBackGun），
        // 不看任何挂点骨骼，直接把枪的物品模型拍在背上。bedrock 模型没有枪械挂点，
        // 实机结果是枪甩到女仆身侧、长度接近整个身体、穿进模型里。三棵树逐字相同，
        // 属上游行为而非移植回归；用户 2026-08-21 裁决砍掉兜底、只保留
        // gecko 模型按 TAC_PISTOL / TAC_RIFLE 骨骼渲染的那条。见 GunMaidRender 类注释。
    }
}