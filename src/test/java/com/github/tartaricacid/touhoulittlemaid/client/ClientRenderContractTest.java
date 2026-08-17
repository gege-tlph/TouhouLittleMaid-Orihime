package com.github.tartaricacid.touhoulittlemaid.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 渲染层的两条契约。**这一层没有行为判据**：GameTest 是纯服务端，客户端渲染在那里根本不加载，
 * 所以下面全是接线判据（🟡），真正的「看起来对不对」只有实机能验。
 * 明写这一点是为了不让「测试通过」被读成「渲染正确」。
 */
class ClientRenderContractTest {
    /** 测试的 workingDir 是 build/test-working，回两级才是项目根。 */
    private static final Path PROJECT_ROOT = Path.of("..", "..");
    private static final Path MAIN_JAVA = PROJECT_ROOT.resolve("src/main/java");
    private static final Path CLIENT_SETUP = MAIN_JAVA.resolve(
            "com/github/tartaricacid/touhoulittlemaid/client/init/ClientSetupEvent.java");

    private static final Pattern SPECIAL_MODEL_RENDERER =
            Pattern.compile("implements\\s+SpecialModelRenderer\\s*<\\s*([A-Za-z0-9_]+)\\s*>");

    /**
     * 允许在 {@code extractArgument} 里每次新建状态的渲染器，**必须写清理由**。
     *
     * <p>目前为空：本仓库两个实现都必须记忆化。若将来有渲染器**故意**逐帧换 identity
     * （例如靠它驱动图集动画的占位物图标），登记到这里并写明它为什么不怕图标缓存失效。</p>
     */
    private static final Map<String, String> FRESH_STATE_ALLOWED = Map.of();

    /**
     * 允许使用世界内「置顶」绘制的文件，**必须写清理由**。
     *
     * <p>「置顶」在 26.1.2 的实现是 {@code LevelRenderer.addLateDebugPass} 里对主渲染目标
     * {@code clearDepthTexture}（反编译源实查）。开光影时它会把光影包的深度附件一并抹掉，
     * 表现为地面整片发白。下面两处是**已知未修**，与行为基准同状态：它们只在玩家主动拿着
     * 河童罗盘 / 打开女仆范围可视化时才画，且行为基准当年也只把追踪标记搬去了 HUD。</p>
     *
     * <p>⚠️ <b>追踪标记不得回到这张表里</b>——它是常驻可见的（手持仆从铃/狐狸卷轴即触发），
     * 正是玩家报上来的那个症状。它现在画在 HUD 层，见 {@code TrackerMarkerOverlay}。</p>
     */
    private static final Map<String, String> ALWAYS_ON_TOP_ALLOWED = Map.of(
            "CompassRenderEvent.java", "河童罗盘范围可视化：仅手持罗盘时绘制，与行为基准同为已知未修",
            "MaidAreaRenderEvent.java", "女仆范围可视化：仅调试/查看范围时绘制，同上");

    /**
     * {@code SpecialModelWrapper} 把 {@code extractArgument} 的返回值追加进 GUI 图标缓存的
     * model identity（26.1.2 反编译源实查）。状态类没有 {@code equals}，所以每次调用新建实例
     * 就等于每帧换 identity，缓存永远失效——创造栏/JEI 满屏手办或坐垫时每帧全量重抽取重绘。
     */
    @Test
    void specialModelStatesAreMemoizedSoTheGuiIconCacheCanHit() throws IOException {
        List<String> offenders = new ArrayList<>();
        int renderers = 0;
        try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = stripComments(Files.readString(file, StandardCharsets.UTF_8));
                Matcher matcher = SPECIAL_MODEL_RENDERER.matcher(source);
                if (!matcher.find()) {
                    continue;
                }
                renderers++;
                String stateType = matcher.group(1);
                String fileName = file.getFileName().toString();
                String body = methodBody(source, "extractArgument");
                if (body == null) {
                    offenders.add(fileName + "：找不到 extractArgument 方法体，扫描依据可能已失效");
                    continue;
                }
                if (body.contains("new " + stateType + "(") && !FRESH_STATE_ALLOWED.containsKey(fileName)) {
                    offenders.add(fileName + "：extractArgument 里直接 new " + stateType
                                  + "，图标缓存会每帧失效；要么按数据记忆化，要么登记进 FRESH_STATE_ALLOWED 并写明理由");
                }
            }
        }
        // 活性判据与结论正交：本条是禁止型断言，「没有违规」与「一个渲染器都没认出来」结果相同
        assertTrue(renderers >= 2,
                "只认出 " + renderers + " 个 SpecialModelRenderer 实现，识别依据可能已失效");
        assertEquals(List.of(), offenders, "特殊模型渲染器的状态必须按数据记忆化");
    }

    /** 世界内「置顶」会清主渲染目标的深度贴图，光影下地面发白。只允许名单里那几处。 */
    @Test
    void worldSpaceAlwaysOnTopIsLimitedToTheNamedAllowlist() throws IOException {
        List<String> offenders = new ArrayList<>();
        int inspected = 0;
        try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                inspected++;
                String source = stripComments(Files.readString(file, StandardCharsets.UTF_8));
                if (!source.contains(".setAlwaysOnTop(")) {
                    continue;
                }
                String fileName = file.getFileName().toString();
                if (!ALWAYS_ON_TOP_ALLOWED.containsKey(fileName)) {
                    offenders.add(PROJECT_ROOT.relativize(file).toString());
                }
            }
        }
        assertTrue(inspected >= 400, "只走过 " + inspected + " 个源文件，扫描范围可能已失效");
        assertEquals(List.of(), offenders,
                "新增的世界内置顶绘制会在光影下把地面画白；确实需要就登记进 ALWAYS_ON_TOP_ALLOWED 并写明它何时可见");
    }

    /** 追踪标记必须挂在 HUD 层，且不得再有世界内的绘制入口。 */
    @Test
    void trackerMarkerIsAttachedToTheHud() throws IOException {
        String setup = stripComments(Files.readString(CLIENT_SETUP, StandardCharsets.UTF_8));
        assertTrue(setup.contains("HudElementRegistry") && setup.contains("TrackerMarkerOverlay.INSTANCE"),
                "TrackerMarkerOverlay 没有登记进 HUD——HudElement 不登记就是纯静默，永远不画");

        List<String> worldSpaceEntries = new ArrayList<>();
        try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = stripComments(Files.readString(file, StandardCharsets.UTF_8));
                if (source.contains("ScrollRenderEvent")) {
                    worldSpaceEntries.add(PROJECT_ROOT.relativize(file).toString());
                }
            }
        }
        assertEquals(List.of(), worldSpaceEntries,
                "追踪标记的世界内绘制入口已被 HUD 版取代，不该再有引用");
    }

    /** 去掉块注释与行注释——否则只在 javadoc 里提到的写法会被判为「存在」。 */
    private static String stripComments(String source) {
        String withoutBlocks = source.replaceAll("(?s)/\\*.*?\\*/", "");
        return withoutBlocks.replaceAll("(?m)//.*$", "");
    }

    /** 取方法体（大括号配对截取）。断言方法内部的写法时必须先缩到那个方法，否则扫的是整份文件。 */
    private static String methodBody(String source, String methodName) {
        int signature = source.indexOf(" " + methodName + "(");
        if (signature < 0) {
            return null;
        }
        int open = source.indexOf('{', signature);
        if (open < 0) {
            return null;
        }
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(open, i + 1);
                }
            }
        }
        return null;
    }
}
