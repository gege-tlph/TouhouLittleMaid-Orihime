package com.github.tartaricacid.touhoulittlemaid.ai.manager.response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 两段式回复的拆分边界。
 *
 * <p>旧实现用 {@code split("---", 2)} 匹配任意位置，又用 {@code replaceAll("---", "")} 抹掉正文里的
 * 三连字符：女仆回复里只要出现 markdown 分隔线，或玩家让她复述一段带横杠的文本，回复就会在那里被
 * 腰斩，后半段还会被当成 TTS 文本念出去。现在分隔符必须独占一行，与提示词里写的
 * "a line containing only ---" 一致。</p>
 *
 * <p>行内兜底不能一并删掉：旧存档的历史里存的就是 {@code chat---tts} 这种没有换行的整串，
 * 个别模型也会无视格式要求把分隔符写在行内。</p>
 */
class ResponseChatSplitTest {
    @Test
    void splitsOnADelimiterLine() {
        ResponseChat response = new ResponseChat("主人好呀。\n---\nHello Master.");

        assertEquals("主人好呀。", response.getChatText());
        assertEquals("Hello Master.", response.getTtsText());
    }

    @Test
    void tripleDashesInsideALineAreContentNotADelimiter() {
        ResponseChat response = new ResponseChat("用 --- 分隔的写法我不喜欢。\n---\nI dislike the --- style.");

        assertEquals("用 --- 分隔的写法我不喜欢。", response.getChatText(),
                "正文里的三连字符既不能触发拆分，也不能被抹掉");
        assertEquals("I dislike the --- style.", response.getTtsText());
    }

    @Test
    void fallsBackToTheInlineDelimiterForLegacyAndNonCompliantOutput() {
        ResponseChat response = new ResponseChat("主人好呀。---Hello Master.");

        assertEquals("主人好呀。", response.getChatText(),
                "旧存档历史与不守格式的模型都会把分隔符写在行内，没有独占一行的分隔符时必须兜底");
        assertEquals("Hello Master.", response.getTtsText());
    }

    @Test
    void aBlankSecondPartFallsBackToTheChatText() {
        ResponseChat response = new ResponseChat("主人好呀。\n---\n   ");

        assertEquals("主人好呀。", response.getChatText());
        assertEquals("主人好呀。", response.getTtsText(), "第二段为空时不能让 TTS 拿到空串");
    }

    @Test
    void aSinglePartResponseKeepsEveryDashItContains() {
        ResponseChat response = new ResponseChat("这段里有 --- 也有\n---\n换行的分隔线").asSinglePart();

        String whole = "这段里有 --- 也有\n---\n换行的分隔线";
        assertEquals(whole, response.getChatText(),
                "没索取第二段时整段都是对话文本，任何形态的 --- 都只是内容");
        assertEquals(whole, response.getTtsText());
    }

    @Test
    void asSinglePartIsSafeOnAnAlreadySplitResponse() {
        ResponseChat response = new ResponseChat("主人好呀。", "Hello Master.").asSinglePart();

        assertEquals("主人好呀。", response.getChatText(),
                "两参构造出来的响应没有原始整串，退化成只保留对话文本即可，不得抛异常");
        assertEquals("主人好呀。", response.getTtsText());
    }
}
