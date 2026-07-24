package com.github.tartaricacid.touhoulittlemaid.config.subconfig;

import net.neoforged.neoforge.common.ModConfigSpec;

public class VanillaConfig {
    private static final String TRANSLATE_KEY = "config.touhou_little_maid.vanilla";
    public static ModConfigSpec.BooleanValue REPLACE_SLIME_MODEL;
    public static ModConfigSpec.BooleanValue REPLACE_MAGMA_CUBE_MODEL;
    public static ModConfigSpec.BooleanValue REPLACE_XP_TEXTURE;
    public static ModConfigSpec.BooleanValue REPLACE_TOTEM_TEXTURE;
    public static ModConfigSpec.BooleanValue REPLACE_XP_BOTTLE_TEXTURE;

    public static void init(ModConfigSpec.Builder builder) {
        builder.translation(TRANSLATE_KEY).push("vanilla");

        // 默认值按用户 2026-07-24 定案改为 false：上游 origin/1.21.1 这五项默认全部替换（true），
        // 本项目有意让原版模型/纹理默认保留，需要油库里/点符时由玩家在配置界面主动开启。
        builder.comment("Whether to replace the vanilla slime model with the yukkuri.")
                .translation(translateKey("replace_slime_model"));
        REPLACE_SLIME_MODEL = builder.define("ReplaceSlimeModel", false);

        // 上游史莱姆与岩浆怪共用 ReplaceSlimeModel；本项目按用户要求拆分为独立开关。
        // 旧配置缺此键时由 ConfigFileMigration 继承 ReplaceSlimeModel 的旧值，而非无条件恢复默认。
        builder.comment("Whether to replace the vanilla magma cube model with the yukkuri.")
                .translation(translateKey("replace_magma_cube_model"));
        REPLACE_MAGMA_CUBE_MODEL = builder.define("ReplaceMagmaCubeModel", false);

        builder.comment("Whether to replace the vanilla xp orb texture with the point items.")
                .translation(translateKey("replace_xp_texture"));
        REPLACE_XP_TEXTURE = builder.define("ReplaceXPTexture", false);

        builder.comment("Whether to replace the vanilla totem texture with the life point.")
                .translation(translateKey("replace_totem_texture"));
        REPLACE_TOTEM_TEXTURE = builder.define("ReplaceTotemTexture", false);

        builder.comment("Whether to replace the vanilla bottle of xp texture with the point items.")
                .translation(translateKey("replace_xp_bottle_texture"));
        REPLACE_XP_BOTTLE_TEXTURE = builder.define("ReplaceXPBottleTexture", false);

        builder.pop();
    }

    private static String translateKey(String key) {
        return TRANSLATE_KEY + "." + key;
    }
}