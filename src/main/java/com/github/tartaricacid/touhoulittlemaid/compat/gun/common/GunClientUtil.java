package com.github.tartaricacid.touhoulittlemaid.compat.gun.common;

import com.github.tartaricacid.touhoulittlemaid.api.entity.IMaid;
import com.github.tartaricacid.simplebedrockmodel.client.bedrock.model.BedrockPart;
import com.github.tartaricacid.touhoulittlemaid.client.entity.GeckoMaidEntity;
import com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz.TacCompat;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.PlayState;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.builder.LoopType;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.event.AnimationEvent;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.IGeoLocatorSource;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * 见 {@link GunCommonUtil} 的说明：基准的卓越前线分支在 1.21.11 上无对应模组，已删除。
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

    public static void renderBackGun(PoseStack poseStack, SubmitNodeCollector submitNode, int packedLightIn, ItemStack stack, IMaid maid) {
        if (TacCompat.isGun(stack)) {
            TacCompat.renderBackGun(poseStack, submitNode, packedLightIn, stack, maid);
        }
    }

    public static void renderBackGun(ItemStack offhandItem, IGeoLocatorSource locators, IMaid maid, PoseStack poseStack, SubmitNodeCollector submitNode, int packedLight) {
        if (TacCompat.isGun(offhandItem)) {
            TacCompat.renderBackGun(offhandItem, locators, maid, poseStack, submitNode, packedLight);
        }
    }

    @Nullable
    public static PlayState playGunMainAnimation(IMaid maid, AnimationEvent<GeckoMaidEntity<?>> event, String animationName, LoopType loopType) {
        if (TacCompat.isGun(maid.asEntity().getMainHandItem())) {
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
