package com.github.tartaricacid.touhoulittlemaid.ai.manager.entity;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.response.ResponseChat;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMMessage;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.Set;

/**
 * 动作判定：这句话是不是一条要女仆动手的直接指令。
 *
 * <p>成因与实测见 {@code StringConstant#TOOL_DISPATCH_DECISION}：
 * <b>对话历史里只要出现助手回合，弱档位模型就不再调用工具</b>，而空历史时一切正常。
 * 所以把这个判断搬进一条不带历史、不带人设、不带工具的请求里——正是它表现正常的那个条件。</p>
 *
 * <p><b>判错的代价是不对称的，所以判据取严</b>：只认那几个精确的工具名，模型答别的、
 * 答多余的字、或者干脆跑题，一律当作「不是动作」而什么都不做。漏掉一次动作，玩家再说一遍就是了；
 * 而误判会让女仆在闲聊时突然改状态——那种「自己动起来」的错误远比没反应更吓人。</p>
 */
public class ToolDispatchCallback extends LLMCallback {
    /** 只认这四个状态类工具：它们是「玩家下令改状态」这件事的全部出口 */
    static final Set<String> ACTION_TOOLS = Set.of(
            "switch_follow_state", "switch_sit", "switch_schedule", "switch_work_task");

    private final String rawMessage;
    /** 判定完（无论结果如何）都要走的下一步：让女仆开口。绝不能有任何分支把它漏掉 */
    private final Runnable narrate;
    /** 动作真的做完之后走的那条：主对话届时只说不动 */
    private final Runnable narrateAfterAction;

    public ToolDispatchCallback(MaidAIChatManager chatManager, List<LLMMessage> messages,
                                String rawMessage, Runnable narrate, Runnable narrateAfterAction) {
        // subagents=true：等待气泡是主对话那一轮的事，这条判定对玩家不可见
        super(chatManager, messages, true);
        this.rawMessage = rawMessage;
        this.narrate = narrate;
        this.narrateAfterAction = narrateAfterAction;
        // 判定本身不需要工具，给了反而可能让它直接动手——那一步要在下一条请求里带着完整工具做
        this.needAddTools = false;
    }

    /** 判定不进上下文压缩的 token 账：它不在主对话的历史里 */
    @Override
    public boolean shouldCacheTokenUsage() {
        return false;
    }

    @Override
    public void onSuccess(ResponseChat response) {
        String verdict = StringUtils.trimToEmpty(response.asSinglePart().getChatText());
        if (!ACTION_TOOLS.contains(verdict)) {
            // NONE、空串、或任何多余内容都走这里：判据取严，宁可不动手
            this.runOnServerThread(this.narrate);
            return;
        }
        TouhouLittleMaid.LOGGER.debug("Tool dispatch for maid {}: verdict={}", this.maid.getId(), verdict);
        this.chatManager.requestToolExecution(this.rawMessage, this.narrateAfterAction);
    }

    /**
     * 判定失败只留一行日志：第一轮的回复已经发出去了，玩家该看到的东西一样不少，
     * 不能因为这条**辅助**请求失败就给他弹一个 LLM 错误。
     */
    @Override
    public void onFailure(@Nullable HttpRequest request, Throwable throwable, int errorCode) {
        TouhouLittleMaid.LOGGER.warn("Tool dispatch decision failed for maid {} (error {}): {}",
                this.maid.getId(), errorCode, throwable.getMessage());
        // 判定失败绝不能把玩家晾在思考气泡上——退化成「只说话」，也就是从前的行为
        this.runOnServerThread(this.narrate);
    }
}
