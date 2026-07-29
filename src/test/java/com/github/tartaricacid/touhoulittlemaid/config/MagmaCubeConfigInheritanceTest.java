package com.github.tartaricacid.touhoulittlemaid.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 岩浆怪替换开关由上游共享的 ReplaceSlimeModel 拆分而来：旧 global TOML 缺新键时
 * 必须继承旧史莱姆值，而不是被 spec 默认 true 补掉；新键存在时保持独立。
 */
class MagmaCubeConfigInheritanceTest {
    private static final List<String> SLIME = List.of("vanilla", "ReplaceSlimeModel");
    private static final List<String> MAGMA = List.of("vanilla", "ReplaceMagmaCubeModel");

    @TempDir
    Path temporaryDirectory;

    @BeforeAll
    static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Test
    void oldGlobalFileWithSlimeFalseInheritsFalseForMagmaCube() throws Exception {
        Path global = temporaryDirectory.resolve(ConfigFileMigration.GLOBAL_FILE_NAME);
        Files.writeString(global, """
                [vanilla]
                ReplaceSlimeModel = false
                ReplaceXPTexture = true
                """, StandardCharsets.UTF_8);

        ConfigFileMigration.inheritMagmaCubeFromSlime(temporaryDirectory);

        CommentedConfig parsed = ConfigFileMigration.read(global);
        assertEquals(false, parsed.getRaw(SLIME));
        assertEquals(false, parsed.getRaw(MAGMA),
                "缺新键的旧文件必须继承 ReplaceSlimeModel=false，而不是恢复默认 true");
    }

    @Test
    void freshDefaultGlobalFileHasBothSwitchesAtSpecDefault() throws Exception {
        // 用户 2026-07-24 定案：五项替换开关默认 false，新建 global 文件应写入 spec 默认 false。
        var spec = GeneralConfig.getConfigSpec();
        ConfigFileMigration.migrateGlobalFileIfNeeded(temporaryDirectory, GeneralConfig.values(), spec);
        Path global = temporaryDirectory.resolve(ConfigFileMigration.GLOBAL_FILE_NAME);

        ConfigFileMigration.inheritMagmaCubeFromSlime(temporaryDirectory);

        CommentedConfig parsed = ConfigFileMigration.read(global);
        assertEquals(false, parsed.getRaw(SLIME));
        assertEquals(false, parsed.getRaw(MAGMA));
    }

    @Test
    void explicitMagmaCubeValueKeepsItsIndependentChoice() throws Exception {
        Path global = temporaryDirectory.resolve(ConfigFileMigration.GLOBAL_FILE_NAME);
        Files.writeString(global, """
                [vanilla]
                ReplaceSlimeModel = false
                ReplaceMagmaCubeModel = true
                """, StandardCharsets.UTF_8);
        byte[] before = Files.readAllBytes(global);

        ConfigFileMigration.inheritMagmaCubeFromSlime(temporaryDirectory);

        assertTrue(java.util.Arrays.equals(before, Files.readAllBytes(global)),
                "新键已存在时迁移不得改写文件");
        CommentedConfig parsed = ConfigFileMigration.read(global);
        assertEquals(false, parsed.getRaw(SLIME));
        assertEquals(true, parsed.getRaw(MAGMA));
    }

    @Test
    void legacyCommonFileWithSlimeFalseCreatesGlobalWithMagmaFalse() throws Exception {
        Files.writeString(temporaryDirectory.resolve("touhou_little_maid-common.toml"), """
                [vanilla]
                ReplaceSlimeModel = false
                """, StandardCharsets.UTF_8);

        var spec = GeneralConfig.getConfigSpec();
        ConfigFileMigration.migrateGlobalFileIfNeeded(temporaryDirectory, GeneralConfig.values(), spec);

        CommentedConfig parsed = ConfigFileMigration.read(
                temporaryDirectory.resolve(ConfigFileMigration.GLOBAL_FILE_NAME));
        assertEquals(false, parsed.getRaw(SLIME));
        assertEquals(false, parsed.getRaw(MAGMA),
                "legacy 单开关文件建 global 时岩浆怪必须继承史莱姆旧值");
    }

    @Test
    void inheritanceRewritePreservesUnknownThirdPartyKeys() throws Exception {
        Path global = temporaryDirectory.resolve(ConfigFileMigration.GLOBAL_FILE_NAME);
        Files.writeString(global, """
                [vanilla]
                ReplaceSlimeModel = false
                ThirdPartyUnknownKey = "keep-me"

                [some_addon]
                custom = 42
                """, StandardCharsets.UTF_8);

        ConfigFileMigration.inheritMagmaCubeFromSlime(temporaryDirectory);

        CommentedConfig parsed = ConfigFileMigration.read(global);
        assertEquals(false, parsed.getRaw(MAGMA));
        assertEquals("keep-me", parsed.getRaw(List.of("vanilla", "ThirdPartyUnknownKey")));
        assertEquals(42, ((Number) parsed.getRaw(List.of("some_addon", "custom"))).intValue());
        try (var files = Files.list(temporaryDirectory)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().endsWith(".tmp")),
                    "事务写盘不得残留临时文件");
        }
    }
}
