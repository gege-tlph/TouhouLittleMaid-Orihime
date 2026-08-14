package com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task;

import com.github.tartaricacid.touhoulittlemaid.entity.ai.combat.MaidCombatManager;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.datafixers.kinds.OptionalBox;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public final class MaidEmergencyWalkToTarget {
    private MaidEmergencyWalkToTarget() {
    }

    public static BehaviorControl<EntityMaid> create(float speedModifier) {
        return BehaviorBuilder.create(instance -> instance.group(
                instance.registered(MemoryModuleType.WALK_TARGET),
                instance.registered(MemoryModuleType.LOOK_TARGET),
                instance.present(MemoryModuleType.ATTACK_TARGET)
        ).apply(instance, (walkTarget, lookTarget, attackTarget) ->
                (level, maid, gameTime) -> setTarget(
                        maid, speedModifier, walkTarget, lookTarget, instance.get(attackTarget))));
    }

    private static boolean setTarget(EntityMaid maid, float speedModifier,
                                     MemoryAccessor<OptionalBox.Mu, WalkTarget> walkTarget,
                                     MemoryAccessor<OptionalBox.Mu, PositionTracker> lookTarget,
                                     LivingEntity target) {
        if (!maid.getCombatManager().isEmergencyActive()
                || !maid.getCombatManager().canRunCombatActions()) {
            return false;
        }

        EntityTracker tracker = new EntityTracker(target, true);
        lookTarget.set(tracker);
        boolean rangedStandoff = MaidCombatManager.isHoldingUsableRangedWeapon(maid)
                && maid.canSee(target)
                && maid.distanceToSqr(target) <= MaidCombatManager.LOCAL_PROTECTION_RANGE
                * MaidCombatManager.LOCAL_PROTECTION_RANGE;
        // 端着远程武器时不再贴脸：目标已在保护半径内且看得见，就站定射击。
        // 同时这一步把 WALK_TARGET 让出来——应战的走位行为要求 WALK_TARGET 缺席才会进入。
        if (rangedStandoff || maid.isWithinMeleeAttackRange(target) || !maid.canBrainMoving()) {
            walkTarget.erase();
        } else {
            walkTarget.set(new WalkTarget(tracker, speedModifier, 0));
        }
        return true;
    }
}
