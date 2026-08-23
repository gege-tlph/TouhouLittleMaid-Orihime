package com.github.tartaricacid.touhoulittlemaid.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AI 规则迁往**实例级** {@code -ai-server.toml} 的升级兼容。
 *
 * <p>源链 = 本次启动的存档 → 实例模板 {@code -server.toml} → {@code -common.toml}（代码宿主
 * 把 AI 键放在这里），<b>逐键</b>取第一处命中；一次性播种，实例文件已存在就绝不再动。</p>
 *
 * <p>为什么必须逐键而不是整文件二选一：「前一个源是被上一轮迁移新建的、AI 值还留在更老的
 * 文件里」这条升级路径会栽在整文件选择上。{@link #fallsThroughToTheInstanceTemplateWhenTheWorldFileLacksTheKeys}
 * 专钉这一条。</p>
 */
class AiServerRuleMigrationTest {
    private static final List<String> LLM_ENABLED = List.of("ai", "LLMEnabled");
    private static final List<String> DEFAULT_LLM_SITE = List.of("ai", "DefaultLLMSite");
    private static final List<String> MAX_TOKENS = List.of("ai", "MaxTokensPerPlayer");

    @TempDir
    Path configDir;

    private ModConfigSpec aiSpec;

    @BeforeEach
    void buildSpec() {
        this.aiSpec = AiServerRuleConfig.init();
    }

    private Path aiFile() {
        return configDir.resolve(ConfigFileMigration.AI_SERVER_FILE_NAME);
    }

    private CommentedConfig migrateAndRead(Path worldFile) throws IOException {
        ConfigFileMigration.migrateAiServerFileIfNeeded(configDir, worldFile,
                AiServerRuleConfig.values(), this.aiSpec);
        assertTrue(Files.isRegularFile(aiFile()), "migration must create the AI rule file");
        return ConfigFileMigration.read(aiFile());
    }

    @Test
    void carriesOldValuesFromTheWorldFile() throws IOException {
        Path worldFile = configDir.resolve("world-server.toml");
        Files.writeString(worldFile, """
                [ai]
                LLMEnabled = false
                DefaultLLMSite = "gemini"
                """);

        CommentedConfig migrated = migrateAndRead(worldFile);
        assertEquals(false, migrated.getRaw(LLM_ENABLED));
        assertEquals("gemini", migrated.getRaw(DEFAULT_LLM_SITE));
        // 旧文件没写的键补 spec 默认，不许缺席
        assertEquals(Integer.MAX_VALUE, (int) migrated.getRaw(MAX_TOKENS));
    }

    @Test
    void fallsThroughToTheInstanceTemplateWhenTheWorldFileLacksTheKeys() throws IOException {
        Path worldFile = configDir.resolve("world-server.toml");
        Files.writeString(worldFile, """
                [maid]
                MaidWorkRange = 16
                """);
        Files.writeString(configDir.resolve(ConfigFileMigration.SERVER_FILE_NAME), """
                [ai]
                DefaultLLMSite = "acme"
                """);

        CommentedConfig migrated = migrateAndRead(worldFile);
        assertEquals("acme", migrated.getRaw(DEFAULT_LLM_SITE));
    }

    @Test
    void fallsThroughToTheCommonFileWhichIsWhereTheHostKeepsThem() throws IOException {
        // 代码宿主 origin/26.1 把 AI 键放在 COMMON spec，也就是玩家现有的 -common.toml。
        // 这是本分支最常见的升级路径：世界文件与实例模板都没有 ai 节。
        Path worldFile = configDir.resolve("world-server.toml");
        Files.writeString(worldFile, """
                [maid]
                MaidWorkRange = 16
                """);
        Files.writeString(configDir.resolve("touhou_little_maid-common.toml"), """
                [ai]
                LLMEnabled = false
                MaxTokensPerPlayer = 4096
                """);

        CommentedConfig migrated = migrateAndRead(worldFile);
        assertEquals(false, migrated.getRaw(LLM_ENABLED));
        assertEquals(4096, (int) migrated.getRaw(MAX_TOKENS));
    }

    @Test
    void neverTouchesAnExistingAiRuleFile() throws IOException {
        Files.writeString(aiFile(), """
                [ai]
                DefaultLLMSite = "keepme"
                """);
        Path worldFile = configDir.resolve("world-server.toml");
        Files.writeString(worldFile, """
                [ai]
                DefaultLLMSite = "clobber"
                """);

        ConfigFileMigration.migrateAiServerFileIfNeeded(configDir, worldFile,
                AiServerRuleConfig.values(), this.aiSpec);

        CommentedConfig existing = ConfigFileMigration.read(aiFile());
        assertEquals("keepme", existing.getRaw(DEFAULT_LLM_SITE), "migration must be one-shot");
    }

    @Test
    void writesSpecDefaultsWhenThereIsNothingToMigrate() throws IOException {
        CommentedConfig migrated = migrateAndRead(configDir.resolve("no-such-world-file.toml"));
        assertEquals(true, migrated.getRaw(LLM_ENABLED));
        assertEquals("", migrated.getRaw(DEFAULT_LLM_SITE));
    }

    @Test
    void anUnreadableSourceIsSkippedInsteadOfAbortingTheWholeMigration() throws IOException {
        // 一个坏文件不该让其余源里的旧值全丢——「加载失败就整批放弃」是本仓库栽过的形态
        Path worldFile = configDir.resolve("world-server.toml");
        Files.writeString(worldFile, "this is not valid toml = = =");
        Files.writeString(configDir.resolve("touhou_little_maid-common.toml"), """
                [ai]
                DefaultLLMSite = "survivor"
                """);

        CommentedConfig migrated = migrateAndRead(worldFile);
        assertEquals("survivor", migrated.getRaw(DEFAULT_LLM_SITE),
                "坏掉的第一个源必须被跳过，而不是让整条迁移放弃");
    }
}
