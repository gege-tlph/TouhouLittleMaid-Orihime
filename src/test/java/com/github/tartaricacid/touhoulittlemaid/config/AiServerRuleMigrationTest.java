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
 * AI 规则从存档级世界文件迁往实例级 -ai-server.toml（§17 v2）的升级兼容。
 *
 * <p>源链 = 本次启动的存档 → 实例模板 -server.toml → legacy，逐键取第一处命中；
 * 一次性播种，实例文件已存在就绝不再动。与客户端侧 {@code AiConfigFileMigrationTest} 同构。</p>
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
                MaidTamedItem = "minecraft:cake"
                """);
        Files.writeString(configDir.resolve(ConfigFileMigration.SERVER_FILE_NAME), """
                [ai]
                DefaultLLMSite = "acme"
                """);

        CommentedConfig migrated = migrateAndRead(worldFile);
        assertEquals("acme", migrated.getRaw(DEFAULT_LLM_SITE));
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
}
