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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 个人 AI 配置拆分（{@code -common.toml} 的 {@code [ai]} 节 → {@code -ai.toml}）的升级兼容：
 * 老值必须原样搬来，且只搬一次。
 *
 * <p>⚠️ 与行为基准 {@code port/1.21.11-fabric} 的同名用例有一处结构差异：那边的源链是
 * {@code -global.toml → -client.toml → -common.toml} 三级，故它有一条「global 里没有 ai 节时
 * 回落到更老文件」的用例；<b>本分支没有前两层</b>（无 global 层，个人配置一律在 common），
 * 源链只有一处，那条用例在这里无从失败，写了也是恒绿，故不写——
 * 「逐键取第一处命中」这条性质由服务端侧的 {@code AiServerRuleMigrationTest} 覆盖，那边源链是三级。</p>
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

    private Path commonFile() {
        return configDir.resolve("touhou_little_maid-common.toml");
    }

    private CommentedConfig migrateAndRead() throws IOException {
        ConfigFileMigration.migrateAiFileIfNeeded(configDir, AiClientConfig.values(), this.aiSpec);
        assertTrue(Files.isRegularFile(aiFile()), "migration must create the AI config file");
        return ConfigFileMigration.read(aiFile());
    }

    @Test
    void carriesOldValuesFromTheCommonFile() throws IOException {
        Files.writeString(commonFile(), """
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
    void leavesTheServerRuleHalfBehind() throws IOException {
        // 宿主把两种所有权混在同一个 [ai] 节里。个人这一份只准搬走自己认领的五项，
        // LLM/TTS 那半归 AiServerRuleConfig，混进来就等于把服务器权威值复制成了个人配置。
        Files.writeString(commonFile(), """
                [ai]
                STTEnabled = false
                LLMEnabled = false
                TTSEnabled = false
                """);

        CommentedConfig migrated = migrateAndRead();
        assertEquals(false, migrated.getRaw(STT_ENABLED));
        assertNull(migrated.getRaw(List.of("ai", "LLMEnabled")),
                "LLM 是实例级 AI 规则，不该出现在个人配置文件里");
        assertNull(migrated.getRaw(List.of("ai", "TTSEnabled")),
                "TTS 是实例级 AI 规则，不该出现在个人配置文件里");
    }

    @Test
    void neverTouchesAnExistingAiFile() throws IOException {
        Files.writeString(aiFile(), """
                [ai]
                MaidCanChatDistance = 7
                """);
        Files.writeString(commonFile(), """
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
