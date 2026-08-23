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
 * hub 的全称契约：<b>在任何一页点「保存」都会把攒着的全部改动一起提交。</b>
 *
 * <p>这条测试存在的理由，是我在别处把这句契约写了三遍（字段注释、javadoc、提交信息），
 * 而当时语音输入页的保存<b>从不碰共享暂存</b>，还无条件闪「已保存」——两次点击就能让规则
 * 静默丢失且用户被告知成功。<b>自己写下的契约是意图，不是证据</b>；越是言之凿凿写了三遍，
 * 越不会有人回去核对。凡写下「所有 X 都会 Y」这类全称契约，当场加一条按 X 枚举的测试。</p>
 *
 * <p><b>识别依据取结构而非表现层</b>：判「这一屏有保存按钮」用的是「它覆写了
 * {@code addFooterButtons}」，不是按钮标签的字面量——标签换成常量、换成别的文案，
 * 按字面量识别会**静默零覆盖**，而零覆盖的测试永远是绿的。为此另配一条下限断言。</p>
 */
class HubSharedStagingContractTest {
    private static final Path ROOT = Path.of("..", "..");
    private static final Path SETTINGS = ROOT.resolve(Path.of("src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid", "client", "gui", "entity", "maid", "ai", "settings"));
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern LINE_COMMENT = Pattern.compile("//[^\\n]*");

    /** 这几页确实各自有一颗保存按钮；少于这个数就说明识别依据已经失效 */
    private static final int MIN_SAVING_SCREENS = 4;

    private static String activeSource(Path file) throws IOException {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        // 注释里的接线不算接线；javadoc 也要剥掉（BLOCK_COMMENT 已覆盖 /** */）
        source = BLOCK_COMMENT.matcher(source).replaceAll(" ");
        return LINE_COMMENT.matcher(source).replaceAll(" ");
    }

    @Test
    void everyScreenWithASaveButtonCommitsTheSharedStaging() throws IOException {
        List<String> savingScreens = new ArrayList<>();
        List<String> offenders = new ArrayList<>();
        int scanned = 0;

        try (Stream<Path> files = Files.list(SETTINGS)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).sorted().toList()) {
                scanned++;
                String source = activeSource(file);
                String name = file.getFileName().toString();
                // 骨架自己那颗「返回」不算：它没有覆写，是 hub 的基类实现
                boolean ownsSaveButton = source.contains("protected void addFooterButtons()")
                        && !name.equals("AIChatSettingsHubScreen.java");
                if (!ownsSaveButton) {
                    continue;
                }
                savingScreens.add(name);
                if (!source.contains("ruleSession().save()")) {
                    offenders.add(name);
                }
            }
        }

        // 活性判据与结论正交：这里数的是「扫到了几个源文件」与「认出了几屏保存按钮」，
        // 不是「有几处违规」——违规为零与压根没扫到，在结果上完全一样。
        assertTrue(scanned > 0, "settings 目录一个文件都没扫到，这条断言已失去看守对象");
        assertTrue(savingScreens.size() >= MIN_SAVING_SCREENS,
                "只认出 %d 屏有保存按钮（期望 ≥ %d）——识别依据多半已经失效，这条测试正在零覆盖空转。实见：%s"
                        .formatted(savingScreens.size(), MIN_SAVING_SCREENS, savingScreens));
        assertTrue(offenders.isEmpty(),
                "这些屏有自己的保存按钮却不提交共享暂存 ruleSession().save()，"
                        + "于是「带着别页的未保存改动切过来 → 点保存」会告诉玩家成功、实际一个字也没上服务器："
                        + offenders);
    }

    /**
     * 暂存必须挂在共享状态上，不能是每屏一个。
     *
     * <p>每屏各自 {@code createSession()} 的话，跨页改动根本汇不到一起——上面那条断言
     * 会全绿，而契约照样不成立。这是同一条契约的另一半。</p>
     */
    @Test
    void theStagingSessionLivesOnTheSharedState() throws IOException {
        Path hub = SETTINGS.resolve("AIChatSettingsHubScreen.java");
        String source = activeSource(hub);
        assertTrue(source.contains("this.state.ruleSession"),
                "暂存没有挂在 SharedState 上，跨页改动汇不到一起");

        List<String> offenders = new ArrayList<>();
        int scanned = 0;
        try (Stream<Path> files = Files.list(SETTINGS)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).sorted().toList()) {
                scanned++;
                if (file.getFileName().toString().equals("AIChatSettingsHubScreen.java")) {
                    continue;
                }
                if (activeSource(file).contains("ServerRulesClientCache.createSession()")) {
                    offenders.add(file.getFileName().toString());
                }
            }
        }
        assertTrue(scanned > 0, "settings 目录一个文件都没扫到，这条断言已失去看守对象");
        assertTrue(offenders.isEmpty(),
                "这些屏自己建了 Session，绕过了共享暂存（唯一入口是 hub 的 ruleSession()）：" + offenders);
    }
}
