package com.github.tartaricacid.touhoulittlemaid.config;

import com.github.tartaricacid.touhoulittlemaid.config.subconfig.*;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 实例级配置（{@code config/touhou_little_maid-common.toml}），向 Forge Config API Port 注册。
 *
 * <p>玩法规则**不在这里**：它们是存档级的服务器权威规则，归 {@link ServerConfig} 的 SERVER spec
 * 与 {@link ServerRuleConfig}。原本代码宿主 {@code origin/26.1} 把两者混放在本文件里。</p>
 *
 * <p>**AI 那一组也不在这里**：实例级 AI 规则归 {@link AiServerRuleConfig}，个人 AI 配置归
 * {@link AiClientConfig}（各有专属文件）。宿主同样把这两半一起塞在本文件的 {@code [ai]} 节里，
 * 旧值由 {@link ConfigFileMigration#migrateAiServerFileIfNeeded} 与
 * {@link ConfigFileMigration#migrateAiFileIfNeeded} 各自搬走。</p>
 */
public final class CommonConfig {
    public static ModConfigSpec CONFIG;

    public static ModConfigSpec init() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        MaidConfig.initCommon(builder);
        MiscConfig.initCommon(builder);
        // 原版替换五开关：实例级个人配置（按 1.21.11 交接裁决进 COMMON 侧；行为基准放在 global 文件，
        // 本分支无 global 层，落在 common 同为个人所有权，语义不变）
        VanillaConfig.init(builder);
        RenderConfig.init(builder);
        CONFIG = builder.build();
        return CONFIG;
    }
}
