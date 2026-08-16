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
 * <p>本 spec <b>要注册</b>（与 {@link AiServerRuleConfig} 相反）：个人配置由 FCAP 正常管理，
 * 读点直接 {@code XXX.get()} 即可，不经 {@link ServerRuleConfig#get}。
 * 注册为 {@code ModConfig.Type.CLIENT} 且<b>整段只在物理客户端建立</b>，与
 * {@link GeneralConfig} 同进退——语音识别全程在玩家自己的客户端做，专服不该生成这个文件。</p>
 *
 * <p>⚠️ 因此本 spec 的五个字段在专服上<b>恒为 null</b>。{@code ai/service/stt/STTSite} 里那句
 * {@code new ConfigProxySelector(() -> AIConfig.STT_PROXY_ADDRESS.get())} 之所以安全，
 * 靠的是两道各自独立的判据：① 写成 lambda 而非方法引用，接口初始化时不解引用字段；
 * ② {@code AvailableSites#managesSttSites} 已按物理端挡住，专服根本不读 {@code stt.json}。
 * <b>动这两处任何一处之前，先回到这里。</b></p>
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
