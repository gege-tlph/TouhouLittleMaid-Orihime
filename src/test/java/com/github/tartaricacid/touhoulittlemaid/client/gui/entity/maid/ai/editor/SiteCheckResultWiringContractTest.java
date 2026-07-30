package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.editor;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 「检查配置」回执的去处。
 *
 * <p>由来：检查跑在服务端（密钥只在那一侧），回执原先走 {@code displayClientMessage} 打进聊天栏。
 * 而**这颗按钮只存在于站点编辑屏上，按它的时候那个屏必然开着，聊天栏在它底下看不见**——
 * 管理员点完按钮屏幕上什么也没有，回执要退出界面翻聊天记录才读得到。</p>
 *
 * <p>这条契约按「屏里有没有 check_config 按钮」枚举，所以以后哪个屏加上这颗按钮都会自动纳入；
 * 靠注释写一句「回执会显示在界面里」是不够的——<b>自己写下的契约是意图，不是证据</b>。</p>
 */
class SiteCheckResultWiringContractTest {
    private static final Path ROOT = Path.of("..", "..");
    private static final Path SOURCE = ROOT.resolve(Path.of("src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid"));
    private static final Path EDITOR_DIR = SOURCE.resolve(Path.of("client", "gui", "entity", "maid", "ai", "editor"));
    private static final Path CHECK_PACKAGE = SOURCE.resolve(Path.of("network", "message", "ai",
            "CheckSiteConfigPackage.java"));
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    /** <b>主菜</b>：凡有「检查配置」按钮的屏，都必须能就地显示回执 */
    @Test
    void everyScreenWithTheCheckButtonCanDisplayTheResult() throws IOException {
        List<String> offenders = new ArrayList<>();
        int checked = 0;
        try (Stream<Path> files = Files.list(EDITOR_DIR)) {
            for (Path file : files.filter(p -> p.getFileName().toString().endsWith("Screen.java")).toList()) {
                String source = activeSource(file);
                // 识别依据取「这屏发不发检查请求」，而不是某个按钮标签的字面量——
                // 标签换成 CHECK_CONFIG_NAME 常量后，按字面量找会静默地零覆盖
                if (!source.contains("CheckSiteConfigPackage")) {
                    continue;
                }
                checked++;
                if (!source.contains("SiteCheckResultDisplay") || !source.contains("showSiteCheckResult")) {
                    offenders.add(file.getFileName().toString());
                }
            }
        }
        assertTrue(checked > 0, "一个带「检查配置」按钮的屏都没找到，这条断言已失去看守对象");
        assertTrue(offenders.isEmpty(),
                "这些屏有「检查配置」按钮却不实现 SiteCheckResultDisplay，回执会退回聊天栏——"
                        + "而点按钮时这个屏正挡着聊天栏：" + String.join(", ", offenders));
    }

