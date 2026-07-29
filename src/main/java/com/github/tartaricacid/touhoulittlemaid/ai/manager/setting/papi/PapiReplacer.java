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
        return renderFullSetting(valueMap) + outputFormat(valueMap, maid, language);
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
                + outputFormat(valueMap, maid, language);
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
     * 只有当第二段真的会被拿去合成、且与第一段内容不同时才索取它，否则模型在为一份被丢弃的
     * 或逐字重复的副本付输出 token。判定见 {@code MaidAIChatManager#needsSeparateTtsText}。
     */
    private static String outputFormat(Map<String, String> valueMap, EntityMaid maid, String language) {
        String template = maid.getAiChatManager().needsSeparateTtsText(language)
                ? OUTPUT_FORMAT_REQUIREMENTS_DIFFERENT_LANGUAGES
                : OUTPUT_FORMAT_REQUIREMENTS_SINGLE;
        return new StrSubstitutor(valueMap).replace(template);
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
