package com.github.tartaricacid.touhoulittlemaid.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
     * 表现为地面整片发白。</p>
     *
     * <p><b>本表有意为空</b>：行为基准与 {@code origin/1.21.1} 上真实调用点都是 <b>0</b>
     * （实查；基准里唯一的字面命中在一段 javadoc 里）。追踪标记画在 HUD 层，
     * 河童罗盘与女仆范围可视化的文字则应当被墙体遮挡。</p>
     */
    private static final Map<String, String> ALWAYS_ON_TOP_ALLOWED = Map.of();

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

    /**
     * 坐垫的缓存对象只负责提供稳定的 model identity，不能持有一次性的实体渲染状态。
     * Gecko 几何提交后会关闭 {@code GeckoRenderData}；若把包含它的状态按 modelId 缓存，
     * 第一帧之后同一坐垫只会反复拿到 CLOSED 数据，表现为 Alex 坐垫完全透明。
     */
    @Test
    void cachedChairIdentityDoesNotOwnOneShotEntityRenderState() throws IOException {
        Path stateFile = MAIN_JAVA.resolve(
                "com/github/tartaricacid/touhoulittlemaid/client/renderer/item/state/ChairRenderRenderState.java");
        String stateSource = stripComments(Files.readString(stateFile, StandardCharsets.UTF_8));
        assertFalse(stateSource.contains("EntityRenderState"),
                "按 modelId 缓存的坐垫 identity 不能持有一次性 EntityRenderState");

        Path rendererFile = MAIN_JAVA.resolve(
                "com/github/tartaricacid/touhoulittlemaid/client/renderer/item/ChairItemRenderer.java");
        String rendererSource = stripComments(Files.readString(rendererFile, StandardCharsets.UTF_8));
        String submit = methodBody(rendererSource, "submit");
        assertTrue(submit != null && submit.contains("extractEntity(") && submit.contains("state.modelId"),
                "坐垫必须在每次实际 submit 时按缓存的 modelId 抽取新实体渲染状态");
    }

    /**
     * 纹理注册必须在渲染线程上发生，或显式分派回去。
     *
     * <p>{@code TextureManager.registerAndLoad} 会当场 load 并上传 GPU
     * （26.1.2 字节码：{@code apply → doLoad → RenderSystem.getDevice()}），而 GL 上下文是线程绑定的。
     * 游戏内下载走的是下载线程：{@code ClientPackDownloadManager} 的
     * {@code CompletableFuture.thenRun} 在完成线程上直接调 {@code reloadPack}，
     * 一路到纹理注册，**不经那条已经分派过的 asyncReload**。</p>
     *
     * <p>⚠️ <b>这一条是补回行为基准早就修过、而本分支漏搬的一处修复</b>
     * （1.21.11 的 C07，用户实机撞到过「下载按钮卡在 DOWNLOADING、模型列表要重启才出现」）。
     * 那个文件在本树与代码宿主逐字相同——**「与宿主一致」正是它可疑的地方**。</p>
     *
     * <p>⚠️ <b>26.1.2 与 1.21.11 在此有真实差异，不要照抄旧结论</b>：本版上传路径里
     * <b>没有</b> {@code assertOnRenderThread}（javap 实查 {@code GlDevice} / {@code GlTexture}
     * 均为 0 处），所以它不会像 1.21.11 那样当场断言失败。**失败形态更安静，因此更需要判据。**</p>
     *
     * <p><b>判据强度说明</b>：按文件粒度判（含 {@code registerAndLoad} 的文件必须同时含
     * {@code execute(} 或 {@code isSameThread(}），不做跨方法的线程可达性分析——那需要调用图。
     * 粗，但足以照出本次这个缺陷：出问题的文件两者皆无。</p>
     */
    @Test
    void textureRegistrationEitherRunsOnTheRenderThreadOrDispatchesToIt() throws IOException {
        // 明确只在渲染线程上被调用的位置：注明理由，别静默放行
        Set<String> renderThreadOnly = Set.of(
                // 缓存屏的逐模型回调本身就跑在渲染线程（RenderSystem.executePendingTasks）
                "CacheScreen.java");

        List<String> offenders = new ArrayList<>();
        int sites = 0;
        try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file, StandardCharsets.UTF_8);
                // 剥注释：只在 javadoc 里提到 registerAndLoad 的文件不算调用点
                String active = source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
                if (!active.contains("registerAndLoad(")) {
                    continue;
                }
                sites++;
                String name = file.getFileName().toString();
                if (renderThreadOnly.contains(name)) {
                    continue;
                }
                if (!active.contains("execute(") && !active.contains("isSameThread(")) {
                    offenders.add(PROJECT_ROOT.relativize(file).toString());
                }
            }
        }

        // 活性断言：与结论正交。识别依据一变就静默零覆盖，而零覆盖恒绿。
        assertTrue(sites >= 3,
                "只认出 " + sites + " 处 registerAndLoad 调用点，识别依据可能已失效");
        assertTrue(offenders.isEmpty(),
                "这些文件注册纹理却既不分派回渲染线程、也不判断当前线程："
                        + offenders + "（GL 上下文线程绑定；本版不会断言，只会安静地坏）");
    }

    /**
     * 传给文字/图形 API 的颜色字面量必须带 alpha 通道。
     *
     * <p>1.21.11 起 {@code Font} 删掉了「alpha 为 0 就补成不透明」的兜底
     * （26.1.2 字节码实查同样没有），于是 {@code 0xF3EFE0} 这类 6 位十六进制在本版
     * 含义是<b>全透明</b>——事件在跑、组件读到、几何算对、绘制执行、日志干净，屏幕上什么都没有。</p>
     *
     * <p>⚠️ 危险之处在于<b>这些常量多半是从上游 1.21.1 原样继承来的</b>，在那边显示正常。
     * 本仓库已因此栽过两次：追踪标记全透明，以及资源下载屏搜索框里打的字一个都看不见
     * （后者由行为基准 2026-07-20 修过，本分支漏搬，2026-08-20 补回）。</p>
     *
     * <p><b>判据</b>：{@code setTextColor} / {@code setFGColor} 一类的颜色入参若写成十六进制
     * 字面量，必须是 8 位（含 alpha）。变量与表达式不在此判据范围内——那要常量传播。</p>
     */
    @Test
    void colorLiteralsPassedToTextApisCarryAnAlphaChannel() throws IOException {
        Pattern colorCall = Pattern.compile(
                "\\b(setTextColor|setTextColorUneditable|setFGColor)\\s*\\(\\s*0x([0-9A-Fa-f]+)\\s*\\)");

        List<String> offenders = new ArrayList<>();
        int sites = 0;
        try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file, StandardCharsets.UTF_8);
                String active = source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
                Matcher m = colorCall.matcher(active);
                while (m.find()) {
                    sites++;
                    if (m.group(2).length() != 8) {
                        offenders.add(PROJECT_ROOT.relativize(file) + " -> " + m.group());
                    }
                }
            }
        }

        // 活性断言：与结论正交——识别依据一变就静默零覆盖，而零覆盖恒绿
        assertTrue(sites >= 2, "只认出 " + sites + " 处颜色字面量调用，识别依据可能已失效");
        assertTrue(offenders.isEmpty(),
                "这些颜色字面量没有 alpha 通道，在本版会画成全透明（看不见，且不报任何错）："
                        + offenders);
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
