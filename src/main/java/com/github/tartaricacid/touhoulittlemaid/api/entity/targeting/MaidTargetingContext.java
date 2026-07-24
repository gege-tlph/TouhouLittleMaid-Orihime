package com.github.tartaricacid.touhoulittlemaid.api.entity.targeting;

/**
 * Why the maid is evaluating a potential combat target.
 * Trigger thresholds and search ranges belong to the caller; this context lets
 * every caller reuse the same safety and hostility policy without conflating
 * a permanent work task with transient protection.
 */
public enum MaidTargetingContext {
    PLANNED_ATTACK,
    SELF_DEFENSE,
    PROTECT_OWNER
}
