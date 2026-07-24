package com.github.tartaricacid.touhoulittlemaid.config.subconfig;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ExperimentalConfig {
    private static final String TRANSLATE_KEY = "config.touhou_little_maid.experimental";

    /**
     * Opt-in follow tuning. Disabled by default so the origin/1.21.1
     * start-distance, speed and teleport thresholds remain the baseline.
     */
    public static ModConfigSpec.BooleanValue SMOOTH_FOLLOW;

    public static void init(ModConfigSpec.Builder builder) {
        builder.translation(TRANSLATE_KEY).push("experimental");

        builder.comment(
                        "Experimental: start following earlier, move faster, and reserve teleporting for a larger distance",
                        "Disabled preserves the origin/1.21.1 follow behavior")
                .translation(TRANSLATE_KEY + ".smooth_follow");
        SMOOTH_FOLLOW = builder.define("SmoothFollow", false);

        builder.pop();
    }

    private ExperimentalConfig() {
    }
}
