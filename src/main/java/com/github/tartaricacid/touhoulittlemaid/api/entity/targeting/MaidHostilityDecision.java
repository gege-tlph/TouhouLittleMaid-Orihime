package com.github.tartaricacid.touhoulittlemaid.api.entity.targeting;

/**
 * Adapter result consumed by the maid's unified targeting policy.
 */
public enum MaidHostilityDecision {
    /** The adapter does not recognize this target. */
    PASS,
    /** An unconditionally protected target, such as a mod-defined tamed mob. */
    PROTECTED,
    /** A recognized target that is not currently hostile to this maid or owner. */
    FRIENDLY,
    /** A recognized target that is currently hostile to this maid or owner. */
    HOSTILE
}
