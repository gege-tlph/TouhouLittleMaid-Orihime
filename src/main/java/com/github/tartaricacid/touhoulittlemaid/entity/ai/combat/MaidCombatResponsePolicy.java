package com.github.tartaricacid.touhoulittlemaid.entity.ai.combat;

import java.util.Locale;

/**
 * Player intent for transient combat. The serialized name is stable across
 * NBT, network payloads, context, and GUI translation keys.
 */
public enum MaidCombatResponsePolicy {
    OFF,
    SELF_DEFENSE,
    PROTECT_OWNER;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public MaidCombatResponsePolicy previous() {
        MaidCombatResponsePolicy[] values = values();
        return values[(ordinal() - 1 + values.length) % values.length];
    }

    public MaidCombatResponsePolicy next() {
        MaidCombatResponsePolicy[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static MaidCombatResponsePolicy fromSerializedName(String name) {
        for (MaidCombatResponsePolicy policy : values()) {
            if (policy.serializedName().equals(name)) {
                return policy;
            }
        }
        return PROTECT_OWNER;
    }

    public static MaidCombatResponsePolicy fromOrdinal(int ordinal) {
        MaidCombatResponsePolicy[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : PROTECT_OWNER;
    }
}
