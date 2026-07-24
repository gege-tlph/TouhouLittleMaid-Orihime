package com.github.tartaricacid.touhoulittlemaid.ai.manager.setting.papi;

import com.github.tartaricacid.touhoulittlemaid.ai.agent.tool.implement.UseSkillTool;
import com.github.tartaricacid.touhoulittlemaid.ai.agent.tool.implement.SwitchFollowStateTool;
import com.github.tartaricacid.touhoulittlemaid.ai.agent.tool.implement.SwitchScheduleTool;
import com.github.tartaricacid.touhoulittlemaid.ai.agent.tool.implement.SwitchSitTool;
import com.github.tartaricacid.touhoulittlemaid.ai.agent.tool.implement.SwitchWorkTaskTool;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptKnowledgeContractTest {
    private static final String SKILL_ROOT = "data/touhou_little_maid/skills/touhou_little_maid/";

    @Test
    void fullSettingRendersCurrentEnvironmentAndInjectedValues() {
        String rendered = PapiReplacer.renderFullSetting(Map.of(
                "main_setting", "TEST CHARACTER",
                "owner_name", "TEST OWNER",
                "available_skills", "TEST SKILL SUMMARY"
        ));

        assertTrue(rendered.contains("TEST CHARACTER"));
        assertTrue(rendered.contains("TEST OWNER"));
        assertTrue(rendered.contains("TEST SKILL SUMMARY"));
        assertTrue(rendered.contains("Version: 1.21.11"));
        assertFalse(rendered.contains("${"));
    }

    @Test
    void promptRequiresUniqueLegalTargetsAndHonestOutcomes() {
        String prompt = normalizeWhitespace(StringConstant.FULL_SETTING);

        assertTrue(prompt.contains("exactly one uniquely identified legal target"));
        assertTrue(prompt.contains("Tool and server validation are final"));
        assertTrue(prompt.contains("succeeded, failed, or was not executed"));
        assertTrue(prompt.contains("Knowledge skills do not replace direct-state tools or live context queries"));
        assertTrue(prompt.contains("`active_activity`, `response_policy`, `emergency_state`"));
        assertTrue(prompt.contains("protect_owner` applies only while the owner is loaded nearby"));
        assertTrue(prompt.contains("Do not call `switch_work_task` merely to simulate one-off self-defense"));
        assertTrue(prompt.contains("stops any current temporary threat response"));
        assertTrue(prompt.contains("clears pending owner-intent evidence"));
        assertFalse(prompt.contains("Zero Tool Reporting"));
        assertFalse(prompt.contains("Can you kill that pig?"));
        assertFalse(prompt.contains("Version: 1.20.1"));
    }

    @Test
    void directControlToolSchemasDescribeTemporaryResponseCancellation() {
        String expected = "stops any current temporary threat response";

        assertTrue(new SwitchFollowStateTool().summary(null).contains(expected));
        assertTrue(new SwitchSitTool().summary(null).contains(expected));
        assertTrue(new SwitchScheduleTool().summary(null).contains(expected));
        assertTrue(new SwitchWorkTaskTool().summary(null).contains(expected));
    }

    @Test
    void builtInKnowledgeDescribesOnlyImplementedCrossLayerFacts() throws IOException {
        String skill = readResource(SKILL_ROOT + "skill.md");
        String chinese = readResource(SKILL_ROOT + "references/zh_cn.md");
        String english = readResource(SKILL_ROOT + "references/en_us.md");
        String normalizedEnglish = normalizeWhitespace(english);

        assertTrue(skill.contains("reference knowledge, not authoritative live state"));
        assertTrue(chinese.contains("跟随主人是兜底行为"));
        assertTrue(chinese.contains("单层浅水"));
        assertTrue(chinese.contains("有主的实体不会成为攻击目标"));
        assertTrue(chinese.contains("默认是保护主人"));
        assertTrue(chinese.contains("两次彼此独立的成功直接攻击"));
        assertTrue(chinese.contains("100 游戏刻"));
        assertTrue(chinese.contains("主人直接发射的弹射物"));
        assertTrue(chinese.contains("工具、普通物品、远程武器或空手"));
        assertTrue(chinese.contains("伤害、攻击速度、附魔和耐久仍按原版持有物品规则计算"));
        assertTrue(chinese.contains("不会自动从背包换取武器"));
        assertTrue(chinese.contains("远程任务仍保留各自的武器和弹药条件"));
        assertTrue(chinese.contains("只用于玩法知识问答"));
        assertTrue(normalizedEnglish.contains("Owner following is a fallback behavior"));
        assertTrue(normalizedEnglish.contains("single shallow water layer"));
        assertTrue(normalizedEnglish.contains("Owned entities cannot become targets"));
        assertTrue(normalizedEnglish.contains("Protect Owner is the current default"));
        assertTrue(normalizedEnglish.contains("changed in the maid configuration screen"));
        assertTrue(normalizedEnglish.contains("after a client reconnect or dedicated-server restart"));
        assertTrue(normalizedEnglish.contains("two separate successful direct attacks"));
        assertTrue(normalizedEnglish.contains("100 game ticks"));
        assertTrue(normalizedEnglish.contains("projectile fired directly by the owner"));
        assertTrue(normalizedEnglish.contains("tool, ordinary item, projectile weapon, or empty hand"));
        assertTrue(normalizedEnglish.contains("Damage, attack speed, enchantments, and durability follow the vanilla held-item rules"));
        assertTrue(normalizedEnglish.contains("no weapon is swapped in from inventory automatically"));
        assertTrue(normalizedEnglish.contains("retain their own weapon and ammunition requirements"));
        assertTrue(normalizedEnglish.contains("only for gameplay knowledge"));

        String englishTranslations = readResource("assets/touhou_little_maid/lang/en_us.json");
        String chineseTranslations = readResource("assets/touhou_little_maid/lang/zh_cn.json");
        for (String translations : new String[]{englishTranslations, chineseTranslations}) {
            assertTrue(translations.contains("\"gui.touhou_little_maid.maid_config.response_policy\""));
            assertTrue(translations.contains("\"gui.touhou_little_maid.maid_config.response_policy.tooltip\""));
            assertTrue(translations.contains("\"gui.touhou_little_maid.maid_config.response_policy.value.off\""));
            assertTrue(translations.contains("\"gui.touhou_little_maid.maid_config.response_policy.value.self_defense\""));
            assertTrue(translations.contains("\"gui.touhou_little_maid.maid_config.response_policy.value.protect_owner\""));
        }

        String allKnowledge = (skill + chinese + english).toLowerCase();
        assertFalse(allKnowledge.contains("emergency combat"));
        assertFalse(allKnowledge.contains("smart combat"));
        assertFalse(allKnowledge.contains("table food"));
    }

    @Test
    void skillInvocationSummaryIsLocalizedWithoutInternalIdentifiers() throws IOException {
        String fallback = new UseSkillTool().invocationSummary("internal-skill-name");
        assertEquals("Consulting maid gameplay knowledge", fallback);
        assertFalse(fallback.contains("use_skill"));
        assertFalse(fallback.contains("internal-skill-name"));

        for (String language : new String[]{"en_us", "zh_cn", "ru_ru", "vi_vn"}) {
            String translations = readResource("assets/touhou_little_maid/lang/" + language + ".json");
            assertTrue(translations.contains("\"ai.touhou_little_maid.chat.tool_call.use_skill\""));
        }
    }

    private static String readResource(String path) throws IOException {
        try (InputStream stream = PromptKnowledgeContractTest.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Missing test resource: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String normalizeWhitespace(String value) {
        return value.replaceAll("\\s+", " ").trim();
    }
}
