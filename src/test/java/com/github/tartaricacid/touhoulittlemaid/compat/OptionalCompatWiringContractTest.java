package com.github.tartaricacid.touhoulittlemaid.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 可选第三方兼容的接线契约。
 *
 * <p><b>为什么不是行为判据</b>：这些兼容按定义只在装了对方模组时才跑，而对方是
 * {@code compileOnly}、不在 {@code runGametest} 的运行时里。行为基准那边有一条
 * {@code isModLoaded} 守着的 GameTest——模组不在时它整条不执行、照样绿，
 * 正是本仓库反复栽过的「零覆盖恒绿」形态，故本树不搬那条，改成始终有效的接线断言。
 * 真正「装上模组后表现对不对」只有实机能验。</p>
 */
class OptionalCompatWiringContractTest {
    /** 测试的 workingDir 是 build/test-working，回两级才是项目根。 */
    private static final Path PROJECT_ROOT = Path.of("..", "..");
    private static final Path MAIN_JAVA = PROJECT_ROOT.resolve("src/main/java");
    private static final Path TLM = MAIN_JAVA.resolve("com/github/tartaricacid/touhoulittlemaid");
    private static final Path FABRIC_MOD_JSON = PROJECT_ROOT.resolve("src/main/resources/fabric.mod.json");
    private static final Path TAVERN_COMPAT = TLM.resolve("compat/kaleidoscopetavern");

    /** 第三方模组的包前缀 → 只允许出现在哪个兼容包里。 */
    private static final List<String[]> FOREIGN_PACKAGE_CONTAINMENT = List.of(
            new String[]{"com.github.ysbbbbbb.kaleidoscopetavern", "compat/kaleidoscopetavern"},
            new String[]{"com.github.ysbbbbbb.kaleidoscopecookery", "compat/kaleidoscope"});

    /**
     * {@code little_maid_extension} 是**第十处静默注册面**：兼容类写好、编译通过、打包正常，
     * 不登记就永远不会被 {@code AnnotatedInstanceUtil.getModExtensions()} 发现，功能从不执行。
     */
    @Test
    void tavernCompatIsRegisteredAsALittleMaidExtension() throws IOException {
        String json = Files.readString(FABRIC_MOD_JSON, StandardCharsets.UTF_8);
        assertTrue(json.contains("\"little_maid_extension\""),
                "fabric.mod.json 缺少 little_maid_extension entrypoint——兼容类不会被发现");
        assertTrue(json.contains("compat.kaleidoscopetavern.TavernCompat"),
                "TavernCompat 没有登记进 little_maid_extension");
    }

    /**
     * 兼容的每个扩展点实现都必须被 {@code isModLoaded} 守着。
     *
     * <p>没有这道守卫，未装该模组的玩家一样会拿到那些行为对象，而它们的方法体里引用着
     * 不存在的类——触发时才 {@code NoClassDefFoundError}，且触发点在 AI tick 上。</p>
     */
    @Test
    void everyTavernExtensionPointIsGuardedByIsModLoaded() throws IOException {
        Path compatEntry = TAVERN_COMPAT.resolve("TavernCompat.java");
        String source = stripComments(Files.readString(compatEntry, StandardCharsets.UTF_8));
        int overrides = 0;
        List<String> unguarded = new ArrayList<>();
        for (String method : source.split("@Override")) {
            if (!method.contains("public void ")) {
                continue;
            }
            overrides++;
            if (!method.contains("isModLoaded(MOD_ID)")) {
                unguarded.add(method.strip().split("\\(")[0]);
            }
        }
        assertTrue(overrides >= 2,
                "只认出 " + overrides + " 个扩展点实现，识别依据可能已失效");
        assertEquals(List.of(), unguarded, "这些扩展点实现没有 isModLoaded 守卫");
    }

    /**
     * 两个为兼容而加的核心钩子必须真的**有人消费**——「加了扩展点」与「扩展点被接进主流程」
     * 是两件事，后者才是行为发生的地方（本仓库多次栽在「init 被调 ≠ init 有内容」上）。
     */
    @Test
    void compatHooksAreConsumedByCoreLogic() throws IOException {
        assertConsumed("ExtraMaidBrainManager.canClimbBlock(",
                TLM.resolve("entity/ai/navigation/MaidNodeEvaluator.java"),
                "攀爬排除钩子没有被寻路消费，附属再怎么否决都不会生效");
        assertConsumed("MaidMealManager.isWorkMealExcluded(",
                TLM.resolve("entity/task/meal/DefaultMaidWorkMeal.java"),
                "工作餐排除钩子没有被默认工作餐消费，登记的排除项永远不起作用");
    }

    /** 第三方类型只许出现在它自己的兼容包里——否则没装那个模组的玩家会在核心路径上崩。 */
    @Test
    void foreignTypesStayInsideTheirCompatPackage() throws IOException {
        List<String> leaks = new ArrayList<>();
        int inspected = 0;
        try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                inspected++;
                String source = stripComments(Files.readString(file, StandardCharsets.UTF_8));
                String normalized = file.toString().replace('\\', '/');
                for (String[] rule : FOREIGN_PACKAGE_CONTAINMENT) {
                    if (source.contains(rule[0]) && !normalized.contains(rule[1])) {
                        leaks.add(PROJECT_ROOT.relativize(file) + " 引用了 " + rule[0]);
                    }
                }
            }
        }
        assertTrue(inspected >= 400, "只走过 " + inspected + " 个源文件，扫描范围可能已失效");
        assertEquals(List.of(), leaks, "第三方类型泄漏到了兼容包之外");
    }

    private static void assertConsumed(String call, Path consumer, String message) throws IOException {
        String source = stripComments(Files.readString(consumer, StandardCharsets.UTF_8));
        assertTrue(source.contains(call), message + "（期望在 " + consumer.getFileName() + " 里找到 " + call + "）");
    }

    /** 去掉块注释与行注释——否则只在 javadoc 里提到的接线会被判为「存在」。 */
    private static String stripComments(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
    }
}
