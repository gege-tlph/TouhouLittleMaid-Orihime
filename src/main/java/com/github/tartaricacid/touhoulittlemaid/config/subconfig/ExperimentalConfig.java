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
     * Opt-in follow tuning. Disabled by default so the origin/1.21.1
     * start-distance, speed and teleport thresholds remain the baseline.
     */
    public static ModConfigSpec.BooleanValue SMOOTH_FOLLOW;

    public static void initServerRule(ModConfigSpec.Builder builder) {
        builder.translation(TRANSLATE_KEY).push("experimental");

        builder.comment(
                        "Experimental: start following earlier, move faster, and reserve teleporting for a larger distance",
                        "Disabled preserves the origin/1.21.1 follow behavior")
                .translation(translateKey("smooth_follow"));
        SMOOTH_FOLLOW = builder.define("SmoothFollow", false);

        builder.pop();
    }

    private static String translateKey(String key) {
        return TRANSLATE_KEY + "." + key;
    }

    private ExperimentalConfig() {
    }
}
