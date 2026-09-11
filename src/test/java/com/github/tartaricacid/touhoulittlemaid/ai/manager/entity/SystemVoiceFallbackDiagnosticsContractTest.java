package com.github.tartaricacid.touhoulittlemaid.ai.manager.entity;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 「云端语音不出声」那条链上，两处必须留痕的地方。
 *
 * <p><b>2026-08-31 专服取证</b>：服务端 20 小时零 {@code TTS request failed}、客户端三份会话零
 * {@code Received TTS audio}、试听也无声且无红字——所有会打日志的环节全是好的，而症状是常态。
 * 静态分析出来的链条是：{@code MaidAIChatData.resolveTTSSite} 的最后一跳无条件回落到
 * {@code system} 站点，该站点默认存在且默认启用，于是「站点缺失 / 被禁 / 没人指定默认站点」
 * 三种配置错误全被静默翻译成「改用客户端的 MC 旁白」——它不发 HTTP、不发音频包，
 * 两端日志因此都是干净的。</p>
 *
 * <p>专服上这尤其致命：回落不改女仆存的 {@code ttsSite}，玩家界面与管理员的 {@code tts.json}
 * 双双显示"正常"，而唯一听得出问题的人在客户端。这两条日志是把两端接上的唯一一根线，
 * 所以它们值一道门。</p>
 */
class SystemVoiceFallbackDiagnosticsContractTest {
    private static final Path ROOT = Path.of("..", "..");
    private static final Path LLM_CALLBACK = source("ai", "manager", "entity", "LLMCallback.java");
    private static final Path SYSTEM_CLIENT = source("ai", "service", "tts", "system", "TTSSystemClient.java");
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    private static Path source(String... tail) {
        Path path = Path.of("src", "main", "java", "com", "github", "tartaricacid", "touhoulittlemaid");
        for (String part : tail) {
            path = path.resolve(part);
        }
        return ROOT.resolve(path);
    }

    /**
     * 告警必须挂在<b>走进 TTS 分支</b>的那一侧。
     *
     * <p>挂错边是很容易的：{@code logSilentReply} 就在同一个 {@code if/else} 的另一半，
     * 而这条回落的特征恰恰是"它成功了"——成功地改用了旁白。写进 else 里就永远不会触发。</p>
     */
    @Test
    void theSystemVoiceFallbackIsReportedFromInsideTheTtsBranch() throws IOException {
        String body = methodBody(activeSource(LLM_CALLBACK), "public void onSuccess(ResponseChat responseChat)");
        int branch = body.indexOf("} else if (");
        assertTrue(branch > 0, "onSuccess 里找不到 TTS 分支的 else，接线已变，本用例需要同步更新");

        int fallback = body.indexOf("logSystemVoiceFallback(");
        assertTrue(fallback > 0, "TTS 分支必须报告「其实回落到了系统旁白」——"
                + "否则这种配置错误在任何一侧都不留痕迹");
        assertTrue(fallback < branch, "logSystemVoiceFallback 必须在 if 一侧：回落的特征是它「成功」了，"
                + "写进 else 就永远不会触发");

        int silent = body.indexOf("logSilentReply(");
        assertTrue(silent > branch, "logSilentReply 必须留在 else 一侧：那条报的是「她压根没出声」，"
                + "两条报的是不同的故障，不能混在同一侧");
    }

    /** 去重是有意的，但不能把「谁触发的」一起省掉——不然日志说不出该去查哪只女仆的配置。 */
    @Test
    void theFallbackWarningNamesTheMaidAndTheFailingSite() throws IOException {
        String body = methodBody(activeSource(LLM_CALLBACK), "private void logSystemVoiceFallback(");
        assertTrue(body.contains("maid.getId()"), "告警必须点名是哪只女仆");
        assertTrue(body.contains("DEFAULT_TTS_SITE"), "站点为空时要能区分「她没选」与「世界默认也没设」，"
                + "后者是开箱即用的默认状态，也是最常见的成因");
        assertTrue(body.contains("SYSTEM_FALLBACK_WARNED"), "必须去重：配置错误是恒定的，"
                + "每句话吼一遍会把日志淹掉");
    }

    /**
     * 原版调旁白前先查 {@code active()}，我们从前不查——库没起来时 {@code say} 是纯空操作。
     */
    @Test
    void theNarratorIsCheckedBeforeSpeaking() throws IOException {
        String body = methodBody(activeSource(SYSTEM_CLIENT), "private void onHandle(String message)");
        int active = body.indexOf(".active()");
        int say = body.indexOf(".say(");
        assertTrue(active > 0, "必须查 narrator.active()：原版 GameNarrator 就是这么做的，"
                + "库没起来时 say 什么都不做且不报错");
        assertTrue(say > 0, "找不到 say 调用，接线已变，本用例需要同步更新");
        assertTrue(active < say, "active() 必须在 say() 之前——查在后面等于没查");
    }

    /**
     * {@code onHandle} 带 {@code @Environment(CLIENT)}，专服上整个方法会被剥掉。
     * 方法体里一旦出现 lambda，javac 会生成<b>不带注解的合成方法</b>，剥不掉，
     * 于是把 {@code Narrator} 留在专服的类签名里——statue / garage_kit / altar 三处已实证过一次。
     */
    @Test
    void theClientOnlyMethodHasNoLambda() throws IOException {
        String body = methodBody(activeSource(SYSTEM_CLIENT), "private void onHandle(String message)");
        assertTrue(!body.contains("->"), "@Environment(CLIENT) 的方法体里不得有 lambda："
                + "合成方法不带注解、剥不掉，会把客户端类型留在专服的类签名里");
    }

    /** 只保留会执行的源码：行注释与块注释（含 javadoc）全部剥掉。 */
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

    /** 按大括号配对截取方法体：断言范围必须缩到那个方法，不能对整份文件 indexOf。 */
    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, "找不到 " + signature + "，接线已变，本用例需要同步更新");
        int open = source.indexOf('{', start);
        assertTrue(open >= 0, signature + " 后面没有方法体");
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
        throw new AssertionError(signature + " 的大括号不配对");
    }
}
