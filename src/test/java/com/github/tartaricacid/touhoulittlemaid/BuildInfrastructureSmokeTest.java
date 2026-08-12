package com.github.tartaricacid.touhoulittlemaid;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 证明「测试设施真的会执行」，而不只是任务存在。
 *
 * <p>本分支的 JUnit 依赖、{@code useJUnitPlatform()} 与 {@code src/test} 是从 1.21.11 分支
 * 照搬过来的。搬完第一次跑 {@code build}，{@code :test} 报的是 {@code NO-SOURCE}——Gradle
 * 直接跳过，看上去和"通过"一模一样。本仓库已经为这种「编译打包启动全正常、功能从不执行」的
 * 纸面接口栽过多次，所以这里放一个必然执行的用例当探针：**它一旦不出现在测试报告里，
 * 就说明测试层又被跳过了。**
 *
 * <p>顺带钉住两件与移植直接相关的事实，避免用例沦为 {@code assertTrue(true)}：
 * modid 不随品牌改名而变；测试源码目录必须处于版本控制之下（新基的 {@code .gitignore}
 * 曾把它整个忽略掉，而 Gradle 照编译不误）。
 */
class BuildInfrastructureSmokeTest {
    /**
     * ⚠️ {@code build.gradle} 把测试的 {@code workingDir} 设成了 {@code build/test-working}，
     * 因此**所有读文件的测试都必须先退回两级**才能拿到项目根。这条约定随测试配置一起从
     * 1.21.11 分支带过来，那边的用例（如 lang 键覆盖测试）同样是这么写的——
     * 搬用例过来时若照抄了相对路径却漏了这一层，症状是「找不到文件」而不是断言不成立。
     */
    private static final Path ROOT = Path.of("..", "..");
    private static final Path MOD_METADATA = ROOT.resolve(Path.of("src", "main", "resources", "fabric.mod.json"));

    @Test
    void modIdStaysStableAcrossBranding() throws IOException {
        assertTrue(Files.isRegularFile(MOD_METADATA), "找不到 " + MOD_METADATA.toAbsolutePath());
        String json = Files.readString(MOD_METADATA, StandardCharsets.UTF_8);
        assertTrue(json.contains("\"id\": \"touhou_little_maid\"")
                        || json.contains("\"id\":\"touhou_little_maid\""),
                "modid 必须保持 touhou_little_maid：它决定存档、注册表与第三方兼容的对接键");
    }

    @Test
    void mockitoIsOnTheTestRuntimeClasspath() {
        // 1.21.11 分支的契约测试大量依赖 Mockito；这里只验它在测试运行时可加载，
        // 否则那批测试搬过来会在运行期才炸，而不是在这一步。
        assertTrue(getClass().getClassLoader().getResource("org/mockito/Mockito.class") != null
                        || tryLoad("org.mockito.Mockito"),
                "Mockito 不在测试运行时类路径上");
    }

    @Test
    void testSourcesAreNotIgnoredByGit() throws IOException {
        Path gitignore = ROOT.resolve(".gitignore");
        assertTrue(Files.isRegularFile(gitignore), "找不到 .gitignore");
        for (String line : Files.readAllLines(gitignore, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            assertFalse(trimmed.equals("src/test") || trimmed.equals("src/test/"),
                    ".gitignore 忽略了测试源码目录，而 Gradle 仍会编译它——"
                            + "新克隆与 CI 拿到的测试集会与本机不同，且 git status 从不提醒");
        }
    }

    private static boolean tryLoad(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
