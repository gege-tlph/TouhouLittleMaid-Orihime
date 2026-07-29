package com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.gecko;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 挂件 layer（手持物 / 头顶方块 / 背包 / 背部物品 / 背旗）对模型系统保持中立。
 *
 * <p>女仆有两套模型系统：TLM 自带 gecko 与 YSM。两者都会跑这同一批 layer，但提供定位信息的形态不同。
 * 移植到 1.21.11 时 layer 被改成直接持有 gecko 的 {@code GeoModelState}，YSM 路径上那个状态不存在，
 * 于是<b>整整一个版本里 YSM 模型的女仆不渲染任何挂件</b>。这条断言就是防止再漂回去。</p>
 */
class GeckoMaidLayerContractTest {
    private static final Path ROOT = Path.of("..", "..");
    private static final Path LAYER_DIR = ROOT.resolve(Path.of("src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid", "client", "renderer", "entity", "gecko", "layer"));
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    /**
     * 定位组只许问 {@code data.locators()}，不许直接摸 {@code data.modelState}。
     *
     * <p>{@code modelState} 是 gecko 那套模型独有的；直接摸它，YSM 路径上就是 NPE
     * （2026-07-28 实测崩在 {@code GeckoLayerMaidBackpack:24}）。</p>
     */
    @Test
    void layersAskTheLocatorSourceNotTheGeckoModelState() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path file : layerFiles()) {
            if (activeSource(file).contains("data.modelState")) {
                offenders.add(file.getFileName().toString());
            }
        }
        assertTrue(offenders.isEmpty(),
                "这些 layer 直接读了 gecko 专有的 modelState，YSM 模型的女仆跑到这里会 NPE，"
                        + "改用 data.locators()：" + String.join(", ", offenders));
    }

    private static List<Path> layerFiles() throws IOException {
        try (Stream<Path> files = Files.list(LAYER_DIR)) {
            List<Path> layers = files.filter(p -> p.getFileName().toString().startsWith("GeckoLayerMaid"))
                    .filter(p -> p.toString().endsWith(".java")).toList();
            assertTrue(layers.size() >= 5,
                    "只找到 " + layers.size() + " 个挂件 layer，少于已知的 5 个——目录改名了就同步更新本测试");
            return layers;
        }
    }

    private static String activeSource(Path path) throws IOException {
        StringBuilder active = new StringBuilder();
        for (String line : Files.readAllLines(path)) {
            if (line.trim().startsWith("//")) {
                continue;
            }
            active.append(line).append('\n');
        }
        return BLOCK_COMMENT.matcher(active.toString()).replaceAll(" ");
    }
}
