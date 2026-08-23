package com.github.tartaricacid.touhoulittlemaid.client.resource.bedrock;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code InternalBedrockModelRegistry} 里注册的每个模型常量，对应的 json 文件必须真在资源树里。
 *
 * <p>失效形态是**运行期静默 + 使用时崩**：加载器对缺失文件只打一行
 * {@code Not found model file} ERROR 继续跑，直到有人用那个模型（{@code getEntityModel}
 * 返回 null）才 NPE——2026-08-14 实机崩过一次（油库里两常量补了、4 个资源文件没跟上，
 * 岩浆怪替换开关一开就崩渲染线程）。与 mixin/entrypoint 两道登记闸同族：
 * 「注册面」与「实体面」必须机械对账，缺哪半都是纸面接口。</p>
 *
 * <p>路径规则照 {@code InternalBedrockModelSet.prepare}：
 * {@code assets/<namespace>/models/<path>.json}。</p>
 */
class BedrockModelResourceInvariantTest {
    /** 测试的 workingDir 是 build/test-working，回两级才是项目根。 */
    private static final Path RESOURCES = Path.of("..", "..").resolve("src/main/resources");

    @Test
    void everyRegisteredBedrockModelHasItsJsonFile() {
        Set<Identifier> registered = new LinkedHashSet<>();
        registered.addAll(InternalBedrockModelRegistry.MODELS.keySet());
        registered.addAll(InternalBedrockModelRegistry.ENTITY_MODELS.keySet());

        // 活性下限：两张注册表合计常年 30+，认不出这么多就是类初始化或反射面坏了
        assertTrue(registered.size() >= 30,
                "只认出 " + registered.size() + " 个注册模型，注册表读取可能已失效");

        List<String> missing = new ArrayList<>();
        for (Identifier location : registered) {
            Path file = RESOURCES.resolve("assets").resolve(location.getNamespace())
                    .resolve("models").resolve(location.getPath() + ".json");
            if (!Files.isRegularFile(file)) {
                missing.add(location + " -> " + file.normalize());
            }
        }
        assertEquals(List.of(), missing,
                "这些注册了常量的 bedrock 模型没有对应 json 文件：加载器只报一行 ERROR 继续跑，"
                        + "用到那一刻才 NPE 崩渲染线程（2026-08-14 油库里实机崩过）");
    }
}
