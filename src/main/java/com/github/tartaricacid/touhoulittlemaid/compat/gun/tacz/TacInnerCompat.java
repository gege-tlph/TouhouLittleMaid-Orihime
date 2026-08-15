package com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz;

import cn.sh1rocu.touhoulittlemaid.util.transfer.ItemUtil;
import com.github.tartaricacid.touhoulittlemaid.compat.gun.common.GunRecognitionRange;
import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.item.GunTabType;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Optional;

import static com.github.tartaricacid.touhoulittlemaid.api.task.IRangedAttackTask.targetConditionsTest;

/**
 * 真正引用 TaCZ 类的一层（compileOnly，只在 {@code TacCompat.INSTALLED} 后被类加载）。
 * 与 1.21.11 分支的差异：{@code targetConditionsTest} 宿主已改收**已解析的 int**
 *（读点纪律的结构化修法），故此处经 {@code ServerRuleConfig.get} 解析后传入；
 * 背包遍历按宿主 Fabric transfer 形态（getSlotCount / ItemUtil.getStack）。
 */
public class TacInnerCompat {
    @Nullable
    static Identifier getGunId(ItemStack stack) {
        IGun iGun = IGun.getIGunOrNull(stack);
        if (iGun != null) {
            return iGun.getGunId(stack);
        }
        return null;
    }

    static boolean isGun(ItemStack itemStack) {
        return itemStack.getItem() instanceof IGun;
    }

    static boolean canSee(EntityMaid maid, LivingEntity target) {
        ItemStack handItem = maid.getMainHandItem();
        IGun iGun = IGun.getIGunOrNull(handItem);
        if (iGun == null) {
            return BehaviorUtils.canSee(maid, target);
        }
        Identifier gunId = iGun.getGunId(handItem);
        return TimelessAPI.getCommonGunIndex(gunId).map(index -> {
            // 枪种 → 哪一档，唯一一份映射在 GunRecognitionRange，与 searchRadius 共用；
            // 三键是世界规则，经唯一读口解析成值再传入
            return targetConditionsTest(maid, target, ServerRuleConfig.get(GunRecognitionRange.configFor(index.getType())));
        }).orElse(BehaviorUtils.canSee(maid, target));
    }

    static int performGunAttack(EntityMaid shooter, LivingEntity target, ItemStack gunItem) {
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return 100;
        }
        Identifier gunId = iGun.getGunId(gunItem);
        Optional<CommonGunIndex> optional = TimelessAPI.getCommonGunIndex(gunId);
        if (optional.isEmpty()) {
            return 100;
        }
        CommonGunIndex gunIndex = optional.get();
        GunData gunData = gunIndex.getGunData();

        double x = target.getX() - shooter.getX();
        double y = target.getEyeY() - shooter.getEyeY();
        double z = target.getZ() - shooter.getZ();

        float yaw = (float) -Math.toDegrees(Math.atan2(x, z));
        float pitch = (float) -Math.toDegrees(Math.atan2(y, Math.sqrt(x * x + z * z)));

        float radius = shooter.getHomeRadius();

        IGunOperator gunOperator = IGunOperator.fromLivingEntity(shooter);
        ShootResult result = gunOperator.shoot(() -> pitch, () -> yaw);

        if (result == ShootResult.ID_NOT_EXIST || result == ShootResult.NOT_GUN) {
            return 100;
        }

        // 如果是狙击枪，应用瞄准
        String sniper = GunTabType.SNIPER.name().toLowerCase(Locale.ENGLISH);
        if (gunIndex.getType().equals(sniper) && !gunOperator.getSynIsAiming()) {
            gunOperator.aim(true);
            // 多加 2 tick，用来平衡延迟
            return Math.round(gunData.getAimTime() * 20) + 2;
        }

