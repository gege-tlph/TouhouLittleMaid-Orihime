package com.github.tartaricacid.touhoulittlemaid.api.task;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Optional;

public interface IRangedAttackTask extends IAttackTask {
    /**
     * 可见性校验工具，来自于 Sensor
     */
    TargetingConditions TARGET_CONDITIONS = TargetingConditions.forCombat();

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
     * @param maid   女仆
     * @param target 女仆将要攻击的对象
     * @param range  相关距离，由调用方经 {@code ServerRuleConfig.get(...)} 解析后传入
     * @return 能够攻击
     *
     * <p>形参原本收的是 {@code ModConfigSpec.IntValue} 本身，由本方法自己 {@code get()}。
     * 那些距离已成为服务器权威的世界规则，读口只有 {@code ServerRuleConfig.get(...)}，
     * 而「把配置对象传进来、在里面读」这种写法会让读点藏在形参后面——按 {@code XXX.get()} 扫是扫不出来的。
     * 故改收**已解析的值**：让调用点天然正确，而不是靠每个调用方自觉。</p>
     */
    static boolean targetConditionsTest(EntityMaid maid, LivingEntity target, int range) {
        TARGET_CONDITIONS.range(range);
        if (maid.level() instanceof ServerLevel serverLevel) {
            return TARGET_CONDITIONS.test(serverLevel, maid, target);
        }
        return false;
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
