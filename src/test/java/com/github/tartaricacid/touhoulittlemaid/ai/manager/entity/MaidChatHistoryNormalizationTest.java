package com.github.tartaricacid.touhoulittlemaid.ai.manager.entity;

import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMMessage;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 助手回复的 TTS 半段不得进入对话状态。
 *
 * <p>{@code ttsText} 是拿去合成语音的翻译，对后续对话零信息量。旧实现把 {@code chat---tts}
 * 整串存进历史，于是模型每轮都能看到十余个「上一次用的是哪种语言」的范例——玩家把 TTS 语言
 * 改成日语后，历史里的英语范例反而胜出，实测清空聊天记录即恢复。</p>
 *
 * <p>现在只存对话文本，但**已有存档里仍是整串**，那些女仆无法靠「以后不再写脏数据」自愈，
 * 故回放时归一化。本用例钉住的就是这条归一化。</p>
 */
class MaidChatHistoryNormalizationTest {
    @Test
    void legacyTwoPartAssistantHistoryLosesItsTtsHalf() {
        LLMMessage legacy = new LLMMessage(Role.ASSISTANT,
                "主人好呀，正要准备晚饭呢~哟。---Hello Master, was just about to prepare dinner~.",
                123L, null, null);

        LLMMessage normalized = MaidAIChatManager.withoutTtsHalf(legacy);

        assertEquals("主人好呀，正要准备晚饭呢~哟。", normalized.message(),
                "旧档里的 TTS 半段必须在回放时剥掉，否则它会继续教模型用过期的语言");
        assertEquals(Role.ASSISTANT, normalized.role());
        assertEquals(123L, normalized.gameTime(), "归一化不得丢失其它字段");
    }

    @Test
    void modernSingleParkAssistantHistoryIsUntouched() {
        LLMMessage modern = new LLMMessage(Role.ASSISTANT, "主人好呀。", 1L, null, null);
        assertSame(modern, MaidAIChatManager.withoutTtsHalf(modern),
                "新格式没有分隔符，应原样返回而不是重建对象");
    }

    @Test
    void userMessagesAreNeverRewrittenEvenIfTheyContainTheDelimiter() {
        LLMMessage user = new LLMMessage(Role.USER, "帮我把这段 a---b 念一遍", 2L, null, null);
        assertSame(user, MaidAIChatManager.withoutTtsHalf(user),
                "只有助手回复才有 TTS 半段；玩家原话里出现分隔符纯属巧合，不得改写");
    }

    @Test
    void blankAssistantMessageWithToolCallsSurvives() {
        LLMMessage toolTurn = new LLMMessage(Role.ASSISTANT, "", 3L, null, null);
        assertSame(toolTurn, MaidAIChatManager.withoutTtsHalf(toolTurn),
                "工具调用轮的消息体为空，不能在归一化时被误处理");
    }
}
