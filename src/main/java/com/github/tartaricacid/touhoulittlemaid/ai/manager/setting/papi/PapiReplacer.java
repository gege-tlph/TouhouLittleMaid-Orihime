package com.github.tartaricacid.touhoulittlemaid.ai.manager.setting.papi;

import com.github.tartaricacid.touhoulittlemaid.ai.agent.skill.SkillLoader;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.Maps;
import net.minecraft.util.Util;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;

import java.util.Locale;
import java.util.Map;

import static com.github.tartaricacid.touhoulittlemaid.ai.manager.setting.papi.StringConstant.*;

/**
 * 因为现在的大语言模型基本都有多语言支持，故直接用英文写设定文件
 * <p>
 * 此类仅负责部分固定的提示词中的关键词替换与语言格式化
 * <p>
 * 女仆实时上下文的注册与取值由 context skill 系统负责
 */
public class PapiReplacer {
    private PapiReplacer() {
    }

    /**
     * 基础设定提示词的关键字替换
     */
    public static String replaceSetting(String input, EntityMaid maid, String language) {
        Map<String, String> valueMap = valueMap(input, maid, language);
        return renderFullSetting(valueMap) + outputFormat(valueMap);
    }

    /**
     * 追加在历史记录之后的权威要求。
     *
     * <p>与 {@link #replaceSetting} 里那份是同一段格式要求，<b>差别只在位置</b>：这一份紧贴生成位置，
     * 用来压过历史里的旧范例。成因与实测见 {@link StringConstant#HISTORY_IS_NOT_INSTRUCTION}。</p>
     */
    public static String trailingRequirements(EntityMaid maid, String language) {
        Map<String, String> valueMap = valueMap(StringUtils.EMPTY, maid, language);
        return new StrSubstitutor(valueMap).replace(HISTORY_IS_NOT_INSTRUCTION)
                + outputFormat(valueMap);
    }

    /**
     * 待合成文本的翻译请求的系统提示词，见 {@link StringConstant#TTS_TRANSLATION}。
     *
     * <p>只替换 {@code tts_language} 一个变量：这条请求里不该出现人设、主人名或技能表——
     * 任何多给的上下文都是一次让它跑偏的机会，而它要做的只是翻译一句话。</p>
     */
    public static String ttsTranslationPrompt(EntityMaid maid) {
        Map<String, String> valueMap = Maps.newHashMap();
        valueMap.put("tts_language", getTtsLanguage(maid));
        return new StrSubstitutor(valueMap).replace(StringConstant.TTS_TRANSLATION);
    }

    private static Map<String, String> valueMap(String input, EntityMaid maid, String language) {
        return Util.make(Maps.newHashMap(), map -> {
            map.put("main_setting", input);
            map.put("owner_name", getOwnerName(maid));
            map.put("chat_language", getChatLanguage(language));
            map.put("tts_language", getTtsLanguage(maid));
            map.put("available_skills", SkillLoader.getSkillSummary());
        });
    }

    /**
     * 主对话**永远只索取一段**。
     *
     * <p>原先在「合成语言与聊天语言不同」时改用两段模板，让模型在同一条回复里用 {@code ---}
     * 分出译文。逐轮实测（见 {@link StringConstant#TTS_TRANSLATION}）表明那在多轮下确定性失效：
     * 助手历史只存对话文本，于是上下文里只剩单段反例、没有一条两段正例，而示范胜过指令。</p>
     *
     * <p>改成恒定单段之后，<b>提示词要求的形状与历史里实际出现的形状一致</b>——没有可漂移的方向。
     * 译文改由一次不带历史的翻译请求产出，见 {@code MaidAIChatManager#requestTtsTranslation}。</p>
     *
     */
    private static String outputFormat(Map<String, String> valueMap) {
        return new StrSubstitutor(valueMap).replace(OUTPUT_FORMAT_REQUIREMENTS_SINGLE);
    }

    static String renderFullSetting(Map<String, String> valueMap) {
        return new StrSubstitutor(valueMap).replace(FULL_SETTING);
    }

    public static String getChatLanguage(String languageTag) {
        return language(languageTag);
    }

    public static String getTtsLanguage(EntityMaid maid) {
        return language(maid.getAiChatManager().getTTSLanguage());
    }

    public static String getOwnerName(EntityMaid maid) {
        String ownerName = maid.getAiChatManager().ownerName;
        if (StringUtils.isBlank(ownerName)) {
            return DEFAULT_OWNER_NAME;
        }
        return ownerName;
    }

    /**
     * 不能调用 LanguageManager，那个是客户端方法
     */
    private static String language(String languageTag) {
        String[] parts = languageTag.split("_");
        if (parts.length == 2) {
            languageTag = parts[0] + "-" + parts[1].toUpperCase(Locale.ENGLISH);
        }
        Locale locale = Locale.forLanguageTag(languageTag);
        return LANGUAGE_FORMAT.formatted(locale.getDisplayLanguage(), locale.getDisplayCountry());
    }

}
