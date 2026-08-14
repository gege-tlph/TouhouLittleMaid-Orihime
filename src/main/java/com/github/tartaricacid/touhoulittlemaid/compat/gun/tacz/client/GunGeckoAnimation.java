package com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz.client;

import com.github.tartaricacid.touhoulittlemaid.api.entity.IMaid;
import com.github.tartaricacid.touhoulittlemaid.client.animation.gecko.condition.ConditionTAC;
import com.github.tartaricacid.touhoulittlemaid.client.entity.GeckoMaidEntity;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.PlayState;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.builder.LoopType;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.event.AnimationEvent;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.GunTabType;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public class GunGeckoAnimation {
    public static PlayState playGrenadeAnimation(AnimationEvent<GeckoMaidEntity<?>> event, InteractionHand hand) {
        // TODO 手雷还没有
        if (hand == InteractionHand.MAIN_HAND) {
            return playLoopAnimation(event, "tac:mainhand:grenade");
        }
        return playLoopAnimation(event, "tac:offhand:grenade");
    }

    /**
     * tac:idle
     * tac:run
     * tac:walk
     */
    public static PlayState playGunMainAnimation(AnimationEvent<GeckoMaidEntity<?>> event, String animationName, LoopType loopType) {
        String tacName = "tac:" + animationName;
        var animatable = event.getAnimatableEntity();
        // 基准是 GeckoLibCache.getInstance().getAnimations().get(id).animations().containsKey(name)；
        // 本树把「这个模型有没有这条动画」收敛成了 getAnimation(name)（见 AnimationManager 的同款判据）
        if (!isMaidCarrying(animatable.getMaid()) && animatable.getAnimation(tacName) != null) {
            return playAnimation(event, tacName, loopType);
        }
        return playAnimation(event, animationName, loopType);
    }

    /**
     * tac:hold:pistol
     * tac:aim:pistol
     * tac:reload:pistol
     * tac:aim_shoot:pistol
     * tac:hold_shoot:pistol
     * tac:run:pistol
     */
    public static PlayState playGunHoldAnimation(AnimationEvent<GeckoMaidEntity<?>> event, ItemStack heldItem) {
        IGun gun = IGun.getIGunOrNull(heldItem);
        if (gun == null) {
            return PlayState.STOP;
        }
        Optional<CommonGunIndex> indexOptional = TimelessAPI.getCommonGunIndex(gun.getGunId(heldItem));
        if (indexOptional.isEmpty()) {
            return PlayState.STOP;
        }
        CommonGunIndex gunIndex = indexOptional.get();
        String weaponType = gunIndex.getType();
        IMaid maid = event.getAnimatableEntity().getMaid();
        if (maid == null) {
            return PlayState.STOP;
        }
        if (isMaidCarrying(maid)) {
            return PlayState.STOP;
        }
        Mob entity = maid.asEntity();
        IGunOperator operator = IGunOperator.fromLivingEntity(entity);
        long fireTick = operator.getSynShootCoolDown();

        if (!entity.isSwimming() && entity.getPose() == Pose.SWIMMING) {
            if (Math.abs(event.getLimbSwingAmount()) > 0.05) {
                return getGunTypeAnimation(event, weaponType, "tac:climb:");
            } else {
                if (fireTick > 0) {
                    return getGunTypeAnimation(event, weaponType, "tac:climbing:fire:");
                }
                return getGunTypeAnimation(event, weaponType, "tac:climbing:");
            }
        }

        float reloadProgress = operator.getSynReloadState().getCountDown();
        if (reloadProgress > 0) {
            if (reloadProgress == 1) {
                // 基准是 controller.shouldResetTick = true + adjustTick(0)，即「换弹开始的这一帧把动画拨回 0」。
                // 本树的动画控制器没有裸 tick 可拨，表达「立刻从头播」的是 indicateReload()——
                // AnimationManager 的挥手/使用物品重启用的也是它（基准那两处则是播一帧 empty 动画）。
                event.getCodedController().indicateReload();
            }
            return getGunTypeAnimation(event, weaponType, "tac:reload:");
        }

        float aimProgress = operator.getSynAimingProgress();
        if (aimProgress > 0) {
            if (fireTick > 0) {
                return getGunTypeAnimation(event, weaponType, "tac:aim:fire:");
            }
            return getGunTypeAnimation(event, weaponType, "tac:aim:");
        } else {
            if (entity.onGround() && entity.isSprinting()) {
                return getGunTypeAnimation(event, weaponType, "tac:run:");
            }
            if (fireTick > 0) {
                return getGunTypeAnimation(event, weaponType, "tac:hold:fire:");
            }
            return getGunTypeAnimation(event, weaponType, "tac:hold:");
        }
    }

    @NotNull
    private static PlayState getGunTypeAnimation(AnimationEvent<GeckoMaidEntity<?>> event, String weaponType, String prefix) {
        IMaid maid = event.getAnimatableEntity().getMaid();
        if (maid == null) {
            return PlayState.STOP;
        }
        Mob entity = maid.asEntity();
        // 基准是静态表 ConditionManager.getTAC(modelId)（模型未登记时返回 null）；
        // 本树的 ConditionManager 已改为随 GeckoContainer 走的每模型实例，字段恒非 null，
        // 未登记时其内部列表为空 → doTest 返回空串，与基准取到 null 后跳过等价。
        ConditionTAC conditionTAC = event.getAnimatableEntity().getGeckoContainer().conditionManager().tac;
        if (conditionTAC != null) {
            ItemStack stack = entity.getMainHandItem();
            String name = conditionTAC.doTest(stack, prefix);
            if (StringUtils.isNoneBlank(name)) {
                return playAnimation(event, name, LoopType.LOOP);
            }
        }
        if (isType(weaponType, GunTabType.PISTOL)) {
            return playLoopAnimation(event, prefix + "pistol");
        }
        if (isType(weaponType, GunTabType.RPG)) {
            return playLoopAnimation(event, prefix + "rpg");
        }
        return playLoopAnimation(event, prefix + "rifle");
    }

    @NotNull
    private static PlayState playLoopAnimation(AnimationEvent<?> event, String animationName) {
        return playAnimation(event, animationName, LoopType.LOOP);
    }

    @NotNull
    private static PlayState playAnimation(AnimationEvent<?> event, String animationName, LoopType loopType) {
        event.getCodedController().setAnimation(animationName, loopType);
        return PlayState.CONTINUE;
    }

    private static boolean isType(String type, GunTabType tabType) {
        return type.equals(tabType.name().toLowerCase(Locale.ENGLISH));
    }

    private static boolean isMaidCarrying(IMaid maid) {
        Mob entity = maid.asEntity();
        return entity.getVehicle() instanceof Player;
    }
}
