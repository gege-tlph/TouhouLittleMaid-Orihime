package com.github.tartaricacid.touhoulittlemaid.config;

import com.github.tartaricacid.touhoulittlemaid.config.subconfig.*;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 实例级配置（{@code config/touhou_little_maid-common.toml}），向 Forge Config API Port 注册。
 *
 * <p>玩法规则**不在这里**：它们是存档级的服务器权威规则，归 {@link ServerConfig} 的 SERVER spec
 * 与 {@link ServerRuleConfig}。原本代码宿主 {@code origin/26.1} 把两者混放在本文件里。</p>
 */
public final class CommonConfig {
    public static ModConfigSpec CONFIG;

    public static ModConfigSpec init() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        MaidConfig.initCommon(builder);
        MiscConfig.initCommon(builder);
        RenderConfig.init(builder);
        AIConfig.init(builder);
        CONFIG = builder.build();
        return CONFIG;
    }
}
