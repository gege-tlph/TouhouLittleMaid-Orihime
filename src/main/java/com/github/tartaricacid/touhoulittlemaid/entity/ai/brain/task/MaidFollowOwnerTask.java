package com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task;

import com.github.tartaricacid.touhoulittlemaid.config.subconfig.ExperimentalConfig;
import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;

import javax.annotation.Nullable;

public class MaidFollowOwnerTask extends Behavior<EntityMaid> {
    private static final int SMOOTH_START_DISTANCE = 5;
    private static final int SMOOTH_TELEPORT_DISTANCE = 16;
    private static final float SMOOTH_SPEED_MODIFIER = 0.6F;
    private static final long FOLLOW_WALK_TARGET_TTL = 1;

    private final float speedModifier;
    private final int stopDistance;

    public MaidFollowOwnerTask(float speedModifier, int stopDistance) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED));
        this.speedModifier = speedModifier;
        this.stopDistance = stopDistance;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        LivingEntity owner = maid.getOwner();
        if (ownerStateConditions(owner, maid)) {
            // 如果女仆在前往呼吸点，玩家不在水中
            if (maid.getSwimManager().isGoingToBreath()) {
                return !owner.isUnderWater();
            }
            return !hasCompetingGoal(maid, owner);
        }
        return false;
    }

    @Override
    protected void start(ServerLevel worldIn, EntityMaid maid, long gameTimeIn) {
        LivingEntity owner = maid.getOwner();

        // 如果女仆在前往呼吸点快要淹死了，那必须就近传送
        // 这个传送会鬼畜，但是没办法，为了救女仆只能这样了
        if (maid.getSwimManager().isGoingToBreath() && ownerStateConditions(owner, maid)
                && maidStateConditions(maid) && maid.teleportToOwner(owner)) {
            maid.getNavigationManager().resetNavigation();
            maid.getSwimManager().setGoingToBreath(false);
            maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            maid.getBrain().eraseMemory(InitEntities.TARGET_POS);
            this.doStop(worldIn, maid, gameTimeIn);
            return;
        }

        // 否则正常传送
        boolean smoothFollow = ServerRuleConfig.get(ExperimentalConfig.SMOOTH_FOLLOW);
        int startDistance = smoothFollow ? SMOOTH_START_DISTANCE : (int) maid.getHomeRadius() - 2;
        int minTeleportDistance = smoothFollow ? SMOOTH_TELEPORT_DISTANCE : startDistance + 4;
        float followSpeed = smoothFollow ? SMOOTH_SPEED_MODIFIER : speedModifier;
        if (ownerStateConditions(owner, maid) && maidStateConditions(maid) && !maid.closerThan(owner, startDistance)) {
            if (!maid.closerThan(owner, minTeleportDistance)) {
                maid.teleportToOwner(owner);
                maid.getNavigationManager().resetNavigation();
            } else if (!ownerIsWalkTarget(maid, owner)) {
                setExpiringFollowTarget(maid, owner, followSpeed, stopDistance);
            }
        }
    }

    private boolean maidStateConditions(EntityMaid maid) {
        return !maid.isHomeModeEnable() && maid.canBrainMoving();
    }

    private boolean ownerStateConditions(@Nullable LivingEntity owner, EntityMaid maid) {
        return owner != null && !owner.isSpectator() && !owner.isDeadOrDying() &&
                // 修复一个过去很多年没解决的 bug —— 女仆神秘传送问题
                // 这个 bug 的原因是，传送时没有检查女仆和主人是否在同一个维度
                maid.level == owner.level;
    }

    static boolean hasCompetingGoal(EntityMaid maid, Entity... allowedFollowTargets) {
        if (maid.getCombatManager().isEmergencyActive()
                || maid.getBrain().hasMemoryValue(InitEntities.TARGET_POS)
                || maid.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
                || maid.isUsingItem()) {
            return true;
        }
        return maid.getBrain().getMemory(MemoryModuleType.WALK_TARGET)
                .map(target -> !tracksAny(target, allowedFollowTargets))
                .orElse(false);
    }

    static void setExpiringFollowTarget(EntityMaid maid, Entity target, float speedModifier, int stopDistance) {
        EntityTracker tracker = new EntityTracker(target, true);
        maid.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, tracker);
        maid.getBrain().setMemoryWithExpiry(MemoryModuleType.WALK_TARGET,
                new WalkTarget(tracker, speedModifier, stopDistance), FOLLOW_WALK_TARGET_TTL);
    }

    private boolean ownerIsWalkTarget(EntityMaid maid, LivingEntity owner) {
        return maid.getBrain().getMemory(MemoryModuleType.WALK_TARGET)
                .map(target -> tracksAny(target, owner))
                .orElse(false);
    }

    private static boolean tracksAny(WalkTarget walkTarget, Entity... allowedTargets) {
        if (!(walkTarget.getTarget() instanceof EntityTracker tracker)) {
            return false;
        }
        Entity tracked = tracker.getEntity();
        for (Entity allowed : allowedTargets) {
            if (tracked.equals(allowed)) {
                return true;
            }
        }
        return false;
    }
}
