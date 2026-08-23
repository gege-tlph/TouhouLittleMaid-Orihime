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
 * 「检查配置」的回执必须回到发起它的那个屏。
 *
 * <p>检查跑在服务端（密钥只在那一侧），结果异步回来。它原先走聊天栏——而<b>触发它的界面
 * 此刻正开着，聊天栏在界面底下看不见</b>，管理员点完按钮什么也没有，回执要退出界面翻
 * 聊天记录才读得到。</p>
 *
 * <p>这条按「发了 {@code CheckSiteConfigPackage} 的屏」枚举：谁发请求，谁就必须能显示回执。
 * 判据取的是**它发不发那个包**（语义稳），不是按钮标签或类名（表现层，一改就静默零覆盖）。</p>
 */
class SiteCheckResultWiringContractTest {
    private static final Path ROOT = Path.of("..", "..");
    private static final Path AI_GUI = ROOT.resolve(Path.of("src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid", "client", "gui", "entity", "maid", "ai"));
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern LINE_COMMENT = Pattern.compile("//[^\\n]*");

    /** LLM 与 TTS 两个编辑屏；STT 站点是本机数据，没有服务端检查 */
    private static final int MIN_REQUESTERS = 2;

    private static String activeSource(Path file) throws IOException {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        source = BLOCK_COMMENT.matcher(source).replaceAll(" ");
        return LINE_COMMENT.matcher(source).replaceAll(" ");
    }

    @Test
    void everyScreenThatAsksForACheckCanShowTheAnswer() throws IOException {
        List<String> requesters = new ArrayList<>();
        List<String> offenders = new ArrayList<>();
        int scanned = 0;

        try (Stream<Path> files = Files.walk(AI_GUI)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).sorted().toList()) {
                scanned++;
                String source = activeSource(file);
                if (!source.contains("new CheckSiteConfigPackage(")) {
                    continue;
                }
                String name = file.getFileName().toString();
                requesters.add(name);
                if (!source.contains("implements SiteCheckResultDisplay")
                        || !source.contains("public void showSiteCheckResult(")) {
                    offenders.add(name);
                }
            }
        }

        assertTrue(scanned > 0, "ai GUI 目录一个文件都没扫到，这条断言已失去看守对象");
        assertTrue(requesters.size() >= MIN_REQUESTERS,
                "只认出 %d 个发起检查的屏（期望 ≥ %d）——识别依据多半已失效。实见：%s"
                        .formatted(requesters.size(), MIN_REQUESTERS, requesters));
        assertTrue(offenders.isEmpty(),
                "这些屏发了检查请求却不能显示回执，管理员点完按钮会什么都看不到：" + offenders);
    }

    /**
     * 判词到期必须还原成常态标签。
     *
     * <p>没有独立计时器——靠每帧调一次 {@code applyCheckVerdict()} 顺带还原。
     * 所以那一次调用必须真的在 {@code extractRenderState} 里，漏了就会把判词永久留在按钮上。</p>
     */
    @Test
    void theVerdictIsRefreshedEveryFrame() throws IOException {
        List<String> offenders = new ArrayList<>();
        int checked = 0;
        try (Stream<Path> files = Files.walk(AI_GUI)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).sorted().toList()) {
                String source = activeSource(file);
                if (!source.contains("private void applyCheckVerdict()")) {
                    continue;
                }
                checked++;
                int render = source.indexOf("public void extractRenderState(");
                int apply = source.indexOf("this.applyCheckVerdict()", render < 0 ? 0 : render);
                if (render < 0 || apply < 0) {
                    offenders.add(file.getFileName().toString());
                }
            }
        }
        assertTrue(checked >= MIN_REQUESTERS,
                "只认出 %d 个带判词的屏（期望 ≥ %d），这条断言已失去看守对象".formatted(checked, MIN_REQUESTERS));
        assertTrue(offenders.isEmpty(),
                "这些屏没有在每帧还原判词，检查结果会永久留在按钮上：" + offenders);
    }
}
