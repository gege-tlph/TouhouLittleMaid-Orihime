package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.editor;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 站点编辑器布局的机械不变量。
 *
 * <p>由来：往基准那排「添加 / 保存 / 返回」里加「检查配置」时取了 {@code BASE_WIDTH - 302}（= 98），
 * 而「添加」占到 108，两颗按钮**重叠 10 像素**；同期加的「清除」压在密钥输入框上，
 * 重叠带里的点击被输入框先吃掉。两处都是纯算术错误，编译、启动、单测全绿，只有肉眼能发现。</p>
 *
 * <p><b>能钉死的就不该靠眼睛看。</b>Screen 要真实客户端才能构造，所以这里不测渲染结果，
 * 只测两件机器判得了的事：① {@link SiteEditorLayout} 里的矩形互不重叠；
 * ② 三个编辑屏确实在消费这些常量，而不是各写字面值。</p>
 *
 * <p><b>覆盖边界（如实写明）</b>：②只认「屏里出现了对应常量名」，因此新加第五颗按钮若直接写字面值，
 * 本测试抓不到——那种情况仍须把坐标登记进 {@code SiteEditorLayout}。字体宽度、纵向排布与
 * 实际观感一律不在覆盖内，仍须人工验收。</p>
 */
class SiteEditorLayoutTest {
    private static final Path ROOT = Path.of("..", "..");
    private static final Path EDITOR_DIR = ROOT.resolve(Path.of("src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid", "client", "gui", "entity", "maid", "ai", "editor"));
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    /** 一个控件的横向占位 */
    private record Span(String name, int x, int width) {
        int right() {
            return this.x + this.width;
        }
    }

    private static List<Span> bottomRow() {
        return List.of(
                new Span("添加", SiteEditorLayout.ADD_MODEL_X, SiteEditorLayout.ADD_MODEL_WIDTH),
                new Span("检查配置", SiteEditorLayout.CHECK_CONFIG_X, SiteEditorLayout.CHECK_CONFIG_WIDTH),
                new Span("保存", SiteEditorLayout.SAVE_X, SiteEditorLayout.SAVE_WIDTH),
                new Span("返回", SiteEditorLayout.BACK_X, SiteEditorLayout.BACK_WIDTH));
    }

    /**
     * <b>本类的主菜</b>：底部一排里任意两颗按钮都不许重叠，且必须留出 {@link SiteEditorLayout#GAP} 间隙。
     *
     * <p>这条就是当初漏掉的那个断言。重叠的直接后果是两颗按钮在同一片像素上互相压字，
     * 而先注册的那颗还会吃掉重叠带里的点击。</p>
     */
    @Test
    void bottomRowButtonsNeverOverlap() {
        List<Span> row = bottomRow();
        List<String> problems = new ArrayList<>();
        for (int i = 0; i < row.size(); i++) {
            for (int j = i + 1; j < row.size(); j++) {
                Span a = row.get(i);
                Span b = row.get(j);
                Span left = a.x() <= b.x() ? a : b;
                Span right = a.x() <= b.x() ? b : a;
                int gap = right.x() - left.right();
                if (gap < SiteEditorLayout.GAP) {
                    problems.add("%s(%d..%d) 与 %s(%d..%d) 间隙 %d < %d".formatted(
                            left.name(), left.x(), left.right(),
                            right.name(), right.x(), right.right(), gap, SiteEditorLayout.GAP));
                }
            }
        }
        assertTrue(problems.isEmpty(), "底部按钮重叠或过近：" + String.join("；", problems));
    }

    /** 整排按钮必须落在内容区内，不许越出面板留白 */
    @Test
    void bottomRowStaysInsideTheContentArea() {
        int contentRight = SiteEditorLayout.PANEL_WIDTH - SiteEditorLayout.MARGIN;
        for (Span span : bottomRow()) {
            assertTrue(span.x() >= SiteEditorLayout.MARGIN,
                    "%s 越出左留白：x=%d".formatted(span.name(), span.x()));
            assertTrue(span.right() <= contentRight,
                    "%s 越出右留白：right=%d > %d".formatted(span.name(), span.right(), contentRight));
        }
    }

    /**
     * 显示「清除」时，密钥输入框必须让出它的位置。
     *
     * <p>输入框的可见底衬正好铺满 {@code [MARGIN, MARGIN + width]}（{@code renderInputField}
     * 按 {@code getX()-6} 与 {@code innerWidth+12} 绘制），所以这里用的就是它的真实右边界。</p>
     */
    @Test
    void theSecretFieldYieldsRoomForTheClearButton() {
        int fieldRight = SiteEditorLayout.MARGIN + SiteEditorLayout.SECRET_WIDTH_WITH_CLEAR;
        int gap = SiteEditorLayout.SECRET_CLEAR_X - fieldRight;
        assertTrue(gap >= SiteEditorLayout.GAP,
                "密钥框右边界 %d 与「清除」左边界 %d 间隙 %d < %d：重叠带里的点击会被输入框吃掉".formatted(
                        fieldRight, SiteEditorLayout.SECRET_CLEAR_X, gap, SiteEditorLayout.GAP));
        assertTrue(SiteEditorLayout.SECRET_CLEAR_X + SiteEditorLayout.SECRET_CLEAR_WIDTH
                        <= SiteEditorLayout.PANEL_WIDTH - SiteEditorLayout.MARGIN,
                "「清除」越出右留白");
    }

    /**
     * 三个编辑屏必须消费共享常量。
     *
     */
    @Test
    void everyEditorScreenConsumesTheSharedLayout() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (String screen : List.of("LLMSiteEditorScreen.java", "TTSSiteEditorScreen.java", "STTSiteEditorScreen.java")) {
            String source = activeSource(EDITOR_DIR.resolve(screen));
            if (!source.contains("SiteEditorLayout.SAVE_X") || !source.contains("SiteEditorLayout.BACK_X")) {
                offenders.add(screen);
            }
        }
        assertTrue(offenders.isEmpty(),
                "这些编辑屏没有使用 SiteEditorLayout 的底部按钮坐标，会重新分叉：" + String.join(", ", offenders));
    }

    /** 带「检查配置」的两屏必须用共享坐标，那颗按钮就是当初重叠的那一颗 */
    @Test
    void screensWithTheCheckButtonUseTheSharedX() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (String screen : List.of("LLMSiteEditorScreen.java", "TTSSiteEditorScreen.java")) {
            String source = activeSource(EDITOR_DIR.resolve(screen));
            if (!source.contains("CheckSiteConfigPackage") || !source.contains("SiteEditorLayout.CHECK_CONFIG_X")) {
                offenders.add(screen);
            }
        }
        assertTrue(offenders.isEmpty(),
                "这些屏自行给「检查配置」定位，会重演与「添加」重叠：" + String.join(", ", offenders));
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
