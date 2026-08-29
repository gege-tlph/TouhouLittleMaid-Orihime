package com.github.tartaricacid.touhoulittlemaid.entity.ai.targeting;

import com.github.tartaricacid.touhoulittlemaid.api.entity.targeting.IMaidHostilityAdapter;
import com.github.tartaricacid.touhoulittlemaid.api.entity.targeting.MaidHostilityDecision;
import com.github.tartaricacid.touhoulittlemaid.api.entity.targeting.MaidTargetingContext;
import com.github.tartaricacid.touhoulittlemaid.api.task.IAttackTask;
import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.data.AttackListData;
import com.github.tartaricacid.touhoulittlemaid.entity.item.AbstractEntityFromItem;
import com.github.tartaricacid.touhoulittlemaid.entity.misc.DefaultMonsterType;
import com.github.tartaricacid.touhoulittlemaid.entity.misc.MonsterType;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitDataAttachment;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Single target-validity chain shared by permanent attack tasks and transient
 * combat. Callers remain responsible for trigger thresholds, range, pathing,
 * and weapon requirements.
 */
public final class MaidTargetingPolicy {
    private static final IMaidHostilityAdapter VANILLA_CONDITIONAL_ADAPTER =
            new VanillaConditionalHostilityAdapter();
    private static final CopyOnWriteArrayList<IMaidHostilityAdapter> EXTERNAL_ADAPTERS =
            new CopyOnWriteArrayList<>();

    private MaidTargetingPolicy() {
    }

    public static boolean registerAdapter(IMaidHostilityAdapter adapter) {
        return EXTERNAL_ADAPTERS.addIfAbsent(Objects.requireNonNull(adapter));
    }

    public static boolean unregisterAdapter(IMaidHostilityAdapter adapter) {
        return EXTERNAL_ADAPTERS.remove(adapter);
    }

    public static boolean canAttackForCurrentTask(EntityMaid maid, LivingEntity target,
                                                   MaidTargetingContext context) {
        if (maid.getTask() instanceof IAttackTask attackTask) {
            return canAttackForTask(maid, target, context, attackTask);
        }
        return canAttackByDefaultRules(maid, target, context);
    }

    public static boolean canAttack(EntityMaid maid, LivingEntity target,
                                    MaidTargetingContext context) {
        if (context == MaidTargetingContext.PLANNED_ATTACK && isOwnerCommandedTarget(maid, target)) {
            // An explicit owner order obeys regardless of default hostility (peaceful / non-angry
            // neutral targets included); only the hard-safety set can still refuse it.
            return canAttackOnOwnerCommand(maid, target);
        }
        return context == MaidTargetingContext.PLANNED_ATTACK
                ? canAttackForCurrentTask(maid, target, context)
                : canAttackByDefaultRules(maid, target, context);
    }

    public static boolean canAttackForTask(EntityMaid maid, LivingEntity target,
                                           MaidTargetingContext context, IAttackTask attackTask) {
        Preclassification preclassification = preclassify(maid, target, context);
        if (preclassification != Preclassification.UNDECIDED) {
            return preclassification == Preclassification.ALLOW;
        }
        return attackTask.canAttack(maid, target);
    }

    public static boolean canAttackByDefaultRules(EntityMaid maid, LivingEntity target,
                                                   MaidTargetingContext context) {
        Preclassification preclassification = preclassify(maid, target, context);
        if (preclassification != Preclassification.UNDECIDED) {
            return preclassification == Preclassification.ALLOW;
        }
        // Transient contexts are created only from a confirmed successful
        // damage signal (or the owner's two-hit confirmation). Do not reuse
        // the planned-task lastHurtMob heuristic here: it is written before
        // projectile impact success and would turn one owner hit into consent.
        if (context != MaidTargetingContext.PLANNED_ATTACK) {
            return true;
        }
        return configuredOrDefaultHostility(maid, target);
    }

    /**
     * Whether an explicit owner command (a skill / LLM tool call) may attack this target. Owner
     * orders are obeyed even for peaceful or non-angry neutral mobs — only the immutable hard-safety
     * set (players, pets, allies, other maids, protected / ignored types) and PROTECTED adapters
     * win. Autonomous targeting still uses the normal hostility rules; this bypass is command-only.
     */
    public static boolean canAttackOnOwnerCommand(EntityMaid maid, LivingEntity target) {
        return !failsHardSafety(maid, target, MaidTargetingContext.PLANNED_ATTACK)
                && adapterDecision(maid, target, MaidTargetingContext.PLANNED_ATTACK)
                != MaidHostilityDecision.PROTECTED;
    }

    private static boolean isOwnerCommandedTarget(EntityMaid maid, LivingEntity target) {
        UUID commanded = maid.getEmergencyCombatManager().getOwnerCommandedAttackTarget();
        return commanded != null && commanded.equals(target.getUUID());
    }

    /**
     * Whether one confirmed successful owner hit may be assisted immediately.
     * Plain peaceful entities intentionally return false until the manager has
     * observed a second independent hit.
     */
    public static boolean isCurrentlyHostile(EntityMaid maid, LivingEntity target,
                                             MaidTargetingContext context) {
        Preclassification preclassification = preclassify(maid, target, context);
        if (preclassification != Preclassification.UNDECIDED) {
            return preclassification == Preclassification.ALLOW;
        }
        var targetTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        AttackListData attackListData = maid.getAttachedOrCreate(InitDataAttachment.ATTACK_LIST);
        MonsterType monsterType = attackListData.attackGroups().containsKey(targetTypeId)
                ? attackListData.attackGroups().get(targetTypeId)
                : DefaultMonsterType.getMonsterType(target);
        return monsterType == MonsterType.HOSTILE;
    }

