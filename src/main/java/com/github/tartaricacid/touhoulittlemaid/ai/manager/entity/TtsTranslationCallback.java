package com.github.tartaricacid.touhoulittlemaid.ai.manager.entity;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.response.ResponseChat;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMMessage;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSSite;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.net.http.HttpRequest;
import java.util.List;

/**
 * 把已经写好的回复翻译成合成语言，**在一次不带历史的独立请求里**。
 *
 * <p>取代原先「让模型在同一条回复里用 {@code ---} 分出第二段」的做法。那种做法在多轮对话下
 * 确定性失效，逐轮实测见 {@code StringConstant#TTS_TRANSLATION} 的类注释：
 * 历史里只要出现一条单段助手回复，第二段就再也不回来了。</p>
 *
 * <p><b>这个请求为什么不会漂移</b>：它的消息列表只有两条——翻译指令与待翻译文本。
 * 没有历史、没有人设、没有工具，<b>因此没有任何可以照抄的范例</b>。漂移不是被压低了概率，
 * 而是没有了发生的位置。</p>
 *
 * <p><b>失败一律降级并留声</b>：回复本身已经成功，不能因为译文拿不到就把气泡撤掉或给玩家报
 * 一个 LLM 错误。降级成用对话文本合成（有声音好过没声音），但必须写进日志——
 * 静默降级正是本仓库反复栽过的形态。</p>
 */
public class TtsTranslationCallback extends LLMCallback {
    private final TTSSite ttsSite;
    private final String chatText;

    public TtsTranslationCallback(MaidAIChatManager chatManager, List<LLMMessage> messages,
                                  TTSSite ttsSite, String chatText, long waitingChatBubbleId) {
        // subagents=true：等待气泡是主对话那一轮建的，这里不能再建一个
        super(chatManager, messages, true);
        this.ttsSite = ttsSite;
        this.chatText = chatText;
        // 沿用主对话的气泡，译文到手（或降级）时由它收尾
        this.waitingChatBubbleId = waitingChatBubbleId;
        // 翻译不需要工具，给了只是让它多一个跑偏的机会
        this.needAddTools = false;
    }

    /**
     * 译文不进上下文压缩的 token 账：压缩服务的是主对话的历史，而这条请求不在那条历史里。
     * 与 {@code HistorySummaryCallback} 同款处置。
     */
    @Override
    public boolean shouldCacheTokenUsage() {
        return false;
    }

    @Override
    public void onSuccess(ResponseChat response) {
        // 这条请求从不索取两段，所以整段响应都是译文；正文里出现的 --- 只是内容
        String translated = response.asSinglePart().getChatText();
        if (StringUtils.isBlank(translated)) {
            this.degrade("the translation came back empty");
            return;
        }
        this.chatManager.tts(this.ttsSite, this.chatText, translated, this.waitingChatBubbleId);
    }

    /**
     * <b>不调用父类</b>：父类会撤掉等待气泡并给主人弹一条 LLM 报错，而此刻回复本身是好的，
     * 玩家该看到的是女仆正常说话，不是一个错误。
     */
    @Override
    public void onFailure(@Nullable HttpRequest request, Throwable throwable, int errorCode) {
        this.degrade("error code %d: %s".formatted(errorCode, throwable.getMessage()));
    }

    private void degrade(String cause) {
        TouhouLittleMaid.LOGGER.warn(
                "TTS translation failed for maid {} ({}). Falling back to synthesizing the chat text, "
                        + "so TTS will speak {} instead of {}.",
                this.maid.getId(), cause, this.chatManager.getChatLanguage(), this.chatManager.getTTSLanguage());
        this.chatManager.tts(this.ttsSite, this.chatText, this.chatText, this.waitingChatBubbleId);
    }
}
