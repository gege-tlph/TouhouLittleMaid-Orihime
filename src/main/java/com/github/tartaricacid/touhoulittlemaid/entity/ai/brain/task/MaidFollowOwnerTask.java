package com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task;

import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitBrains;
import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class MaidFollowOwnerTask extends Behavior<EntityMaid> {
    /** Roam radius while the owner is on the move — keep the maid close so it visibly follows. */
    private static final int FOLLOW_MOVING_LEASH = 5;
    /** Roam radius once the owner has stood still — let the maid farm / stroll around freely. */
    private static final int FOLLOW_SETTLED_LEASH = 12;
    /** When returning, walk back only to within this distance (hysteresis, not to the owner's feet). */
    private static final int FOLLOW_COMFORT_DISTANCE = 4;
    /** Only teleport when the owner is genuinely far away (walked / flew off). */
    private static final int FOLLOW_TELEPORT_DISTANCE = 20;
    /** Ticks of owner stillness before the maid may roam on the wider settled leash. */
    private static final int OWNER_SETTLE_TICKS = 40;
    private static final float SMOOTH_SPEED_MODIFIER = 0.6F;
    private static final long FOLLOW_WALK_TARGET_TTL = 1;

    private final float speedModifier;
    private final int stopDistance;
    private boolean returningToOwner = false;
    private @Nullable Vec3 lastOwnerPos;
    private long ownerLastMovedTick = Long.MIN_VALUE;

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

    /**
     * Only genuine combat stops a follow-mode maid from coming to its owner. Pending work
     * (TARGET_POS), item use, or a stray non-owner walk target must NOT block following —
     * players read that as "the maid won't come when I call it". Emergency threat response
     * still interrupts following by design. 两种跟随实现共用这一条守卫。
     */
    static boolean hasCompetingGoal(EntityMaid maid, Entity... allowedFollowTargets) {
        return maid.isEmergencyCombatActive()
                || maid.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET);
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
            maid.getBrain().eraseMemory(InitBrains.TARGET_POS);
            this.doStop(worldIn, maid, gameTimeIn);
            return;
        }

        // 否则跟随主人。是否启用「平滑跟随」由世界规则开关决定：
        // 开 -> 随主人动静变松紧的自然牵引圈；关 -> 原版经典的 home 半径跟随。
        if (!ownerStateConditions(owner, maid) || !maidStateConditions(maid)) {
            return;
        }
        if (ServerRuleConfig.get(MaidConfig.SMOOTH_FOLLOW)) {
            smoothFollow(maid, owner, gameTimeIn);
        } else {
            classicFollow(maid, owner);
        }
    }

    /**
     * Movement-aware leash: keep the maid close while the owner is moving so it reads as following;
     * once the owner has stood still for a moment, widen the leash so the maid can farm / stroll
     * around freely and its own wandering never yanks it back. Returning uses hysteresis (walk back
     * to a comfortable inner distance, then resume roaming) so it never oscillates at the edge, and
     * a teleport is reserved for a genuinely distant owner.
     */
    private void smoothFollow(EntityMaid maid, LivingEntity owner, long gameTimeIn) {
        Vec3 ownerPos = owner.position();
        if (this.lastOwnerPos == null || this.lastOwnerPos.distanceToSqr(ownerPos) > 0.02D) {
            this.ownerLastMovedTick = gameTimeIn;
        }
        this.lastOwnerPos = ownerPos;
        boolean ownerSettled = gameTimeIn - this.ownerLastMovedTick >= OWNER_SETTLE_TICKS;
        int leash = ownerSettled ? FOLLOW_SETTLED_LEASH : FOLLOW_MOVING_LEASH;
        double distanceSqr = maid.distanceToSqr(owner);

        if (distanceSqr > square(FOLLOW_TELEPORT_DISTANCE)) {
            maid.teleportToOwner(owner);
            maid.getNavigationManager().resetNavigation();
            this.returningToOwner = false;
            return;
        }
        if (this.returningToOwner) {
            if (distanceSqr <= square(FOLLOW_COMFORT_DISTANCE)) {
                this.returningToOwner = false;
            }
        } else if (distanceSqr > square(leash)) {
            this.returningToOwner = true;
        }
        if (this.returningToOwner && !ownerIsWalkTarget(maid, owner)) {
            setExpiringFollowTarget(maid, owner, SMOOTH_SPEED_MODIFIER, FOLLOW_COMFORT_DISTANCE);
        }
    }

    /** Classic follow: original home-radius leash + teleport, matching origin/1.21.1 behavior. */
    private void classicFollow(EntityMaid maid, LivingEntity owner) {
        this.returningToOwner = false;
        this.lastOwnerPos = null;
        int startDistance = (int) maid.getHomeRadius() - 2;
        int minTeleportDistance = startDistance + 4;
        if (!maid.closerThan(owner, startDistance)) {
            if (!maid.closerThan(owner, minTeleportDistance)) {
                maid.teleportToOwner(owner);
                maid.getNavigationManager().resetNavigation();
            } else if (!ownerIsWalkTarget(maid, owner)) {
                setExpiringFollowTarget(maid, owner, this.speedModifier, this.stopDistance);
            }
        }
    }

    private static double square(int value) {
        return (double) value * value;
    }

    /**
     * 跟随用的 WALK_TARGET 带 1 tick 有效期：跟随行为每 tick 都会重设它，而一个**不过期**的
     * WALK_TARGET 会在女仆已经跟上后继续压住工作行为（它们进不去）。过期语义让「不再需要跟随」
     * 这件事自动生效，不依赖任何一处显式擦除。
     */
    static void setExpiringFollowTarget(EntityMaid maid, Entity target, float speedModifier, int stopDistance) {
        EntityTracker tracker = new EntityTracker(target, true);
        maid.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, tracker);
        maid.getBrain().setMemoryWithExpiry(MemoryModuleType.WALK_TARGET,
                new WalkTarget(tracker, speedModifier, stopDistance), FOLLOW_WALK_TARGET_TTL);
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
