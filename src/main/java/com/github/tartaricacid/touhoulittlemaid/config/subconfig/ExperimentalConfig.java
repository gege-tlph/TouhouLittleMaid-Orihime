package com.github.tartaricacid.touhoulittlemaid.config.subconfig;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ExperimentalConfig {
    private static final String TRANSLATE_KEY = "config.touhou_little_maid.experimental";


    /**
     * 可选的平滑跟随调优。默认关闭，以保留标准的跟随距离、移动速度和传送阈值。
     */
    public static ModConfigSpec.BooleanValue SMOOTH_FOLLOW;

    public static void init(ModConfigSpec.Builder builder) {
        builder.translation(TRANSLATE_KEY).push("experimental");

        builder.comment(
                        "实验功能：更早开始跟随、提高移动速度，并仅在距离较远时传送",
                        "禁用时使用标准跟随行为")
                .translation(TRANSLATE_KEY + ".smooth_follow");
        SMOOTH_FOLLOW = builder.define("SmoothFollow", false);

        builder.pop();
    }

    private ExperimentalConfig() {
    }
}
