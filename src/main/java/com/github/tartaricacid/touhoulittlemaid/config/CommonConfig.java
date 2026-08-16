package com.github.tartaricacid.touhoulittlemaid.config;

import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 实例级、**两侧都要读**的配置（{@code config/touhou_little_maid-common.toml}），
 * 向 Forge Config API Port 以 {@code ModConfig.Type.COMMON} 注册，专服上也会生成。
 *
 * <p>本文件是「剩下的那一小撮」，不是杂物间。收录判据只有一条：
 * <b>这个键的消费点里，有没有一处会在专服上执行？</b>有才进来。</p>
 *
 * <ul>
 *   <li>玩法规则**不在这里**：它们是存档级的服务器权威规则，归 {@link ServerConfig} 的 SERVER spec
 *       与 {@link ServerRuleConfig}。</li>
 *   <li>AI 那一组**也不在这里**：实例级 AI 规则归 {@link AiServerRuleConfig}，个人 AI 配置归
 *       {@link AiClientConfig}（各有专属文件）。</li>
 *   <li>玩家个人偏好**也不在这里**：归 {@link GeneralConfig}
 *       （{@code -global.toml}，只在客户端注册）。</li>
 * </ul>
 *
 * <p>代码宿主 {@code origin/26.1} 把上面四样一股脑塞在本文件里，旧值分别由
 * {@link ConfigFileMigration} 的四条迁移各自搬走；本文件因此对老玩家仍是迁移源，
 * {@code LEGACY_FILE_NAME} 这个名字就是这么来的。</p>
 */
public final class CommonConfig {
    public static ModConfigSpec CONFIG;

    public static ModConfigSpec init() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        MaidConfig.initCommon(builder);
        CONFIG = builder.build();
        return CONFIG;
    }
}
