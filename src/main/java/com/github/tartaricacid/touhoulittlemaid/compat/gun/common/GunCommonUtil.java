package com.github.tartaricacid.touhoulittlemaid.compat.gun.common;

import com.github.tartaricacid.touhoulittlemaid.compat.gun.common.ai.GunShootTargetTask;
import com.github.tartaricacid.touhoulittlemaid.compat.gun.common.task.TaskGunAttack;
import com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz.TacCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * 枪械类模组的公共门面。
 * <p>
 * 基准（origin/1.21.1）这里是 TaCZ 与卓越前线（Superb Warfare）两路并存的分发器。
 * 卓越前线没有 1.21.11 Fabric 端，故本移植只保留 TaCZ 一路：
 * 基准里那些 {@code SWarfareCompat.xxx} 分支在未安装卓越前线时**恒为 false / 恒为空**，
 * 删除它们与基准在 1.21.11 上的可观察行为一致。若将来卓越前线有了 Fabric 端，
 * 按基准把第二路分发加回本类即可，其余文件无需改动。
 */
public class GunCommonUtil {
    public static void initAndAddTask(TaskManager manager) {
        boolean tacz = TacCompat.init();
        if (tacz) {
            manager.add(new TaskGunAttack());
        }
    }

    public static boolean isInstalled() {
        return TacCompat.isInstalled();
    }

    public static boolean isGun(ItemStack stack) {
        return TacCompat.isGun(stack);
    }

    /**
     * 不单单判断枪械，还判断女仆是否在载具上 <br>
     * 女仆在载具上时也可以开火
     * <p>
     * 载具部分属于卓越前线，1.21.11 无该模组：基准在未安装时，
     * 两条载具分支都只会返回 {@code SWarfareCompat.isVehicle(...) == false}，
     * 故此处等价地收敛为「主手是否持枪」。
     */
    public static boolean canStartAttacking(EntityMaid maid) {
        ItemStack item = maid.getMainHandItem();
        return isGun(item);
    }

    @Nullable
    public static Identifier getGunId(ItemStack stack) {
        if (TacCompat.isGun(stack)) {
            return TacCompat.getGunId(stack);
        }
        return null;
    }

    public static Optional<Boolean> canSee(EntityMaid maid, LivingEntity target) {
        ItemStack handItem = maid.getMainHandItem();
        if (TacCompat.isGun(handItem)) {
            return Optional.of(TacCompat.canSee(maid, target));
        }
        // 基准此处的最后一跳是 SWarfareCompat.canVehicleSee(...)，未安装时返回 Optional.empty()
        return Optional.empty();
    }

    /**
     * 基准里这一步只服务卓越前线（TaCZ 侧不需要逐 tick 维护射击状态），
     * 故 1.21.11 上是空实现；保留方法与调用点，避免动 AI 任务的结构。
     */
    public static void tick(EntityMaid shooter, LivingEntity target, ItemStack gunItem) {
    }

    public static int performGunAttack(EntityMaid shooter, LivingEntity target, ItemStack gunItem) throws Exception {
        if (TacCompat.isGun(gunItem)) {
            return TacCompat.performGunAttack(shooter, target, gunItem);
        }
        return 100;
    }

    /**
     * 主手那把枪对应的识别距离配置项；不是枪 / 未装 TaCZ 时返回 {@code null}。
     * 与 {@link #canSee} 共用 {@link GunRecognitionRange} 那一份映射。
     */
    @Nullable
    public static ModConfigSpec.IntValue recognitionRangeConfig(EntityMaid maid) {
        return TacCompat.recognitionRangeConfig(maid);
    }

    /** 手持可用枪械（有枪且有弹）。未装 TaCZ 时恒 false。 */
    public static boolean hasUsableGun(EntityMaid maid) {
        return TacCompat.hasUsableGun(maid);
    }

    public static void onStop(EntityMaid maid, GunShootTargetTask task) {
        TacCompat.stopAim(maid);
    }
}