    /**
     * Owner-confirmed peaceful targets may bypass only the legacy default
     * hostility bucket. Every hard protection, real NeutralMob anger check,
     * and conditional-hostility adapter still wins.
     */
    public static boolean canAttackAfterOwnerConfirmation(EntityMaid maid, LivingEntity target) {
        return preclassify(maid, target, MaidTargetingContext.PROTECT_OWNER)
                == Preclassification.UNDECIDED;
    }

    /**
     * Candidate retention is safe only when immutable protections pass.
     * Neutral and conditional entities may be retained briefly while waiting
     * for their real hostility state to change.
     */
    public static boolean canObserveOwnerIntent(EntityMaid maid, LivingEntity target) {
        if (failsHardSafety(maid, target, MaidTargetingContext.PROTECT_OWNER)) {
            return false;
        }
        return adapterDecision(maid, target, MaidTargetingContext.PROTECT_OWNER)
                != MaidHostilityDecision.PROTECTED;
    }

    public static boolean canContinueTargeting(EntityMaid maid, LivingEntity target,
                                               MaidTargetingContext context) {
        return target.isAlive() && !target.isRemoved() && target.level() == maid.level()
                && canAttack(maid, target, context);
    }

    private static Preclassification preclassify(EntityMaid maid, LivingEntity target,
                                                 MaidTargetingContext context) {
        if (failsHardSafety(maid, target, context)) {
            return Preclassification.DENY;
        }

        MaidHostilityDecision adapterDecision = adapterDecision(maid, target, context);
        if (adapterDecision == MaidHostilityDecision.PROTECTED) {
            return Preclassification.DENY;
        }

        if (target instanceof NeutralMob neutralMob) {
            if (!(maid.level() instanceof ServerLevel serverLevel)) {
                return Preclassification.DENY;
            }
            LivingEntity owner = maid.getOwner();
            boolean angryAtMaid = neutralMob.isAngryAt(maid, serverLevel);
            boolean angryAtOwner = owner != null && owner.level() == maid.level()
                    && neutralMob.isAngryAt(owner, serverLevel);
            return angryAtMaid || angryAtOwner ? Preclassification.ALLOW : Preclassification.DENY;
        }

        if (adapterDecision == MaidHostilityDecision.FRIENDLY) {
            return Preclassification.DENY;
        }
        if (adapterDecision == MaidHostilityDecision.HOSTILE) {
            return Preclassification.ALLOW;
        }
        return Preclassification.UNDECIDED;
    }

    private static boolean failsHardSafety(EntityMaid maid, LivingEntity target,
                                           MaidTargetingContext context) {
        if (target == maid || !target.isAlive() || target.isRemoved() || target.level() != maid.level()) {
            return true;
        }

        var targetTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        if (target instanceof Player || target instanceof ArmorStand || target instanceof AbstractEntityFromItem
                || target instanceof Npc || target instanceof EntityMaid
                || maid.isAlliedTo(target)) {
            return true;
        }
        if (target instanceof OwnableEntity ownable && ownable.getOwnerReference() != null) {
            return true;
        }
        if (target.getCustomName() != null
                && target.getCustomName().getString().startsWith(IAttackTask.MAID_NO_ATTACK_TAG)) {
            return true;
        }
        if (ServerRuleConfig.get(MaidConfig.MAID_ATTACK_IGNORE).contains(targetTypeId.toString())) {
            return true;
        }
        return false;
    }

    private static MaidHostilityDecision adapterDecision(EntityMaid maid, LivingEntity target,
                                                          MaidTargetingContext context) {
        MaidHostilityDecision result = VANILLA_CONDITIONAL_ADAPTER.evaluate(maid, target, context);
        for (IMaidHostilityAdapter adapter : EXTERNAL_ADAPTERS) {
            MaidHostilityDecision next = Objects.requireNonNullElse(
                    adapter.evaluate(maid, target, context), MaidHostilityDecision.PASS);
            if (next == MaidHostilityDecision.PROTECTED) {
                return next;
            }
            if (next == MaidHostilityDecision.FRIENDLY
                    || next == MaidHostilityDecision.HOSTILE && result == MaidHostilityDecision.PASS) {
                result = next;
            }
        }
        return result;
    }

    private static boolean configuredOrDefaultHostility(EntityMaid maid, LivingEntity target) {
        var targetTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        AttackListData attackListData = maid.getAttachedOrCreate(InitDataAttachment.ATTACK_LIST);
        MonsterType monsterType;
        if (attackListData.attackGroups().containsKey(targetTypeId)) {
            monsterType = attackListData.attackGroups().get(targetTypeId);
        } else {
            monsterType = DefaultMonsterType.getMonsterType(target);
        }
        return DefaultMonsterType.canAttack(maid, target, monsterType);
    }

    private enum Preclassification {
        ALLOW,
        DENY,
        UNDECIDED
    }
}
