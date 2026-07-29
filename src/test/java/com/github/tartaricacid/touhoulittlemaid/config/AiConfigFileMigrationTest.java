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
 * 个人 AI 配置拆分（-global.toml → -ai.toml）的升级兼容：老值必须原样搬来，且只搬一次。
 *
 * <p>候选链是逐键取第一处命中，不是整文件二选一——「global 是后来新建的、AI 值还留在
 * 更老的 -client.toml 里」这条升级路径会栽在整文件选择上，这里专门钉一条。</p>
 */
class AiConfigFileMigrationTest {
    private static final List<String> STT_ENABLED = List.of("ai", "STTEnabled");
    private static final List<String> STT_TYPE = List.of("ai", "STTType");
    private static final List<String> DISTANCE = List.of("ai", "MaidCanChatDistance");
    private static final List<String> MICROPHONE = List.of("ai", "STTMicrophone");

    @TempDir
    Path configDir;

    private ModConfigSpec aiSpec;

    @BeforeEach
    void buildSpec() {
        this.aiSpec = AiClientConfig.getConfigSpec();
    }

    private Path aiFile() {
        return configDir.resolve(ConfigFileMigration.AI_FILE_NAME);
    }

    private CommentedConfig migrateAndRead() throws IOException {
        ConfigFileMigration.migrateAiFileIfNeeded(configDir, AiClientConfig.values(), this.aiSpec);
        assertTrue(Files.isRegularFile(aiFile()), "migration must create the AI config file");
        return ConfigFileMigration.read(aiFile());
    }

    @Test
    void carriesOldValuesFromTheGlobalFile() throws IOException {
        Files.writeString(configDir.resolve(ConfigFileMigration.GLOBAL_FILE_NAME), """
                [ai]
                STTEnabled = false
                STTType = "TENCENT"
                MaidCanChatDistance = 33
                """);

        CommentedConfig migrated = migrateAndRead();
        assertEquals(false, migrated.getRaw(STT_ENABLED));
        assertEquals("TENCENT", migrated.getRaw(STT_TYPE));
        assertEquals(33, (int) migrated.getRaw(DISTANCE));
        // 旧文件没写的键补 spec 默认，不许缺席
        assertEquals("", migrated.getRaw(MICROPHONE));
    }

    @Test
    void fallsThroughToOlderFilesWhenTheGlobalFileLacksTheSection() throws IOException {
        // global 存在但没有 ai 节（例如它本身是被上一轮迁移新建的）；值躺在更老的 -client.toml 里
        Files.writeString(configDir.resolve(ConfigFileMigration.GLOBAL_FILE_NAME), """
                [vanilla]
                ReplaceSlimeModel = true
                """);
        Files.writeString(configDir.resolve("touhou_little_maid-client.toml"), """
                [ai]
                STTEnabled = false
                MaidCanChatDistance = 21
                """);

        CommentedConfig migrated = migrateAndRead();
        assertEquals(false, migrated.getRaw(STT_ENABLED));
        assertEquals(21, (int) migrated.getRaw(DISTANCE));
    }

    @Test
    void neverTouchesAnExistingAiFile() throws IOException {
        Files.writeString(aiFile(), """
                [ai]
                MaidCanChatDistance = 7
                """);
        Files.writeString(configDir.resolve(ConfigFileMigration.GLOBAL_FILE_NAME), """
                [ai]
                MaidCanChatDistance = 99
                """);

        ConfigFileMigration.migrateAiFileIfNeeded(configDir, AiClientConfig.values(), this.aiSpec);

        CommentedConfig existing = ConfigFileMigration.read(aiFile());
        assertEquals(7, (int) existing.getRaw(DISTANCE), "migration must be one-shot");
    }

    @Test
    void writesSpecDefaultsWhenThereIsNothingToMigrate() throws IOException {
        CommentedConfig migrated = migrateAndRead();
        assertEquals(true, migrated.getRaw(STT_ENABLED));
        assertEquals(12, (int) migrated.getRaw(DISTANCE));
    }
}
