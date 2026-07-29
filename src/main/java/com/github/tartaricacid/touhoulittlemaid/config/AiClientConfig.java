package com.github.tartaricacid.touhoulittlemaid.config;

import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * 个人 AI 配置的专属 spec（{@code touhou_little_maid-ai.toml}）。
 *
 * <p>2026-07-28 按用户定案从 {@code -global.toml} 拆出：AI 这组（语音识别开关/来源/麦克风/
 * 聊天距离/代理）与画质、提示这类杂项挤在同一个文件里，既不好找也让「AI 与语音设置」屏
 * 的保存牵动整个全局文件。老值由 {@link ConfigFileMigration#migrateAiFileIfNeeded} 一次性搬来。</p>
 */
public final class AiClientConfig {
    public static ModConfigSpec CONFIG;

    private AiClientConfig() {
    }

    public static ModConfigSpec getConfigSpec() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        AIConfig.initClient(builder);
        CONFIG = builder.build();
        return CONFIG;
    }

    public static List<ModConfigSpec.ConfigValue<?>> values() {
        return List.of(
                AIConfig.STT_ENABLED,
                AIConfig.STT_TYPE,
                AIConfig.STT_MICROPHONE,
                AIConfig.MAID_CAN_CHAT_DISTANCE,
                AIConfig.STT_PROXY_ADDRESS
        );
    }
}
