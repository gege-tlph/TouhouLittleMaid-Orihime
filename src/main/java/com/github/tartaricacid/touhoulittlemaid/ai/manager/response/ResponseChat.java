package com.github.tartaricacid.touhoulittlemaid.ai.manager.response;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * 一次 LLM 文字回复的解析结果，拆成给玩家看的 {@link #chatText} 与拿去合成语音的 {@link #ttsText}。
 *
 * <p>只有当 TTS 确实会被调用、且合成语言与聊天语言不同时才会索取第二段，判定见
 * {@code MaidAIChatManager#needsSeparateTtsText}。其余情况下整段响应都是对话文本，
 * 由 {@link #asSinglePart()} 表达。</p>
 */
public class ResponseChat {
    /**
     * 分隔符必须独占一行，与提示词里写的 "a line containing only ---" 一致。
     *
     * <p>旧实现用 {@code split("---", 2)} 匹配任意位置，又用 {@code replaceAll("---", "")}
     * 抹掉正文里的三连字符：女仆回复里只要出现 markdown 分隔线，或玩家让她复述一段带横杠的文本，
     * 回复就会在那里被腰斩，后半段还会被当成 TTS 文本念出去。</p>
     */
    private static final Pattern DELIMITER_LINE = Pattern.compile("(?m)^[ \\t]*---[ \\t]*$");
    /**
     * 行内兜底，服务两处：旧存档历史里存的是 {@code chat---tts} 这种没有换行的整串，
     * 个别模型也会无视格式要求把分隔符写在行内。仅在找不到独占一行的分隔符时才启用。
     */
    private static final String INLINE_DELIMITER = "---";

    /**
     * 未经拆分的原始响应，{@link #asSinglePart()} 需要它还原整段文本
     */
    private final String raw;
    public String chatText;
    public String ttsText;

    public ResponseChat(String input) {
        this.raw = StringUtils.defaultString(input);
        String trimmed = this.raw.trim();
        String[] split = DELIMITER_LINE.split(trimmed, 2);
        if (split.length < 2 && trimmed.contains(INLINE_DELIMITER)) {
            split = new String[]{
                    StringUtils.substringBefore(trimmed, INLINE_DELIMITER),
                    StringUtils.substringAfter(trimmed, INLINE_DELIMITER)
            };
        }
        this.chatText = split[0].trim();
        this.ttsText = split.length > 1 ? split[1].trim() : this.chatText;
        if (StringUtils.isBlank(this.ttsText)) {
            this.ttsText = this.chatText;
        }
    }

    public ResponseChat(String chatText, String ttsText) {
        this.raw = StringUtils.defaultString(chatText);
        this.chatText = chatText;
        this.ttsText = ttsText;
    }

    /**
     * 按「整段都是对话文本」重新解释这次响应，供没有索取第二段的那些请求使用。
     *
     * <p>此时正文里出现的 {@code ---} 只是内容，不是分隔符，拆分反而会截断回复。</p>
     */
    public ResponseChat asSinglePart() {
        String text = this.raw.trim();
        return new ResponseChat(text, text);
    }

    public String getChatText() {
        return chatText;
    }

    public String getTtsText() {
        return ttsText;
    }

    @Override
    public String toString() {
        return "%s---%s".formatted(chatText, ttsText);
    }
}
