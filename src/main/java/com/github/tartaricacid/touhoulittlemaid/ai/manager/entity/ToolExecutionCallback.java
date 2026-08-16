package com.github.tartaricacid.touhoulittlemaid.ai.manager.entity;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.response.ResponseChat;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMMessage;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.net.http.HttpRequest;
import java.util.List;

/**
 * 动作执行：判定为动作之后，用一条**不带历史**的请求把工具真正调出来。
 *
 * <p>它<b>只负责动手，不负责说话</b>——女仆已经在第一轮回过话了。若这里再产出一段文本，
 * 玩家会看到同一句话被回两遍。因此 {@link #onSuccess} 是有意的空实现：
 * 工具由父类的 {@code onFunctionCall} 执行并把结果写进历史，走到这里就说明该做的已经做完。</p>
 *
 * <p>不带历史是这条链的关键，不是省 token 的顺手之举：实测中弱档位模型只在空历史下才会调用工具，
 * 见 {@code StringConstant#TOOL_DISPATCH_DECISION} 里的逐轮数据。<b>谁要是往这条请求里加历史或人设，
 * 这个修复就当场失效</b>，而且失效得毫无声响——所以有契约测试钉着。</p>
 */
public class ToolExecutionCallback extends LLMCallback {
    /** 动作做完（无论成败）都要走的下一步：让女仆把已经发生的事讲出来 */
    private final Runnable narrate;

    public ToolExecutionCallback(MaidAIChatManager chatManager, List<LLMMessage> messages, Runnable narrate) {
        // subagents=true：不建等待气泡，这一步对玩家不可见
        super(chatManager, messages, true);
        this.narrate = narrate;
    }

    /**
     * 只挂动作类工具，与判定阶段的白名单是同一份。
     *
     * <p>不限制的话，这条**不可见**的旁路会把 {@code use_skill} 也挂出去，而它会起一个知识库
     * 子 agent，并拿着本回调的 {@code waitingChatBubbleId}（这里是 0）去碰聊天气泡——
     * 与刚修掉的孤儿气泡同一类问题。{@code query_minecraft_wiki} 同理：它既不是动作，
     * 又会在玩家看不见的路径上发外部请求。</p>
     */
    @Override
    public boolean allowsTool(String toolId) {
        return ToolDispatchCallback.ACTION_TOOLS.contains(toolId);
    }

    @Override
    public boolean shouldCacheTokenUsage() {
        return false;
    }

    /**
     * <b>这条旁路不许碰聊天气泡。</b>
     *
     * <p>父类在执行工具时会调它，往等待气泡下面追一行「正在调用 xxx」——那对主对话是有用的进度提示，
     * 但对这里是灾难：本回调的 {@code waitingChatBubbleId} 是 0（{@code subagents=true} 没建气泡），
     * 而 {@code refreshThinkingText} 会**新建**一个思考气泡；本类的 {@link #onSuccess} 又是空实现，
     * 于是那个气泡没有任何人来替换，只能挂到 90 秒超时。</p>
     *
     * <p>实测表现：玩家已经看到女仆回话了，头顶却一直挂着「少女思考中……切换为 坐下」。
     * 用户截图定的案——在此之前我一直在查 TTS 那条路。</p>
     *
     * <p>顺带堵掉第二个洞：{@code refreshThinkingText} 对 {@code previousChatBubbleId >= 0} 会先 remove，
     * 而 0 是个合法 id，可能误删别人的气泡。</p>
     */
    @Override
    public void refreshWaitingChatBubble(Component summaryComponent) {
        // 有意留空：这一步对玩家不可见
    }

    /**
     * 有意什么都不做。
     *
     * <p>走到这里有两种情形：① 工具已执行完，父类回来收尾——此时再说话就是重复回复；
     * ② 模型没调工具而是回了段文本——那说明判定过严或模型不配合，此时也没有值得说的东西。
     * 两种情形下正确的行为都是沉默。</p>
     */
    @Override
    public void onSuccess(ResponseChat response) {
        // 见上：这里不说话，改由主对话去说——那时历史里已经有刚才的调用与结果
        this.runOnServerThread(this.narrate);
    }

    /** 与判定那条同理：辅助请求失败不该打扰玩家，但必须留声 */
    @Override
    public void onFailure(@Nullable HttpRequest request, Throwable throwable, int errorCode) {
        TouhouLittleMaid.LOGGER.warn("Tool dispatch execution failed for maid {} (error {}): {}",
                this.maid.getId(), errorCode, throwable.getMessage());
        // 执行失败同样要让她开口——历史里没有成功记录，她会照实说没做到
        this.runOnServerThread(this.narrate);
    }
}
