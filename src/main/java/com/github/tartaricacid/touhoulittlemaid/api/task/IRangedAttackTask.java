package com.github.tartaricacid.touhoulittlemaid.api.task;

import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Optional;

public interface IRangedAttackTask extends IAttackTask {
    /**
     * 可见性校验工具，来自于 Sensor
     */
    TargetingConditions TARGET_CONDITIONS = TargetingConditions.forCombat();

    /**
     * 按女仆手里的武器找一个能开火的远程实现。
     *
     * <p>{@code EntityMaid.performRangedAttack} 原本只认**当前工作任务**：任务不是远程任务时
     * 整个方法是空操作。后果是「农场女仆手持弓有箭」在威胁响应里只能冲上去肉搏，而同样情形下
     * 手持枪却会站定开枪（枪走 TaCZ 自己的射击链，不受任务限制）——同样是「手里有能打的远程武器」，
     * 一个用一个不用。因此弓弩也不再绑定工作任务，改为按手里的武器找实现。</p>
     *
     * <p>优先用当前任务（这样已经是弓手/弩手的女仆行为逐字不变），找不到才扫注册表。
     * 扫描顺序即 {@code TaskManager.getTaskIndex()} 的顺序，取第一个认领这把武器的远程任务，
     * 因此第三方模组注册的远程任务也会被自动覆盖。</p>
     *
     * @return 能开火的实现；手里那把没有任何远程任务认领时返回 {@code null}
     */
    @Nullable
    static IRangedAttackTask resolveImplementation(EntityMaid maid, ItemStack weapon) {
        // 当前任务只要是远程任务就直接用它，**不再额外要求它的 isWeapon 认这把武器**——
        // 基准就是这么做的（MaidShootTargetTask 的进入条件只要求 ProjectileWeaponItem，不要求是弓），
        // 加了那层检查会让「弓兵模式手持弩」改用弩的实现，手持无人认领的模组远程武器时
        // 更会解析为 null 直接哑火。扫注册表只在任务不是远程任务时兜底，于是新能力是纯增量。
        if (maid.getTask() instanceof IRangedAttackTask current) {
            return current;
        }
        for (IMaidTask task : TaskManager.getTaskIndex()) {
            if (task instanceof IRangedAttackTask ranged && ranged.isWeapon(maid, weapon)) {
                return ranged;
            }
        }
        return null;
    }

    /**
     * 寻找第一个可见目标，使用独立的方法，区别于 IAttackTask
     *
     * @param maid 女仆
     * @return 第一个可视对象
     */
    static Optional<? extends LivingEntity> findFirstValidAttackTarget(EntityMaid maid) {
        // 先检查攻击女仆的对象
        LivingEntity lastAttacker = maid.getLastHurtByMob();
        if (lastAttacker != null && maid.canAttack(lastAttacker) && maid.canSee(lastAttacker)) {
            return Optional.of(lastAttacker);
        }
        // 再检查记忆中的可见对象
        var memory = maid.getBrain().getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES);
        if (memory.isEmpty()) {
            return Optional.empty();
        }
        // 改回 for 循环，避免 stream 带来的额外开销
        for (LivingEntity e : memory.get()) {
            if (maid.canAttack(e) && maid.canSee(e)) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    /**
     * 依据配置文件和 TargetingConditions 来检验攻击目标是否符合条件
     *
     * @param maid        女仆
     * @param target      女仆将要攻击的对象
     * @param configRange 相关距离的配置文件
     * @return 能够攻击
     */
    static boolean targetConditionsTest(EntityMaid maid, LivingEntity target, ModConfigSpec.IntValue configRange) {
        TARGET_CONDITIONS.range(ServerRuleConfig.get(configRange));
        // TODO: 1.21.11 fix - TargetingConditions.test now requires ServerLevel as first arg
        if (!(maid.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return false;
        }
        return TARGET_CONDITIONS.test(serverLevel, maid, target);
    }

    /**
     * 执行射击动作
     *
     * @param shooter        射击者
     * @param target         射击目标
     * @param distanceFactor 距离因素，即弓箭的蓄力值
     */
    void performRangedAttack(EntityMaid shooter, LivingEntity target, float distanceFactor);

    /**
     * 女仆是否能看到敌人
     * <p>
     * 因为原版默认的攻击识别范围是固定死的 16 格，但是一些远程武器我们希望获得超视距打击
     * 通过修改此处来获得更远的攻击距离
     *
     * @param maid   女仆
     * @param target 攻击目标
     * @return 是否在可视范围内
     */
    default boolean canSee(EntityMaid maid, LivingEntity target) {
        return BehaviorUtils.canSee(maid, target);
    }
}
