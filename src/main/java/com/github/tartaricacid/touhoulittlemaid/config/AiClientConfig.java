package com.github.tartaricacid.touhoulittlemaid.config;

import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * 个人 AI 配置的专属 spec（{@code config/touhou_little_maid-ai.toml}）。
 *
 * <p>从 {@link CommonConfig} 拆出：AI 这组（语音识别开关 / 来源 / 麦克风 / 聊天距离 / 代理）
 * 与画质、原版替换这类杂项挤在同一个文件里，既不好找，也让「AI 与语音设置」屏的一次保存
 * 牵动整个实例级文件。老值由 {@link ConfigFileMigration#migrateAiFileIfNeeded} 一次性搬来。</p>
 *
 * <p>⚠️ 与行为基准 {@code port/1.21.11-fabric} 的一处结构差异：那边这份注册为
 * {@code ModConfig.Type.CLIENT}（它有 {@code -global.toml} 那一层），<b>本分支没有 global 层</b>，
 * 个人配置一律落在 {@code Type.COMMON}（{@link CommonConfig} 已是此形态）。所有权与语义不变，
 * 只是专服上也会生成这个文件——与 {@code touhou_little_maid-common.toml} 一致。</p>
 *
 * <p>本 spec <b>要注册</b>（与 {@link AiServerRuleConfig} 相反）：个人配置由 FCAP 正常管理，
 * 读点直接 {@code XXX.get()} 即可，不经 {@link ServerRuleConfig#get}。</p>
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
