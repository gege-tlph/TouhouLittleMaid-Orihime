package com.github.tartaricacid.touhoulittlemaid.ai.manager.entity;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 动作判定与执行这条旁路的硬约束。
 *
 * <p><b>逐轮实测</b>（2026-07-30，同一个 {@code deepseek-v4-flash}、同一句「现在开始跟着我」，
 * 读请求与响应原文）：</p>
 *
 * <pre>
 * 历史                     调用工具
 * 空                          是
 * 2 轮纯聊天                  否
 * 6 轮纯聊天                  否
 * 6 轮 + 合成正例             否   ← 「补正例」方向已证伪并删除
 * 26 轮纯聊天                 否
 * deepseek-v4-pro / 26 轮     是
 * </pre>
 *
 * <p>结论：<b>对话历史里只要出现助手回合，弱档位模型就不再调用工具</b>。修法是把判断与执行
 * 搬进两条不带历史的请求——空历史正是它表现正常的唯一条件。<b>这个修复的有效性完全依赖
 * 「真的不带历史」</b>：谁往里加一句历史或人设，它就当场失效，而且失效得毫无声响
 * （女仆照样会回话，只是不动手）。所以这两条必须是断言，不是注释。</p>
 */
class ToolDispatchWiringContractTest {
    private static final Path ROOT = Path.of("..", "..");
    private static final Path SRC = ROOT.resolve(Path.of("src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid"));
    private static final Path MANAGER = SRC.resolve(Path.of("ai", "manager", "entity", "MaidAIChatManager.java"));
    private static final Path DISPATCH = SRC.resolve(Path.of("ai", "manager", "entity", "ToolDispatchCallback.java"));
    private static final Path EXECUTION = SRC.resolve(Path.of("ai", "manager", "entity", "ToolExecutionCallback.java"));
    private static final Path MAIN_CALLBACK = SRC.resolve(Path.of("ai", "manager", "entity", "LLMCallback.java"));
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    /**
     * 会把**对话**带进请求的东西，一个都不许出现在这两条旁路里。
     *
     * <p><b>注意 {@code UserPromptContexts} 不在名单里，这是有意的</b>：实测出来的抑制源于
     * <b>助手回合的示范</b>（历史里全是「只说话不动手」的回复），而 {@code <context>} 是一条
     * <b>user 消息</b>里的状态快照，不构成示范。判定阶段需要它——不给状态，「坐下」在她已经坐着时
     * 照样被判成动作，白白多走一次执行（实测多花 3 次请求约 3 秒）。</p>
     *
     * <p>⚠️ 这条豁免的安全性由实机验证背书，不是推理：见提交里「带状态快照后重跑两次坐下」那一节。</p>
     */
    private static final List<String> CONTEXT_CARRIERS = List.of(
            "getHistory()", "buildMessage", "customSetting", "getSetting",
            "appendSummaryMessage", "trailingRequirements", "replaceSetting");

    /** <b>本类的主菜</b>：判定请求不得携带任何对话上下文 */
    @Test
    void theDecisionRequestCarriesNoConversationContext() throws IOException {
        String body = methodBody(activeSource(MANAGER), "public void requestToolDispatch(String rawMessage, Runnable narrate, Runnable narrateAfterAction)");
        for (String carrier : CONTEXT_CARRIERS) {
            if (carrier.equals("getHistory()")) {
                // 这条方法确实要读历史长度来决定要不要走，但只能用于判空，不能进消息
                continue;
            }
            assertTrue(!body.contains(carrier),
                    "判定请求不得携带对话，出现了 " + carrier + "：抑制来自助手回合的示范");
        }
        assertTrue(body.contains("TOOL_DISPATCH_DECISION"), "必须用专用的判定提示词");
        assertTrue(!body.contains("LLMMessage.assistantChat"),
                "判定请求里不得出现任何 assistant 回合——那正是抑制的来源");
    }

    /** 执行请求同样不得携带对话上下文 */
    @Test
    void theExecutionRequestCarriesNoConversationContext() throws IOException {
        String body = methodBody(activeSource(MANAGER), "public void requestToolExecution(String rawMessage, Runnable narrate)");
        for (String carrier : CONTEXT_CARRIERS) {
            assertTrue(!body.contains(carrier),
                    "执行请求不得携带对话上下文，出现了 " + carrier + "：加回去这个修复就当场失效");
        }
        assertTrue(body.contains("TOOL_DISPATCH_EXECUTION"), "必须用专用的执行提示词");
    }

    /**
     * 判定必须不带工具，执行必须带工具。
     *
     * <p>判定若带上工具，模型可能直接动手——那一步要在下一条请求里带着完整工具做，
     * 否则「判定」与「执行」混在一起，判错时也没有第二道关。</p>
     */
    @Test
    void theDecisionHasNoToolsAndTheExecutionKeepsThem() throws IOException {
        assertTrue(activeSource(DISPATCH).contains("needAddTools = false"),
                "判定请求不得带工具");
        assertTrue(!activeSource(EXECUTION).contains("needAddTools = false"),
                "执行请求必须保留工具，否则它没有任何东西可调");
    }

    /**
     * 判定必须取严：只认精确的工具名。
     *
     * <p>误判的代价不对称——漏掉一次动作，玩家再说一遍就是了；而闲聊时女仆突然改状态，
     * 远比没反应更吓人。</p>
     */
    @Test
    void anUnrecognisedVerdictDoesNothing() throws IOException {
        String body = methodBody(activeSource(DISPATCH), "public void onSuccess(ResponseChat response)");
        assertTrue(body.contains("ACTION_TOOLS.contains"),
                "判定结果必须与精确工具名比对");
        int guard = body.indexOf("ACTION_TOOLS.contains");
        int trigger = body.indexOf("requestToolExecution");
        assertTrue(guard >= 0 && trigger > guard,
                "必须先过白名单再触发执行，不能先动手再检查");
    }

