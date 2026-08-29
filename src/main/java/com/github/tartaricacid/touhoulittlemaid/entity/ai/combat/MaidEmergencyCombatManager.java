package com.github.tartaricacid.touhoulittlemaid.entity.ai.combat;

import com.github.tartaricacid.touhoulittlemaid.api.entity.targeting.MaidTargetingContext;
import com.github.tartaricacid.touhoulittlemaid.api.task.IRangedAttackTask;
import com.github.tartaricacid.touhoulittlemaid.compat.gun.common.GunCommonUtil;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidUpdateActivityFromSchedule;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.targeting.MaidTargetingPolicy;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.MaidManagerDef;
import com.github.tartaricacid.touhoulittlemaid.init.InitBrains;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Owns the transient execution state for emergency combat. Permanent task,
 * schedule, Home positions, and task data remain outside this manager.
 *
 * <p>行为基准是 1.21.11 分支的 {@code entity.ai.combat.MaidCombatManager}。本树宿主已有一个
 * 同名不同物的 {@code entity.passive.MaidCombatManager}（战斗执行层：盾牌、横扫、伤害事件），
 * 且 {@code getCombatManager()} 访问器归它——故本类按宿主 manager 约定改名挂接，
 * 访问器为 {@code getEmergencyCombatManager()}，行为与基准逐字对齐。</p>
 */
@MaidManagerDef(alias = "emergencyCombatManager")
public final class MaidEmergencyCombatManager {
    public static final double LOCAL_PROTECTION_RANGE = 16.0;
    private static final long PLAYER_COMMAND_REENTRY_SUPPRESSION_TICKS = 20;
    private static final long TARGET_STICKINESS_TICKS = 40;
    private static final long UNREACHABLE_TIMEOUT_TICKS = 60;
    private static final long OWNER_CONFIRMATION_WINDOW_TICKS = 100;
    private static final int MAX_OWNER_INTENT_CANDIDATES = 4;

    private final EntityMaid maid;
    private final LinkedHashMap<UUID, OwnerIntentCandidate> ownerIntentCandidates = new LinkedHashMap<>();
    private @Nullable LivingEntity target;
    private MaidTargetingContext targetingContext = MaidTargetingContext.SELF_DEFENSE;
    private long reentrySuppressedUntil;
    private long targetStickyUntil;
    private long lastLocalThreatTick;
    private @Nullable UUID ownerCommandedAttackTarget;

    public MaidEmergencyCombatManager(EntityMaid maid) {
        this.maid = maid;
    }

    public boolean beginEmergency(LivingEntity target, MaidTargetingContext context) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(context, "context");
        if (context == MaidTargetingContext.PLANNED_ATTACK
                || !policyAllows(context)
                || maid.level().getGameTime() < reentrySuppressedUntil
                || !isLocalThreat(target, context)
                || !MaidTargetingPolicy.canAttackByDefaultRules(maid, target, context)) {
            return false;
        }

        long gameTime = maid.level().getGameTime();
        if (isEmergencyActive() && this.target != target
                && context != MaidTargetingContext.SELF_DEFENSE
                && gameTime < targetStickyUntil
                && MaidTargetingPolicy.canContinueTargeting(
                maid, this.target, this.targetingContext)) {
            return false;
        }

