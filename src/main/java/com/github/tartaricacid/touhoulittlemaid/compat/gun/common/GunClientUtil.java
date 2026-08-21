package com.github.tartaricacid.touhoulittlemaid.compat.gun.common;

import com.github.tartaricacid.simplebedrockmodel.client.bedrock.model.BedrockPart;
import com.github.tartaricacid.touhoulittlemaid.client.entity.GeckoMaidEntity;
import com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz.TacCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.PlayState;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.builder.LoopType;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.event.AnimationEvent;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.GeoModelState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * 见 {@link GunCommonUtil} 的说明：基准的卓越前线分支在 Fabric 上无对应模组，已删除。
 */
@Environment(EnvType.CLIENT)
public class GunClientUtil {
    public static boolean onHoldGun(ItemStack handItem, @Nullable BedrockPart armLeft, @Nullable BedrockPart armRight) {
        return TacCompat.onHoldGun(handItem, armLeft, armRight);
    }

    public static void addItemTranslate(PoseStack poseStack, ItemStack itemStack, boolean isLeft) {
        if (TacCompat.isGun(itemStack)) {
            TacCompat.addItemTranslate(poseStack, itemStack, isLeft);
        }
    }

    /**
     * 背部枪械渲染。<b>只有 gecko 模型走得到这里</b>，且只在模型作者提供了
     * {@code TAC_PISTOL} / {@code TAC_RIFLE} 挂点骨骼时才真的画出东西。
     *
     * <p>⚠️ 曾经还有一个五参重载，走的是不看骨骼的固定变换兜底（bedrock 模型与
     * 「gecko + 穿背包」两条路的落点）。实机结果是枪甩到女仆身侧、穿进模型里，
     * 用户 2026-08-21 裁决砍掉，见 {@code GunMaidRender} 的类注释。</p>
     */
    public static void renderBackGun(ItemStack offhandItem, GeoModelState modelState, EntityMaid maid, PoseStack poseStack, SubmitNodeCollector submitNode, int packedLight) {
        if (TacCompat.isGun(offhandItem)) {
            TacCompat.renderBackGun(offhandItem, modelState, maid, poseStack, submitNode, packedLight);
        }
    }

    @Nullable
    public static PlayState playGunMainAnimation(EntityMaid maid, AnimationEvent<GeckoMaidEntity<?>> event, String animationName, LoopType loopType) {
        if (TacCompat.isGun(maid.getMainHandItem())) {
            return TacCompat.playGunMainAnimation(maid, event, animationName, loopType);
        }
        return null;
    }

    @Nullable
    public static PlayState playGunHoldAnimation(ItemStack mainHandItem, AnimationEvent<GeckoMaidEntity<?>> event) {
        if (TacCompat.isGun(mainHandItem)) {
            return TacCompat.playGunHoldAnimation(mainHandItem, event);
        }
        return null;
    }
}
