package com.github.tartaricacid.touhoulittlemaid;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code fabric.mod.json} 里每个 entrypoint 指向的类都必须真实存在。
 *
 * <p>与 {@code MixinRegistrationInvariantTest} 是一对：那边看住「源码有、登记没有」，
 * 这边看住「登记有、源码没有」（类改名/挪包后 entrypoint 忘改）。失效形态分两档：
 * {@code main}/{@code client} 这类必载入口会崩启动（有闸看着）；
 * {@code rei_client}/{@code jei_mod_plugin}/{@code fabric-gametest} 这类**由第三方拉起的入口
 * 是纯静默的**——REI 不在场时坏类名永远不暴露，在场时才崩，正是纸面接口的形态。</p>
 */
class EntrypointRegistrationInvariantTest {
    /** 测试的 workingDir 是 build/test-working，回两级才是项目根。 */
    private static final Path ROOT = Path.of("..", "..");
    private static final Path FABRIC_MOD_JSON = ROOT.resolve("src/main/resources/fabric.mod.json");
    private static final List<Path> SOURCE_ROOTS = List.of(
            ROOT.resolve("src/main/java"),
            ROOT.resolve("src/client/java"));

    @Test
    void everyEntrypointClassExistsInSources() throws IOException {
        JsonObject entrypoints;
        try (Reader reader = Files.newBufferedReader(FABRIC_MOD_JSON, StandardCharsets.UTF_8)) {
            entrypoints = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("entrypoints");
        }

        List<String> missing = new ArrayList<>();
        int checked = 0;
        for (Map.Entry<String, JsonElement> group : entrypoints.entrySet()) {
            for (JsonElement entry : group.getValue().getAsJsonArray()) {
                // entrypoint 两种写法：裸字符串 或 {"adapter":…, "value":…}
                String value = entry.isJsonObject()
                        ? entry.getAsJsonObject().get("value").getAsString()
                        : entry.getAsString();
                // 方法引用形式 "a.b.Class::method" 只取类那半；内部类取外层类
                String className = value.split("::")[0].split("\\$")[0];
                checked++;
                if (!sourceExists(className)) {
                    missing.add(group.getKey() + " -> " + value);
                }
            }
        }

        // 活性下限：本仓库 entrypoint 常年在 10 个以上，认不出这么多就是解析坏了
        assertTrue(checked >= 10, "只解析出 " + checked + " 个 entrypoint，解析多半是坏的");
        assertEquals(List.of(), missing,
                "这些 entrypoint 指向不存在的类：必载入口会崩启动，第三方拉起的入口（REI/JEI/GameTest）"
                        + "在对方不在场时**纯静默**，在场时崩——正是纸面接口形态");
    }

    private static boolean sourceExists(String className) {
        String relative = className.replace('.', '/') + ".java";
        for (Path root : SOURCE_ROOTS) {
            if (Files.isRegularFile(root.resolve(relative))) {
                return true;
            }
        }
        return false;
    }
}
