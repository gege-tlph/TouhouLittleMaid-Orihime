package com.github.tartaricacid.touhoulittlemaid.ai.manager.entity;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 待合成文本这条链的两条硬约束：<b>翻译请求不许带历史</b>，<b>拿不到译文不许静默</b>。
 *
 * <p><b>为什么有这个类。</b>原设计让模型在同一条回复里用 {@code ---} 分出第二段。
 * 逐轮实测（2026-07-30，deepseek-v4-flash，读请求与响应原文）：</p>
 *
 * <pre>
 * 轮次  历史里单段 assistant 范例  索取两段  拿到第二段
 *  1              0                 是         是
 *  2              1                 是         否
 *  3              2                 是         否
 *  4              3                 是         否
 * </pre>
 *
 * <p>助手历史只存对话文本（那是为了不让旧语言成为范例），于是上下文里<b>只剩单段反例、
 * 没有一条两段正例</b>。示范胜过指令，把要求重述在历史之后只是位置竞争，压不过它。
 * 而缺第二段被兜底静默翻译成「拿对话文本去合成」，于是玩家听见「语音又变回中文了」，
 * 排查的人在日志里什么也看不到。</p>
 *
 * <p>修法是把译文移出对话通道：一次<b>不带历史</b>的独立翻译请求。它的有效性完全依赖
 * 「真的不带历史」——一旦有人「顺手把人设/历史也带上」，漂移立刻原样回来，而且照样静默。
 * 所以这两条必须是断言，不是注释。</p>
 */
class MissingTtsPartIsNotSilentContractTest {
    private static final Path ROOT = Path.of("..", "..");
    private static final Path ENTITY = ROOT.resolve(Path.of("src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid", "ai", "manager", "entity"));
    private static final Path MANAGER = ENTITY.resolve("MaidAIChatManager.java");
    private static final Path TRANSLATION = ENTITY.resolve("TtsTranslationCallback.java");
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    /**
     * <b>本类的主菜</b>：翻译请求的消息列表里不得出现历史、人设或工具。
     *
     * <p>这三样正是漂移的载体。带上任何一样，「结构上不可能漂移」就退化成「概率低一些」，
     * 而实测证明概率低一些是不够的——第 2 轮就翻了。</p>
     */
    @Test
    void theTranslationRequestCarriesNoConversationContext() throws IOException {
        String body = methodBody(activeSource(MANAGER),
                "public void requestTtsTranslation(TTSSite ttsSite, String chatText, long waitingChatBubbleId)");

        for (String forbidden : new String[]{"getHistory", "buildMessage", "customSetting", "getSetting",
                "UserPromptContexts", "appendSummaryMessage", "trailingRequirements"}) {
            assertTrue(!body.contains(forbidden),
                    "翻译请求不得携带对话上下文，出现了 " + forbidden + "：带上历史或人设就等于把漂移放回来");
        }
        assertTrue(body.contains("ttsTranslationPrompt"),
                "翻译请求必须用专用的翻译提示词，不得复用主对话那套");

        String callback = activeSource(TRANSLATION);
        assertTrue(callback.contains("needAddTools = false"),
                "翻译请求不得带工具：给了只是多一个跑偏的机会");
    }

    /** 主对话不得再索取两段：提示词要求的形状必须与历史里实际出现的形状一致 */
    @Test
    void theMainConversationNeverAsksForTwoParts() throws IOException {
        Path papi = ROOT.resolve(Path.of("src", "main", "java", "com", "github", "tartaricacid",
                "touhoulittlemaid", "ai", "manager", "setting", "papi", "PapiReplacer.java"));
        String body = methodBody(activeSource(papi), "private static String outputFormat(");

        assertTrue(body.contains("OUTPUT_FORMAT_REQUIREMENTS_SINGLE"),
                "主对话的格式要求必须恒定单段");
        assertTrue(!body.contains("DIFFERENT_LANGUAGES"),
                "两段模板已删除，不得复活：它要求的是一种助手历史里从未出现过的形状");
    }

    /**
     * 每一条「用对话文本代替译文」的降级出口都必须留声。
     *
     * <p>凡降级必留声，是本仓库反复栽过之后立下的规矩：原 P0「服务器提供 STT 不可用」的真因
     * 就是站点文件损坏后整批放弃、站点表变空，而下游把空表翻译成了一句语义相反的用户提示。</p>
     */
    @Test
    void everyFallbackToTheChatTextIsLogged() throws IOException {
        String degrade = methodBody(activeSource(TRANSLATION), "private void degrade(String cause)");
        assertTrue(degrade.contains("LOGGER.warn"), "degrade 必须先留声再降级");
        assertTrue(degrade.contains("tts("), "degrade 必须真的把声音发出去：有声音好过没声音");

        String onSuccess = methodBody(activeSource(TRANSLATION), "public void onSuccess(ResponseChat response)");
        String onFailure = methodBody(activeSource(TRANSLATION),
                "public void onFailure(@Nullable HttpRequest request, Throwable throwable, int errorCode)");
        assertTrue(onSuccess.contains("degrade("), "译文为空时必须走 degrade，不得直接合成对话文本");
        assertTrue(onFailure.contains("degrade("), "请求失败时必须走 degrade");
        assertTrue(!onFailure.contains("super.onFailure"),
                "不得调用父类 onFailure：那会撤掉等待气泡并给主人报一个 LLM 错误，"
                        + "而此刻回复本身是好的，玩家该看到女仆正常说话");

        String noSite = methodBody(activeSource(MANAGER),
                "public void requestTtsTranslation(TTSSite ttsSite, String chatText, long waitingChatBubbleId)");
        int warn = noSite.indexOf("LOGGER.warn");
        int fallback = noSite.indexOf("this.tts(");
        assertTrue(warn >= 0 && fallback > warn,
                "没有可用 LLM 站点时同样是降级，必须先留声再合成对话文本");
    }

    /** 断言方法内部的关系时必须先把范围缩到那个方法体 */
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