        Brain<EntityMaid> brain = maid.getBrain();
        clearExecutionMemories(brain);
        this.target = target;
        this.targetingContext = context;
        this.targetStickyUntil = gameTime + TARGET_STICKINESS_TICKS;
        this.lastLocalThreatTick = gameTime;
        brain.setMemory(InitBrains.EMERGENCY_COMBAT_ACTIVE, true);
        brain.setMemory(MemoryModuleType.ATTACK_TARGET, target);
        // The accepted threat is now owned by this manager. Do not leave the
        // old panic trigger competing for the non-core activity.
        brain.eraseMemory(MemoryModuleType.HURT_BY);
        brain.eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
        brain.setActiveActivityIfPossible(InitBrains.EMERGENCY_COMBAT);
        if (brain.isActive(InitBrains.EMERGENCY_COMBAT)) {
            return true;
        }
        this.target = null;
        this.targetingContext = MaidTargetingContext.SELF_DEFENSE;
        brain.eraseMemory(InitBrains.EMERGENCY_COMBAT_ACTIVE);
        clearExecutionMemories(brain);
        return false;
    }

    public void tick(ServerLevel level) {
        pruneAndEvaluateOwnerIntent(level.getGameTime());
        if (!isEmergencyActive()) {
            return;
        }
        Brain<EntityMaid> brain = maid.getBrain();
        LivingEntity memoryTarget = brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        if (memoryTarget != target
                || !policyAllows(targetingContext)
                || !MaidTargetingPolicy.canContinueTargeting(maid, target, targetingContext)
                || hasBeenUnreachableTooLong(brain, level.getGameTime())
                || !retainLocalThreat(level.getGameTime())) {
            cancelEmergency(false);
            return;
        }
        if (!brain.isActive(InitBrains.EMERGENCY_COMBAT)) {
            brain.setActiveActivityIfPossible(InitBrains.EMERGENCY_COMBAT);
        }
    }

    public void onPlayerCommand() {
        ownerIntentCandidates.clear();
        this.ownerCommandedAttackTarget = null;
        cancelEmergency(true);
    }

    public void onMaidDamaged(DamageSource source) {
        Entity responsible = source.getEntity();
        if (!(responsible instanceof LivingEntity attacker)) {
            return;
        }
        if (attacker == target && isEmergencyActive()) {
            targetingContext = MaidTargetingContext.SELF_DEFENSE;
            targetStickyUntil = maid.level().getGameTime() + TARGET_STICKINESS_TICKS;
            lastLocalThreatTick = maid.level().getGameTime();
            return;
        }
        beginEmergency(attacker, MaidTargetingContext.SELF_DEFENSE);
    }

    public void onOwnerDamaged(LivingEntity attacker) {
        beginEmergency(attacker, MaidTargetingContext.PROTECT_OWNER);
    }

    public void onOwnerDealtDirectDamage(LivingEntity attacked) {
        if (getResponsePolicy() != MaidCombatResponsePolicy.PROTECT_OWNER
                || !isLocalThreat(attacked, MaidTargetingContext.PROTECT_OWNER)
                || !MaidTargetingPolicy.canObserveOwnerIntent(maid, attacked)) {
            return;
        }

        long gameTime = maid.level().getGameTime();
        pruneOwnerIntentCandidates(gameTime);
        if (MaidTargetingPolicy.isCurrentlyHostile(
                maid, attacked, MaidTargetingContext.PROTECT_OWNER)) {
            ownerIntentCandidates.remove(attacked.getUUID());
            beginEmergency(attacked, MaidTargetingContext.PROTECT_OWNER);
            return;
        }

        OwnerIntentCandidate candidate = ownerIntentCandidates.get(attacked.getUUID());
        if (candidate == null) {
            makeRoomForOwnerIntentCandidate();
            ownerIntentCandidates.put(attacked.getUUID(),
                    new OwnerIntentCandidate(attacked, gameTime, gameTime, 1));
            return;
        }
        if (candidate.lastHitTick == gameTime) {
            return;
        }
        if (gameTime - candidate.firstHitTick > OWNER_CONFIRMATION_WINDOW_TICKS) {
            ownerIntentCandidates.put(attacked.getUUID(),
                    new OwnerIntentCandidate(attacked, gameTime, gameTime, 1));
            return;
        }

        OwnerIntentCandidate updated = new OwnerIntentCandidate(
                attacked, candidate.firstHitTick, gameTime, candidate.hitCount + 1);
        ownerIntentCandidates.put(attacked.getUUID(), updated);
        if (updated.hitCount >= 2
                && MaidTargetingPolicy.canAttackAfterOwnerConfirmation(maid, attacked)) {
            ownerIntentCandidates.remove(attacked.getUUID());
            beginEmergency(attacked, MaidTargetingContext.PROTECT_OWNER);
        }
    }

    public void onBrainRefresh() {
        ownerIntentCandidates.clear();
        this.ownerCommandedAttackTarget = null;
        this.target = null;
        this.targetingContext = MaidTargetingContext.SELF_DEFENSE;
        this.targetStickyUntil = 0;
        this.lastLocalThreatTick = 0;
        Brain<EntityMaid> brain = maid.getBrain();
        brain.eraseMemory(InitBrains.EMERGENCY_COMBAT_ACTIVE);
        clearExecutionMemories(brain);
    }

    public void cancelEmergency(boolean suppressReentry) {
        boolean wasActive = isEmergencyActive();
        if (suppressReentry) {
            reentrySuppressedUntil = Math.max(reentrySuppressedUntil,
                    maid.level().getGameTime() + PLAYER_COMMAND_REENTRY_SUPPRESSION_TICKS);
        }

        this.target = null;
        this.targetingContext = MaidTargetingContext.SELF_DEFENSE;
        this.targetStickyUntil = 0;
        this.lastLocalThreatTick = 0;
        Brain<EntityMaid> brain = maid.getBrain();
        brain.eraseMemory(InitBrains.EMERGENCY_COMBAT_ACTIVE);
        clearExecutionMemories(brain);
        maid.getNavigation().stop();

        if (wasActive) {
            // Force an immediate lookup of the current schedule. Old path and
            // target snapshots are intentionally never restored.
            brain.lastScheduleUpdate = maid.level().getGameTime() - 21L;
            MaidUpdateActivityFromSchedule.updateActivityFromSchedule(maid, brain);
        }
    }

    /**
     * 女仆手上是不是一把「现在就能用」的远程武器。
     *
     * <p>威胁响应的三个消费者共用这一条判据：近战行为据它决定要不要放行、应战走位据它决定
     * 贴脸还是站定、应战远程行为据它决定能不能进入。<b>不许各写各的</b>——判据分叉的那天，
     * 就会出现「站定不动却又不开枪」这种自相矛盾的行为。</p>
     *
     * <p>「能用」含弹药：弓弩没箭、枪没子弹时返回 false，让她回落到近战，
     * 而不是端着空枪站在原地挨打。</p>
     */
    public static boolean isHoldingUsableRangedWeapon(EntityMaid maid) {
        ItemStack held = maid.getMainHandItem();
        // 三个条件缺一不可，少任何一个都会让她「站定却打不出去」：
        //   ① 手里确实是远程武器；
        //   ② 她真的开得出火——即这把武器能找到一个远程任务实现来执行射击。
        //      **不要用 canUseNonMeleeWeapon**：女仆把它覆写成了「当前工作任务是不是远程任务」，
        //      而射击实现已于弓弩解绑那刀改为按武器解析，两者不再等价；
        //   ③ 有弹药——getProjectile 是女仆自己的取弹逻辑，会翻手持与背包，
        //      canUseNonMeleeWeapon 不看这一层。
        boolean vanillaRanged = held.getItem() instanceof ProjectileWeaponItem
                && IRangedAttackTask.resolveImplementation(maid, held) != null
                && !maid.getProjectile(held).isEmpty();
        // 枪械不经 performRangedAttack（TaCZ 有自己的射击链），故不受任务类型限制
        return vanillaRanged || GunCommonUtil.hasUsableGun(maid);
    }

    public boolean isEmergencyActive() {
        return target != null && maid.getBrain().hasMemoryValue(InitBrains.EMERGENCY_COMBAT_ACTIVE);
    }

    @Nullable
    public LivingEntity getTarget() {
        return target;
    }

    /**
     * The entity the owner explicitly commanded the maid to attack (via a skill / LLM tool call),
     * if any. Such a target obeys only the hard-safety rules — it may be a peaceful or non-angry
     * neutral mob that autonomous targeting would never pick on its own.
     */
    @Nullable
    public UUID getOwnerCommandedAttackTarget() {
        return ownerCommandedAttackTarget;
    }

    public void setOwnerCommandedAttackTarget(@Nullable UUID target) {
        this.ownerCommandedAttackTarget = target;
    }

    public MaidTargetingContext getTargetingContext() {
        return isEmergencyActive() ? targetingContext : MaidTargetingContext.PLANNED_ATTACK;
    }

    public MaidCombatResponsePolicy getResponsePolicy() {
        return maid.getConfigManager().getCombatResponsePolicy();
    }

    public void setResponsePolicy(MaidCombatResponsePolicy responsePolicy) {
        MaidCombatResponsePolicy next = Objects.requireNonNull(responsePolicy);
        if (getResponsePolicy() == next) {
            onPlayerCommand();
            return;
        }
        maid.getConfigManager().setCombatResponsePolicy(next);
        onPlayerCommand();
    }

    public boolean canRunCombatActions() {
        if (!isEmergencyActive()) {
            return true;
        }
        return !maid.getSwimManager().isGoingToBreath()
                && !maid.getSwimManager().isEatBreatheItem()
                && (!maid.isUsingItem()
                    || maid.getUsedItemHand() == InteractionHand.OFF_HAND
                        && maid.getUseItem().has(DataComponents.BLOCKS_ATTACKS))
                && !maid.isOnFire()
                && !maid.isCanClimb()
                && !maid.onClimbable();
    }

    private static void clearExecutionMemories(Brain<EntityMaid> brain) {
        brain.eraseMemory(MemoryModuleType.PATH);
        brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
        brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
        brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        brain.eraseMemory(InitBrains.TARGET_POS);
    }

    private boolean policyAllows(MaidTargetingContext context) {
        return switch (context) {
            case PLANNED_ATTACK -> false;
            case SELF_DEFENSE -> getResponsePolicy() != MaidCombatResponsePolicy.OFF;
            case PROTECT_OWNER -> getResponsePolicy() == MaidCombatResponsePolicy.PROTECT_OWNER;
        };
    }

    private boolean isLocalThreat(LivingEntity candidate, MaidTargetingContext context) {
        if (candidate.level() != maid.level()
                || maid.distanceToSqr(candidate) > LOCAL_PROTECTION_RANGE * LOCAL_PROTECTION_RANGE) {
            return false;
        }
        return context != MaidTargetingContext.PROTECT_OWNER || getLocalOwner() != null;
    }

    @Nullable
    private ServerPlayer getLocalOwner() {
        if (!(maid.getOwner() instanceof ServerPlayer owner)
                || !owner.isAlive()
                || owner.level() != maid.level()
                || maid.distanceToSqr(owner) > LOCAL_PROTECTION_RANGE * LOCAL_PROTECTION_RANGE) {
            return null;
        }
        return owner;
    }

    private boolean retainLocalThreat(long gameTime) {
        boolean targetLocal = maid.distanceToSqr(target)
                <= LOCAL_PROTECTION_RANGE * LOCAL_PROTECTION_RANGE;
        boolean ownerLocal = targetingContext != MaidTargetingContext.PROTECT_OWNER
                || getLocalOwner() != null;
        if (targetLocal && ownerLocal) {
            lastLocalThreatTick = gameTime;
            return true;
        }
        if (targetingContext == MaidTargetingContext.PROTECT_OWNER) {
            if (!(maid.getOwner() instanceof ServerPlayer owner)
                    || !owner.isAlive() || owner.level() != maid.level()) {
                return false;
            }
        }
        return gameTime - lastLocalThreatTick <= TARGET_STICKINESS_TICKS;
    }

    private static boolean hasBeenUnreachableTooLong(Brain<EntityMaid> brain, long gameTime) {
        return brain.getMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)
                .map(since -> gameTime - since >= UNREACHABLE_TIMEOUT_TICKS)
                .orElse(false);
    }

    private void pruneAndEvaluateOwnerIntent(long gameTime) {
        pruneOwnerIntentCandidates(gameTime);
        if (getResponsePolicy() != MaidCombatResponsePolicy.PROTECT_OWNER || getLocalOwner() == null) {
            ownerIntentCandidates.clear();
            return;
        }
        Iterator<Map.Entry<UUID, OwnerIntentCandidate>> iterator =
                ownerIntentCandidates.entrySet().iterator();
        while (iterator.hasNext()) {
            OwnerIntentCandidate candidate = iterator.next().getValue();
            LivingEntity observed = candidate.target;
            if (!isLocalThreat(observed, MaidTargetingContext.PROTECT_OWNER)
                    || !MaidTargetingPolicy.canObserveOwnerIntent(maid, observed)) {
                iterator.remove();
                continue;
            }
            if (MaidTargetingPolicy.isCurrentlyHostile(
                    maid, observed, MaidTargetingContext.PROTECT_OWNER)) {
                iterator.remove();
                beginEmergency(observed, MaidTargetingContext.PROTECT_OWNER);
                return;
            }
        }
    }

    private void pruneOwnerIntentCandidates(long gameTime) {
        ownerIntentCandidates.entrySet().removeIf(entry -> {
            OwnerIntentCandidate candidate = entry.getValue();
            LivingEntity observed = candidate.target;
            return gameTime - candidate.firstHitTick > OWNER_CONFIRMATION_WINDOW_TICKS
                    || !observed.isAlive()
                    || observed.isRemoved()
                    || observed.level() != maid.level();
        });
    }

    private void makeRoomForOwnerIntentCandidate() {
        if (ownerIntentCandidates.size() < MAX_OWNER_INTENT_CANDIDATES) {
            return;
        }
        Iterator<UUID> iterator = ownerIntentCandidates.keySet().iterator();
        if (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    private record OwnerIntentCandidate(LivingEntity target, long firstHitTick,
                                        long lastHitTick, int hitCount) {
    }
}
