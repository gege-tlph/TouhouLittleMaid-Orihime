package com.github.tartaricacid.touhoulittlemaid.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.VanillaConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 岩浆怪替换开关由上游共享的 ReplaceSlimeModel 拆分而来：旧 TOML 缺新键时必须继承旧史莱姆值，
 * 而不是被 spec 默认值补掉；新键存在时保持独立。自 1.21.11 分支搬入，
 * 迁移目标按本分支适配为 COMMON 文件（{@code touhou_little_maid-common.toml}——
 * 行为基准的 {@code VanillaConfig} 住 global 文件，本分支无 global 层，语义不变）。
 * 基准另有两例依赖 {@code GeneralConfig}/global 建档路径，本分支无该层，不适用故未搬。
 */
class MagmaCubeConfigInheritanceTest {
    private static final String COMMON_FILE = "touhou_little_maid-common.toml";
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
    void oldCommonFileWithSlimeFalseInheritsFalseForMagmaCube() throws Exception {
        Path common = temporaryDirectory.resolve(COMMON_FILE);
        Files.writeString(common, """
                [vanilla]
                ReplaceSlimeModel = false
                ReplaceXPTexture = true
                """, StandardCharsets.UTF_8);

        ConfigFileMigration.inheritMagmaCubeFromSlime(temporaryDirectory);

        CommentedConfig parsed = ConfigFileMigration.read(common);
        assertEquals(false, parsed.getRaw(SLIME));
        assertEquals(false, parsed.getRaw(MAGMA),
                "缺新键的旧文件必须继承 ReplaceSlimeModel=false，而不是恢复 spec 默认");
    }

    @Test
    void explicitMagmaCubeValueKeepsItsIndependentChoice() throws Exception {
        Path common = temporaryDirectory.resolve(COMMON_FILE);
        Files.writeString(common, """
                [vanilla]
                ReplaceSlimeModel = false
                ReplaceMagmaCubeModel = true
                """, StandardCharsets.UTF_8);
        byte[] before = Files.readAllBytes(common);

        ConfigFileMigration.inheritMagmaCubeFromSlime(temporaryDirectory);

        assertTrue(java.util.Arrays.equals(before, Files.readAllBytes(common)),
                "新键已存在时迁移不得改写文件");
        CommentedConfig parsed = ConfigFileMigration.read(common);
        assertEquals(false, parsed.getRaw(SLIME));
        assertEquals(true, parsed.getRaw(MAGMA));
    }

    @Test
    void missingFileIsANoOp() {
        // 全新安装没有 common 文件：迁移必须静默跳过，不能自己造一个文件出来
        ConfigFileMigration.inheritMagmaCubeFromSlime(temporaryDirectory);
        assertTrue(Files.notExists(temporaryDirectory.resolve(COMMON_FILE)));
    }

    /** 接线哨兵：VanillaConfig 五键真的进了 COMMON spec（漏了 CommonConfig.init 里那行就全为 null）。 */
    @Test
    void vanillaTogglesAreWiredIntoTheCommonSpec() {
        CommonConfig.init();
        assertNotNull(VanillaConfig.REPLACE_SLIME_MODEL, "VanillaConfig.init 没有接进 CommonConfig");
        assertEquals(SLIME, VanillaConfig.REPLACE_SLIME_MODEL.getPath());
        assertEquals(MAGMA, VanillaConfig.REPLACE_MAGMA_CUBE_MODEL.getPath());
        // 用户 2026-07-24 定案：五项默认 false（上游默认全 true）
        assertEquals(false, VanillaConfig.REPLACE_SLIME_MODEL.getDefault());
        assertEquals(false, VanillaConfig.REPLACE_TOTEM_TEXTURE.getDefault());
    }
}
