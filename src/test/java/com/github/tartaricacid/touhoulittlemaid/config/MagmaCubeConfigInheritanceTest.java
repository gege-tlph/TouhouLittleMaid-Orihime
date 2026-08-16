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
 * 而不是被 spec 默认值补掉；新键存在时保持独立。自 1.21.11 分支搬入。
 *
 * <p>⚠️ 本类看管的是**迁移源**侧：{@code inheritMagmaCubeFromSlime} 作用在代码宿主留下的
 * {@code touhou_little_maid-common.toml} 上，而 {@code VanillaConfig} 五键如今住
 * {@code touhou_little_maid-global.toml}（{@link GeneralConfig}）。两者靠**次序**衔接——
 * 先补源、再由 {@code migrateGlobalFileIfNeeded} 逐键搬走。下面
 * {@code slimeChoiceSurvivesAllTheWayIntoTheGlobalFile} 走的就是这条完整链路，
 * 它才是玩家真正会经历的那条路；单独的源侧三例是它的分解动作。</p>
 */
class MagmaCubeConfigInheritanceTest {
    private static final String COMMON_FILE = "touhou_little_maid-common.toml";
    private static final String GLOBAL_FILE = "touhou_little_maid-global.toml";
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

    /** 接线哨兵：VanillaConfig 五键真的进了 CLIENT spec（漏了 GeneralConfig 里那行就全为 null）。 */
    @Test
    void vanillaTogglesAreWiredIntoTheGlobalSpec() {
        GeneralConfig.getConfigSpec();
        assertNotNull(VanillaConfig.REPLACE_SLIME_MODEL, "VanillaConfig.init 没有接进 GeneralConfig");
        assertEquals(SLIME, VanillaConfig.REPLACE_SLIME_MODEL.getPath());
        assertEquals(MAGMA, VanillaConfig.REPLACE_MAGMA_CUBE_MODEL.getPath());
        // 用户 2026-07-24 定案：五项默认 false（上游默认全 true）
        assertEquals(false, VanillaConfig.REPLACE_SLIME_MODEL.getDefault());
        assertEquals(false, VanillaConfig.REPLACE_TOTEM_TEXTURE.getDefault());
    }

    /**
     * 端到端：玩家在旧 {@code -common.toml} 里调过的值，必须一个不少地出现在新建的
     * {@code -global.toml} 里——包括那个「旧文件根本没有、要从史莱姆开关继承」的岩浆怪键。
     *
     * <p>这一条钉的是**次序**。把入口里 {@code inheritMagmaCubeFromSlime()} 挪到
     * {@code migrateGlobalFileIfNeeded} 之后，岩浆怪那行当场变回默认 false 而其余全绿——
     * 一个只在「升级 + 老文件 + 没碰过岩浆怪开关」时才发作、且没有任何报错的静默丢值。</p>
     */
    @Test
    void slimeChoiceSurvivesAllTheWayIntoTheGlobalFile() throws Exception {
        Path common = temporaryDirectory.resolve(COMMON_FILE);
        Files.writeString(common, """
                [vanilla]
                ReplaceSlimeModel = true
                [misc]
                CloseOptifineWarning = true
                """, StandardCharsets.UTF_8);

        // 入口里的真实次序：先补源，再建目标
        ConfigFileMigration.inheritMagmaCubeFromSlime(temporaryDirectory);
        ConfigFileMigration.migrateGlobalFileIfNeeded(temporaryDirectory,
                GeneralConfig.values(), GeneralConfig.getConfigSpec());

        CommentedConfig global = ConfigFileMigration.read(temporaryDirectory.resolve(GLOBAL_FILE));
        assertEquals(true, global.getRaw(SLIME), "玩家调过的史莱姆开关必须搬进 -global.toml");
        assertEquals(true, global.getRaw(MAGMA),
                "岩浆怪开关必须继承史莱姆的旧值——次序反了这里会变回默认 false，且不会有任何报错");
        assertEquals(true, global.getRaw(List.of("misc", "CloseOptifineWarning")),
                "其余个人偏好同样要逐键搬过来");
    }

    /**
     * {@code GeneralConfig.values()} 必须覆盖它 spec 里的**每一个**键。
     * 漏登记一个 = 那个键的旧值在升级时静默丢失（新文件按默认值建，旧文件随后被 correct 剥掉），
     * 而任何一条只看「某几个键搬对了」的用例都照不出来。
     *
     * <p>判据从**独立来源**重建：拿 spec 自己 correct 出来的默认配置去数叶子节点，
     * 不拿 values() 去数 values()——自证式断言等于没看管。</p>
     */
    @Test
    void everyKeyInTheGlobalSpecIsClaimedForMigration() {
        CommentedConfig defaults = ConfigFileMigration.emptyConfig();
        GeneralConfig.getConfigSpec().correct(defaults);

        java.util.Set<List<String>> claimed = new java.util.HashSet<>();
        GeneralConfig.values().forEach(value -> claimed.add(value.getPath()));

        java.util.List<List<String>> leaves = new java.util.ArrayList<>();
        collectLeaves(defaults, List.of(), leaves);

        assertTrue(leaves.size() >= 21,
                "活性：spec 里至少该有 21 个键，只数到 " + leaves.size() + " 个——解析坏了，不是没差异");
        for (List<String> leaf : leaves) {
            assertTrue(claimed.contains(leaf),
                    "GeneralConfig.values() 漏登记 " + leaf + "，升级时它的旧值会静默丢失");
        }
    }

    private static void collectLeaves(com.electronwill.nightconfig.core.UnmodifiableConfig config,
                                      List<String> prefix, java.util.List<List<String>> out) {
        config.valueMap().forEach((key, value) -> {
            java.util.List<String> path = new java.util.ArrayList<>(prefix);
            path.add(key);
            if (value instanceof com.electronwill.nightconfig.core.UnmodifiableConfig nested) {
                collectLeaves(nested, List.copyOf(path), out);
            } else {
                out.add(List.copyOf(path));
            }
        });
    }
}
