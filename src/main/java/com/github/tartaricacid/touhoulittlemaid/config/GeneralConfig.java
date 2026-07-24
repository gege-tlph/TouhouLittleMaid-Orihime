package com.github.tartaricacid.touhoulittlemaid.config;

import com.github.tartaricacid.touhoulittlemaid.config.subconfig.*;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class GeneralConfig {
    public static ModConfigSpec CONFIG;

    public static ModConfigSpec getConfigSpec() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        MaidConfig.initClient(builder);
        MiscConfig.initClient(builder);
        VanillaConfig.init(builder);
        RenderConfig.init(builder);
        AIConfig.initClient(builder);
        CONFIG = builder.build();
        return CONFIG;
    }

    public static List<ModConfigSpec.ConfigValue<?>> values() {
        return List.of(
                MaidConfig.GLOBAL_MAID_SOUND_FREQUENCY,
                MaidConfig.GLOBAL_MAID_SHOW_CHAT_BUBBLE,
                MiscConfig.CLOSE_OPTIFINE_WARNING,
                MiscConfig.USE_NEW_MAID_FAIRY_MODEL,
                MiscConfig.MODEL_ICON_CACHE,
                MiscConfig.INVULNERABLE_PARTICLE_EFFECT,
                VanillaConfig.REPLACE_SLIME_MODEL,
                VanillaConfig.REPLACE_MAGMA_CUBE_MODEL,
                VanillaConfig.REPLACE_XP_TEXTURE,
                VanillaConfig.REPLACE_TOTEM_TEXTURE,
                VanillaConfig.REPLACE_XP_BOTTLE_TEXTURE,
                RenderConfig.ENABLE_COMPASS_TIP,
                RenderConfig.ENABLE_GOLDEN_APPLE_TIP,
                RenderConfig.ENABLE_POTION_TIP,
                RenderConfig.ENABLE_MILK_BUCKET_TIP,
                RenderConfig.ENABLE_SCRIPT_BOOK_TIP,
                RenderConfig.ENABLE_GLASS_BOTTLE_TIP,
                RenderConfig.ENABLE_NAME_TAG_TIP,
                RenderConfig.ENABLE_LEAD_TIP,
                RenderConfig.ENABLE_SADDLE_TIP,
                RenderConfig.ENABLE_SHEARS_TIP,
                AIConfig.STT_ENABLED,
                AIConfig.STT_TYPE,
                AIConfig.STT_MICROPHONE,
                AIConfig.MAID_CAN_CHAT_DISTANCE,
                AIConfig.STT_PROXY_ADDRESS
        );
    }
}