    /**
     * 判词只是一个词，<b>完整原因必须进悬停提示，且判词必须会还原</b>。
     *
     * <p>「界面里要简洁」不等于把诊断信息丢掉：把「地址不通」和「密钥没填」分开，正是这个功能
     * 存在的理由（它明确不验证密钥是否正确）。若只留一个「配置错误」而不给详情，就等于退回到
     * 重构前那句笼统的失败。</p>
     *
     * <p>还原同样是必须的：不还原的话按钮会永远写着「检查通过」，下次谁都不知道自己看的是
     * 这一次的结果还是上一次的残留——那正是本仓库栽过的「把历史当现状」。</p>
     */
    @Test
    void theVerdictCarriesTheDetailAndExpires() throws IOException {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.list(EDITOR_DIR)) {
            for (Path file : files.filter(p -> p.getFileName().toString().endsWith("Screen.java")).toList()) {
                String source = activeSource(file);
                // 识别依据取「这屏发不发检查请求」，而不是某个按钮标签的字面量——
                // 标签换成 CHECK_CONFIG_NAME 常量后，按字面量找会静默地零覆盖
                if (!source.contains("CheckSiteConfigPackage")) {
                    continue;
                }
                String body = methodBody(source, "private void applyCheckVerdict()");
                boolean carriesDetail = body.contains("setTooltips") && body.contains("checkDetail");
                boolean expires = body.contains("CHECK_RESULT_MS");
                boolean restores = body.contains("CHECK_CONFIG_NAME") && body.contains("clearTooltips");
                if (!carriesDetail || !expires || !restores) {
                    offenders.add("%s(详情=%b 到期=%b 还原=%b)".formatted(
                            file.getFileName(), carriesDetail, expires, restores));
                }
            }
        }
        assertTrue(offenders.isEmpty(),
                "这些屏的判词没带完整原因、或不会到期还原：" + String.join(", ", offenders));
    }

    /**
     * <b>没有可配置项的站点不许显示「保存」与「检查配置」。</b>
     *
     * <p>系统朗读器站点 {@code url()} 返回空串，检查必然停在「站点还没填地址」这条红字上，
     * 永远不可能成功；它的 {@code buildSite} 返回的是一份逐字相同的副本，存了等于没存。
     * <b>只能骗人的按钮不该只是没用，而是不该出现</b>——与「不许有说谎的标签」同一条纪律。</p>
     */
    @Test
    void aSiteWithNothingToConfigureShowsNoSaveOrCheckButton() throws IOException {
        String source = activeSource(EDITOR_DIR.resolve("TTSSiteEditorScreen.java"));
        int guard = source.indexOf("layout.isConfigurable()");
        int check = source.indexOf("CHECK_CONFIG_X");
        int save = source.indexOf("SAVE_X");
        assertTrue(guard >= 0, "必须按「有没有可配置项」决定是否显示这两颗按钮");
        assertTrue(guard < check && guard < save,
                "守卫必须在两颗按钮之前：检查与保存对无可配置项的站点都只能骗人");
    }

    /** 服务端不许再把回执直接写进聊天栏：那是玩家此刻读不到的地方 */
    @Test
    void theServerAnswersThroughThePacketNotTheChatBox() throws IOException {
        String source = activeSource(CHECK_PACKAGE);
        assertTrue(!source.contains("displayClientMessage"),
                "CheckSiteConfigPackage 不得用 displayClientMessage 回执：发起检查的界面正挡着聊天栏");
        assertTrue(source.contains("SiteCheckResultPackage"),
                "回执必须经 SiteCheckResultPackage 回到发起它的那个屏");
    }

    /**
     * 回执配色必须带 alpha。
     *
     * <p>1.21.11 的 {@code Font} 不再把 alpha=0 补成不透明。仆从铃标记就是这么栽的：
     * 照抄基准的 {@code 0xff8800}，事件在跑、文字算对、屏幕上全透明。</p>
     */
    @Test
    void everyResultColourCarriesAlpha() throws IOException {
        String source = activeSource(CHECK_PACKAGE);
        Matcher matcher = Pattern.compile("COLOR_[A-Z_]+\\s*=\\s*(0[xX][0-9a-fA-F]+)").matcher(source);
        List<String> bad = new ArrayList<>();
        int found = 0;
        while (matcher.find()) {
            found++;
            String literal = matcher.group(1);
            // 八位十六进制且高字节非零，才是真正不透明
            if (literal.length() != 10 || Integer.parseInt(literal.substring(2, 4), 16) == 0) {
                bad.add(literal);
            }
        }
        assertTrue(found >= 3, "没找到回执配色常量，这条断言已失去看守对象");
        assertTrue(bad.isEmpty(), "这些回执配色缺 alpha，1.21.11 会画成全透明：" + String.join(", ", bad));
    }

    /** 断言方法内部的关系时必须先把范围缩到那个方法体，否则有效性依赖「文件里只有一处」 */
    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, "源码里找不到方法：" + signature);
        int brace = source.indexOf('{', start);
        assertTrue(brace >= 0, "方法没有方法体：" + signature);
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
        throw new IllegalStateException("方法体大括号不配对：" + signature);
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
