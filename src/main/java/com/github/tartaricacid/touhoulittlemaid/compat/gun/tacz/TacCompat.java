package com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz;

import cn.sh1rocu.touhoulittlemaid.api.event.ExplosionEvents;
import cn.sh1rocu.touhoulittlemaid.api.event.LivingAttackEvent;
import com.github.tartaricacid.simplebedrockmodel.client.bedrock.model.BedrockPart;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidEquipEvent;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidHurtEvent;
import com.github.tartaricacid.touhoulittlemaid.client.entity.GeckoMaidEntity;
import com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz.client.GunBaseAnimation;
import com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz.client.GunGeckoAnimation;
import com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz.client.GunMaidRender;
import com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz.event.GunHurtMaidEvent;
import com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz.event.MaidGunEquipEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.PlayState;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.builder.LoopType;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.event.AnimationEvent;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.GeoModelState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.item.ammo.AmmoSourceRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * TaCZ（Refabricated 26.1.2 分支）兼容门面。与 1.21.11 分支的差异（皆为宿主写法适配，行为不变）：
 * IMaid 形参 → EntityMaid（宿主调用点全是 EntityMaid，无 YSM 路径）；
 * IGeoLocatorSource → {@code GeoModelState}（宿主的定位组载体，gecko 层经 {@code data.modelState} 取得）。
 */
public class TacCompat {
    public static final Identifier MINIGUN_ID = Identifier.fromNamespaceAndPath("tacz", "minigun");
    public static final String TACZ_ID = "tacz";
    private static boolean INSTALLED = false;

    public static boolean init() {
        if (FabricLoader.getInstance().isModLoaded(TACZ_ID)) {
            GunHurtMaidEvent gunHurtMaidEvent = new GunHurtMaidEvent();
            MaidHurtEvent.CALLBACK.register(gunHurtMaidEvent::onMaidHurt);
            EntityHurtByGunEvent.PRE.register(gunHurtMaidEvent::onGunHurt);
            LivingAttackEvent.CALLBACK.register(gunHurtMaidEvent::onPlayerHurt);
            ExplosionEvents.DETONATE.register(gunHurtMaidEvent::onExplosionDetonateEvent);

            MaidGunEquipEvent maidGunEquipEvent = new MaidGunEquipEvent();
            MaidEquipEvent.CALLBACK.register(maidGunEquipEvent::onMaidEquip);

            // 女仆背包作为弹药来源。上游 26.1.2_R2 起提供官方 AmmoSource API，取代原来的四个 mixin
            // ——那四个注入锚点在 R2 里一个不剩（双 jar javap 实证），而 mixins.json 是 required:true，
            // 留着旧写法上 R2 是启动崩溃。本处在 isModLoaded 守卫内，且 MaidAmmoSource 单独成类，
            // 未装 TaCZ 时不会被类加载。
            // 两个逻辑端都要登记：换弹动画那条判定（GunAnimationStateContext）跑在客户端，
            // 而本方法经 CommonRegistry → TaskManager.init() 在两端都会执行。
            AmmoSourceRegistry.EVENT.register(MaidAmmoSource::findFor);

            INSTALLED = true;
        }
        return INSTALLED;
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }

    public static boolean isGun(ItemStack stack) {
        if (INSTALLED) {
            return TacInnerCompat.isGun(stack);
        }
        return false;
    }

    public static boolean isGrenade(ItemStack itemStack) {
        // TODO 手雷还没有
        return false;
    }

    @Nullable
    public static Identifier getGunId(ItemStack stack) {
        if (INSTALLED) {
            return TacInnerCompat.getGunId(stack);
        }
        return null;
    }

    public static boolean canSee(EntityMaid maid, LivingEntity target) {
        if (INSTALLED) {
            return TacInnerCompat.canSee(maid, target);
        }
        return false;
    }

    public static int performGunAttack(EntityMaid shooter, LivingEntity target, ItemStack gunItem) throws Exception {
        if (INSTALLED) {
            return TacInnerCompat.performGunAttack(shooter, target, gunItem);
        }
        return 100;
    }

    @Nullable
    public static ModConfigSpec.IntValue recognitionRangeConfig(EntityMaid maid) {
        if (INSTALLED) {
            return TacInnerCompat.recognitionRangeConfig(maid);
        }
        return null;
    }

    public static boolean hasUsableGun(EntityMaid maid) {
        if (INSTALLED) {
            return TacInnerCompat.hasUsableGun(maid);
        }
        return false;
    }

    public static void stopAim(EntityMaid maid) {
        if (INSTALLED) {
            TacInnerCompat.stopAim(maid);
        }
    }

    @Environment(EnvType.CLIENT)
    public static boolean onHoldGun(ItemStack handItem, @Nullable BedrockPart armLeft, @Nullable BedrockPart armRight) {
        if (INSTALLED) {
            return GunBaseAnimation.onHoldGun(handItem, armLeft, armRight);
        }
        return false;
    }

    @Environment(EnvType.CLIENT)
    public static void addItemTranslate(PoseStack matrixStack, ItemStack itemStack, boolean isLeft) {
        if (INSTALLED) {
            GunMaidRender.addItemTranslate(matrixStack, itemStack, isLeft);
        }
    }

    @Environment(EnvType.CLIENT)
    public static void renderBackGun(PoseStack poseStack, SubmitNodeCollector submitNode, int packedLightIn, ItemStack stack, EntityMaid maid) {
        if (INSTALLED) {
            GunMaidRender.renderBackGun(poseStack, submitNode, packedLightIn, stack, maid);
        }
    }

    @Environment(EnvType.CLIENT)
    public static void renderBackGun(ItemStack offhandItem, GeoModelState modelState, EntityMaid maid, PoseStack poseStack, SubmitNodeCollector submitNode, int packedLight) {
        if (INSTALLED && isGun(offhandItem)) {
            poseStack.pushPose();
            GunMaidRender.renderBackGun(offhandItem, modelState, maid, poseStack, submitNode, packedLight);
            poseStack.popPose();
        }
    }

    @Environment(EnvType.CLIENT)
    @Nullable
    public static PlayState playGunMainAnimation(EntityMaid maid, AnimationEvent<GeckoMaidEntity<?>> event, String animationName, LoopType loopType) {
        if (INSTALLED && isGun(maid.getMainHandItem())) {
            return GunGeckoAnimation.playGunMainAnimation(event, animationName, loopType);
        }
        return null;
    }

    @Environment(EnvType.CLIENT)
    @Nullable
    public static PlayState playGunHoldAnimation(ItemStack mainHandItem, AnimationEvent<GeckoMaidEntity<?>> event) {
        if (INSTALLED && isGun(mainHandItem)) {
            return GunGeckoAnimation.playGunHoldAnimation(event, mainHandItem);
        }
        return null;
    }
}
