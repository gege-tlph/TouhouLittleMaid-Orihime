package com.github.tartaricacid.touhoulittlemaid.config.subconfig;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 实验性玩法开关。**存档级世界规则**（与 {@code MaidConfig} 等同族，经
 * {@code ServerRuleConfig} 的唯一读口读取），不是个人偏好——跟随手感影响服务端 AI，
 * 由服主统一决定。
 */
public final class ExperimentalConfig {
    private static final String TRANSLATE_KEY = "config.touhou_little_maid.experimental";

    /**
     * Opt-in: let the maid's play snowball knock players back.
     *
     * <p>Disabled by default so vanilla semantics remain the baseline — a snowball deals 0 damage
     * to anything but a blaze, and {@code Player.hurtServer} returns false at {@code amount == 0}
     * before ever reaching the knockback block in {@code LivingEntity.hurtServer}. Mobs are
     * knocked back either way, so this switch only ever changes what players feel.</p>
     */
    public static ModConfigSpec.BooleanValue SNOWBALL_KNOCKBACK;

    public static void initServerRule(ModConfigSpec.Builder builder) {
        builder.translation(TRANSLATE_KEY).push("experimental");

        builder.comment(
                        "Experimental: the maid's play snowball knocks players back, the way it already knocks mobs back",
                        "Disabled preserves vanilla, where a 0 damage hit on a player is dropped before knockback")
                .translation(translateKey("snowball_knockback"));
        SNOWBALL_KNOCKBACK = builder.define("SnowballKnockback", false);

        builder.pop();
    }

    private static String translateKey(String key) {
        return TRANSLATE_KEY + "." + key;
    }

    private ExperimentalConfig() {
    }
}