        // 如果是非狙击枪，超出 radius 范围，那么也瞄准
        if (!gunIndex.getType().equals(sniper)) {
            float distance = shooter.distanceTo(target);
            if (distance <= radius && gunOperator.getSynIsAiming()) {
                gunOperator.aim(false);
                // 多加 2 tick，用来平衡延迟
                return Math.round(gunData.getAimTime() * 20) + 2;
            }
            if (distance > radius && !gunOperator.getSynIsAiming()) {
                gunOperator.aim(true);
                // 多加 2 tick，用来平衡延迟
                return Math.round(gunData.getAimTime() * 20) + 2;
            }
        }

        if (result == ShootResult.NOT_DRAW) {
            gunOperator.draw(shooter::getMainHandItem);
            // 多加 2 tick，用来平衡延迟
            return Math.round(gunData.getDrawTime() * 20) + 2;
        }

        if (result == ShootResult.NEED_BOLT) {
            gunOperator.bolt();
            return Math.round(gunData.getBoltActionTime() * 20) + 2;
        }

        if (result == ShootResult.NO_AMMO) {
            // reload 不会触发 MaidRequestItemEvent，此处手动请求弹药
            var availableInv = shooter.getAvailableInv(true);
            ItemsUtil.findStackSlot(availableInv, stack -> {
                IAmmo ammo = IAmmo.getIAmmoOrNull(stack);
                return ammo != null && ammo.isAmmoOfGun(gunItem, stack);
            });
            gunOperator.reload();
            float emptyTime = gunData.getReloadData().getCooldown().getEmptyTime();
            return Math.round(emptyTime * 20) + 2;
        }

        FireMode fireMode = iGun.getFireMode(gunItem);
        if (fireMode == FireMode.SEMI || fireMode == FireMode.BURST) {
            return 10 + shooter.getRandom().nextInt(5);
        }

        return 2;
    }

    /**
     * 主手那把枪对应的识别距离配置项；不是枪或枪包索引查不到时返回 {@code null}。
     * 供 {@code TaskGunAttack.searchRadius} 用，与 {@link #canSee} 共用同一份映射。
     */
    @Nullable
    static ModConfigSpec.IntValue recognitionRangeConfig(EntityMaid maid) {
        ItemStack handItem = maid.getMainHandItem();
        IGun iGun = IGun.getIGunOrNull(handItem);
        if (iGun == null) {
            return null;
        }
        return TimelessAPI.getCommonGunIndex(iGun.getGunId(handItem))
                .map(index -> GunRecognitionRange.configFor(index.getType()))
                .orElse(null);
    }

    /**
     * 手持枪械且背包里有对应弹药——威胁响应据此决定「站定射击」还是「上去近战」。
     * 判据与 {@code AbstractGunItemMixin.hasInventoryAmmo} 一致：弹药或弹药盒任一命中即可。
     */
    static boolean hasUsableGun(EntityMaid maid) {
        ItemStack gunItem = maid.getMainHandItem();
        if (!(gunItem.getItem() instanceof IGun)) {
            return false;
        }
        var inv = maid.getAllInv();
        for (int i = 0; i < inv.getSlotCount(); i++) {
            ItemStack stack = ItemUtil.getStack(inv, i);
            if (stack.getItem() instanceof IAmmo ammo && ammo.isAmmoOfGun(gunItem, stack)) {
                return true;
            }
            if (stack.getItem() instanceof IAmmoBox box && box.isAmmoBoxOfGun(gunItem, stack)) {
                return true;
            }
        }
        return false;
    }

    static void stopAim(EntityMaid maid) {
        ItemStack mainHandItem = maid.getMainHandItem();
        IGun iGun = IGun.getIGunOrNull(mainHandItem);
        if (iGun == null) {
            return;
        }
        Identifier gunId = iGun.getGunId(mainHandItem);
        TimelessAPI.getCommonGunIndex(gunId).ifPresent(gunIndex -> {
            IGunOperator gunOperator = IGunOperator.fromLivingEntity(maid);
            if (gunOperator.getSynIsAiming()) {
                gunOperator.aim(false);
            }
        });
    }
}