    /**
     * 执行回调不得再产出一段回复。
     *
     * <p>女仆已经在第一轮回过话了；这里再说一句，玩家会看到同一句话被回两遍。</p>
     */
    @Test
    void theExecutionCallbackStaysSilent() throws IOException {
        String body = methodBody(activeSource(EXECUTION), "public void onSuccess(ResponseChat response)");
        assertTrue(!body.contains("addLLMChatText") && !body.contains("tts(") && !body.contains("addAssistantHistory"),
                "执行回调只负责动手不负责说话，否则同一句话会被回两遍");
    }

    /**
     * <b>旁路不许碰聊天气泡。</b>
     *
     * <p>父类执行工具时会调 {@code refreshWaitingChatBubble} 追一行进度提示——对主对话有用，
     * 对这条不可见的旁路是灾难：它会**新建**一个思考气泡，而本回调的 {@code onSuccess} 是空实现，
     * 没有任何人来替换它，只能挂到 90 秒超时。实测表现是玩家已经看到回话了，
     * 女仆头顶却一直挂着「少女思考中……切换为 坐下」。</p>
     */
    @Test
    void theExecutionCallbackTouchesNoChatBubble() throws IOException {
        String source = activeSource(EXECUTION);
        assertTrue(source.contains("public void refreshWaitingChatBubble"),
                "执行回调必须覆写 refreshWaitingChatBubble：不覆写就会留下一个没人替换的思考气泡");
        String body = methodBody(source, "public void refreshWaitingChatBubble(Component summaryComponent)");
        assertTrue(!body.contains("refreshThinkingText") && !body.contains("super."),
                "覆写必须是空实现——这一步对玩家不可见，任何气泡操作都会留下孤儿气泡");
    }

    /**
     * <b>执行旁路只许拿到动作类工具。</b>
     *
     * <p>{@code needAddTools} 是全有或全无，所以不限制就等于把 {@code use_skill} 也挂了出去——
     * 而它会起一个知识库子 agent，并拿着本回调的 {@code waitingChatBubbleId}（这里是 0）
     * 去碰聊天气泡，与孤儿气泡同一类问题。{@code query_minecraft_wiki} 同理：既不是动作，
     * 又会在玩家看不见的路径上发外部请求。</p>
     */
    @Test
    void theExecutionCallbackOnlyGetsActionTools() throws IOException {
        String source = activeSource(EXECUTION);
        assertTrue(source.contains("public boolean allowsTool"),
                "执行回调必须限制工具集合，否则 use_skill / query_minecraft_wiki 会挂到不可见路径上");
        String body = methodBody(source, "public boolean allowsTool(String toolId)");
        assertTrue(body.contains("ACTION_TOOLS"),
                "白名单必须与判定阶段共用同一份，两处各写一份迟早分叉");
    }

    /**
     * <b>先做，后说。</b>
     *
     * <p>判定必须发生在主对话**之前**。原先是「回话 → 判定 → 执行」，于是女仆有机会承诺一件
     * 还没发生的事——实测她回过「好的主人，酒狐跟着你走啦~」而工具一次都没调。
     * <b>说了没做，比说做不到糟得多</b>：后者只是让人失望，前者一次就摧毁信任。</p>
     */
    @Test
    void theDecisionHappensBeforeTheMaidSpeaks() throws IOException {
        String manager = activeSource(MANAGER);
        String body = methodBody(manager, "private void decideThenChat(");
        assertTrue(body.contains("requestToolDispatch"), "判定必须在主对话之前发起");
        assertTrue(body.contains("tryToChat"), "说话必须作为判定之后的续程");

        String onSuccess = methodBody(activeSource(MAIN_CALLBACK), "public void onSuccess(ResponseChat responseChat)");
        assertTrue(!onSuccess.contains("requestToolDispatch"),
                "主回调里不得再触发判定：那正是「先说后做」的旧顺序，会让她承诺没发生的事");
    }

    /**
     * <b>任何分支都必须继续走到「说话」。</b>
     *
     * <p>等待气泡在判定之前就建好了。判定或执行只要有一条路径不往下走，玩家就会被永远晾在
     * 一个「少女思考中」上——那正是今天刚修过的孤儿气泡，只是换了个成因。
     * 失败时退化成「只说话」，也就是从前的行为，不会更糟。</p>
     */
    @Test
    void everyBranchStillReachesTheSpeakingStep() throws IOException {
        for (Path file : List.of(DISPATCH, EXECUTION)) {
            String source = activeSource(file);
            String name = file.getFileName().toString();
            assertTrue(source.contains("narrate"), name + " 必须持有并调用续程");
            String onFailure = methodBody(source,
                    "public void onFailure(@Nullable HttpRequest request, Throwable throwable, int errorCode)");
            assertTrue(onFailure.contains("narrate"),
                    name + " 的失败路径也必须继续走到说话，否则玩家被晾在思考气泡上");
        }
        String verdict = methodBody(activeSource(DISPATCH), "public void onSuccess(ResponseChat response)");
        assertTrue(verdict.split("narrate", -1).length - 1 >= 1,
                "判定答 NONE 的那条分支也必须继续说话");

        String manager = activeSource(MANAGER);
        for (String method : List.of("public void requestToolDispatch(String rawMessage, Runnable narrate, Runnable narrateAfterAction)",
                "public void requestToolExecution(String rawMessage, Runnable narrate)")) {
            String body = methodBody(manager, method);
            assertTrue(body.contains("narrate.run()"),
                    method + " 的早退分支（站点不可用）必须直接走到说话");
        }
    }

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
