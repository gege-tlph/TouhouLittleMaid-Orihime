package com.github.tartaricacid.touhoulittlemaid.client.animation.special;

import com.github.tartaricacid.simplebedrockmodel.client.bedrock.model.BedrockPart;
import com.github.tartaricacid.touhoulittlemaid.api.animation.IAnimation;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.state.EntityMaidRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.TridentItem;

import java.util.HashMap;

/**
 * origin/1.21.1 行为基准：实体驱动（mob.isUsingItem/getUsedItemHand/getMainHandItem instanceof TridentItem，
 * tick = getTicksUsingItem() + partialTick）。RenderState 化后各值由 HUB 经
 * HumanoidMobRenderer.extractHumanoidRenderState 填充（ticksUsingItem = getTicksUsingItem(partialTick)，
 * 已含插值，与 origin 等价）；物品判定保持 origin 的 instanceof TridentItem（26.1 改用
 * ConventionalItemTags.TRIDENT_TOOLS 属行为漂移，且该常量在本版 Fabric API 不存在）。
 */
public class TridentAnimation implements IAnimation<EntityMaidRenderState> {
    @Override
    public void setupAnimation(EntityMaidRenderState state, HashMap<String, BedrockPart> models) {
        if (!state.sleeping && state.isUsingItem
                && state.useItemHand == InteractionHand.MAIN_HAND
                && state.getMainHandItemStack().getItem() instanceof TridentItem
        ) {
            BedrockPart armRight = models.get("armRight");
            if (armRight != null) {
                float rot = state.ticksUsingItem / 10f;
                armRight.xRot = (armRight.getInitRotX() - 80) - Math.min(rot, Mth.PI / 2) - 10;
                armRight.zRot = -Math.min(rot, Mth.PI / 6);
            }
        }
    }
}
