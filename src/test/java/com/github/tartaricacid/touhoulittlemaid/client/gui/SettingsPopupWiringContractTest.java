package com.github.tartaricacid.touhoulittlemaid.client.gui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 弹出列表的两条接线契约。
 *
 * <p>弹出层是<b>覆盖层</b>：它必须画在子类自绘物（滚动条、提示字、保存绿字）<b>之后</b>，
 * 否则会被压在底下——那时它仍然响应点击，于是表现为「点了没反应，其实点中了看不见的列表」。
 * 而画它的调用点在每个子类各写一次，漏一处就是漏一屏。</p>
 *
 * <p>第二条：{@code init()} 重建控件时必须关掉弹层。弹层里记着的坐标与回调都指向旧控件，
 * 留着它等于把一组过期回调挂在屏上。</p>
 */
class SettingsPopupWiringContractTest {
    private static final Path ROOT = Path.of("..", "..");
    private static final Path AI_GUI = ROOT.resolve(Path.of("src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid", "client", "gui", "entity", "maid", "ai"));
    private static final Path HUB = AI_GUI.resolve(Path.of("settings", "AIChatSettingsHubScreen.java"));
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern LINE_COMMENT = Pattern.compile("//[^\\n]*");

    /** LLM / TTS / STTConfig / Usage 四页都自绘了东西，四页都得调 */
    private static final int MIN_OVERLAY_CALLERS = 4;

    private static String activeSource(Path file) throws IOException {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        source = BLOCK_COMMENT.matcher(source).replaceAll(" ");
        return LINE_COMMENT.matcher(source).replaceAll(" ");
    }

    /** 截取方法体：从签名处的 {@code {} 起按大括号配对取到匹配的 {@code }} */
    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        if (start < 0) {
            return "";
        }
        int brace = source.indexOf('{', start);
        if (brace < 0) {
            return "";
        }
        int depth = 0;
        for (int i = brace; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(brace, i + 1);
                }
            }
        }
        return "";
    }

    @Test
    void everyScreenThatDrawsItsOwnOverlayRendersThePopupLast() throws IOException {
        List<String> callers = new ArrayList<>();
        List<String> offenders = new ArrayList<>();
        int scanned = 0;

        try (Stream<Path> files = Files.list(AI_GUI.resolve("settings"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).sorted().toList()) {
                scanned++;
                String name = file.getFileName().toString();
                if (name.equals("AIChatSettingsHubScreen.java")) {
                    continue;
                }
                String source = activeSource(file);
                String body = methodBody(source, "public void extractRenderState(");
                if (body.isEmpty()) {
                    continue;
                }
                callers.add(name);
                int overlay = body.indexOf("renderHubOverlays(");
                if (overlay < 0) {
                    offenders.add(name + "（根本没画弹层）");
                    continue;
                }
                // 必须是最后一个绘制动作：其后不许再有别的自绘调用
                String tail = body.substring(overlay + "renderHubOverlays(".length());
                boolean drawsAfter = tail.contains("this.render") || tail.contains("graphics.text")
                        || tail.contains("graphics.fill") || tail.contains("super.extractRenderState");
                if (drawsAfter) {
                    offenders.add(name + "（弹层之后还在画别的，会被压住）");
                }
            }
        }

        assertTrue(scanned > 0, "settings 目录一个文件都没扫到，这条断言已失去看守对象");
        assertTrue(callers.size() >= MIN_OVERLAY_CALLERS,
                "只认出 %d 屏覆写了 extractRenderState（期望 ≥ %d）——识别依据多半已失效。实见：%s"
                        .formatted(callers.size(), MIN_OVERLAY_CALLERS, callers));
        assertTrue(offenders.isEmpty(),
                "弹出层没有画在最后，会被子类自绘物压住（点得中但看不见）：" + offenders);
    }

    @Test
    void rebuildingWidgetsClosesThePopup() throws IOException {
        String initBody = methodBody(activeSource(HUB), "protected void init()");
        assertTrue(!initBody.isEmpty(), "找不到 hub 的 init() 方法体，这条断言已失去看守对象");
        assertTrue(initBody.contains("closeHubPopup()"),
                "init() 没有关掉弹出列表：重建后弹层里记的坐标与回调都指向已被丢弃的控件");
    }

    /** 弹层打开时滚轮归它，否则底下的站点列表会跟着一起滚 */
    @Test
    void popupTakesTheScrollWheel() throws IOException {
        String body = methodBody(activeSource(HUB), "protected boolean handleListScroll(");
        assertTrue(!body.isEmpty(), "找不到 handleListScroll 的方法体，这条断言已失去看守对象");
        int guard = body.indexOf("popupEntries != null");
        int listCheck = body.indexOf("this.listArea == null");
        assertTrue(guard >= 0, "handleListScroll 没有为弹层让出滚轮");
        assertTrue(listCheck > guard,
                "弹层的滚轮让位判据必须排在列表判据之前，否则列表照样会跟着滚");
    }
}
