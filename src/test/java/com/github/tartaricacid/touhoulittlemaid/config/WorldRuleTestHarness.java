package com.github.tartaricacid.touhoulittlemaid.config;

import com.electronwill.nightconfig.core.CommentedConfig;

import java.nio.file.Path;

/**
 * 给 config 包之外的测试用的规则搭台工具。
 *
 * <p>{@code ConfigFileMigration.emptyConfig/writeAtomically} 与两个店的 {@code loadFromPath}
 * 都是包内私有——这是有意的（它们不是生产 API），但继承链的回归测试住在 {@code ai.manager.entity}，
 * 需要已加载的规则快照才能测「跟随解析到默认」。桥在这里，仅测试源集可见。</p>
 *
 * <p>§17 v2 后是**两个店**：存档级世界规则（{@code ServerRuleConfig}）与实例级 AI 规则
 * （{@code AiServerRuleConfig}），一起搭。</p>
 */
public final class WorldRuleTestHarness {
    private WorldRuleTestHarness() {
    }

    /** 初始化两个 spec 并从临时目录各加载一份全默认规则；返回世界规则 TOML 路径。 */
    public static Path loadDefaults(Path temporaryDirectory) throws Exception {
        ServerConfig.init();
        AiServerRuleConfig.init();

        CommentedConfig config = ConfigFileMigration.emptyConfig();
        ServerConfig.CONFIG.correct(config);
        Path worldConfig = temporaryDirectory.resolve("world/serverconfig/" + ConfigFileMigration.SERVER_FILE_NAME);
        ConfigFileMigration.writeAtomically(config, worldConfig);
        if (!ServerRuleConfig.loadFromPath(worldConfig)) {
            throw new IllegalStateException("世界规则加载失败：" + worldConfig);
        }

        CommentedConfig aiConfig = ConfigFileMigration.emptyConfig();
        AiServerRuleConfig.SPEC.correct(aiConfig);
        Path aiFile = temporaryDirectory.resolve("config/" + ConfigFileMigration.AI_SERVER_FILE_NAME);
        ConfigFileMigration.writeAtomically(aiConfig, aiFile);
        if (!AiServerRuleConfig.loadFromPath(aiFile)) {
            throw new IllegalStateException("AI 规则加载失败：" + aiFile);
        }
        return worldConfig;
    }

    /** AI 规则的事务式写入口（保存即激活），给继承链测试设默认值用 */
    public static boolean applyAiJson(String json) {
        return AiServerRuleConfig.applyJson(json);
    }
}
