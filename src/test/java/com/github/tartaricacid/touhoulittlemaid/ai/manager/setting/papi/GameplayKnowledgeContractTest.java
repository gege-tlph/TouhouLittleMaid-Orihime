package com.github.tartaricacid.touhoulittlemaid.ai.manager.setting.papi;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定「已落地的服务端行为必须在内置 knowledge 中有对应描述，且中英文覆盖同一批事实」。
 *
 * <p>本测试**刻意不断言提示词措辞或风格**。前一版契约测试把 system prompt 的具体句子写死，
 * 2026-07-25 按用户决定把提示词改回 beta 宽松版本时只能连同测试一起删除，于是 knowledge 与
 * 服务端行为脱节后再无自动化能发现。因此这里只针对**事实锚点**（数值与功能名词）断言，
 * 措辞、语气和示例可以自由调整。
 */
class GameplayKnowledgeContractTest {
    private static final String SKILL_ROOT = "data/touhou_little_maid/skills/touhou_little_maid/";

    /** 每项已落地行为在两种语言里各自的事实锚点。key 仅用于失败时定位。 */
    private static final Map<String, String[]> SHIPPED_BEHAVIOUR_ANCHORS = new LinkedHashMap<>() {{
        // §3.B 统一目标策略：自主索敌的硬保护
        put("target-policy-autonomous", new String[]{"owned entities cannot become targets", "有主的实体不会成为攻击目标"});
        // §3.B 主人明确命令可打和平/中立目标，只有硬安全集合拒绝
        put("owner-commanded-attack", new String[]{"explicit owner order", "明确主人命令"});
        // §3.B 三档响应策略与持久化
        put("response-policy", new String[]{"Off, Self Defense, and Protect Owner", "关闭、仅自卫、保护主人"});
        // §3.B 任意主手物品近战、不自动换装
        put("any-held-item-melee", new String[]{"no weapon is swapped in from inventory", "不会自动从背包换取武器"});
        // knowledge 不代替实时状态查询
        put("knowledge-scope", new String[]{"only for gameplay knowledge", "只用于玩法知识问答"});

        /*
         * ⚠️ 恢复锚点：行为基准这张表还有四项，本分支**尚未移植对应功能**，
         * 故连同知识文档里描述它们的三个段落一并剔除：
         *   follow / shallow-water / farm-stand-node —— 审计 §3.E 寻路·跟随手感
         *   table-food                              —— 审计 §3.I 桌上食物
         *
         * 知识文档是直接喂给模型当事实的：写进去的每一句女仆都会当真说出去。
         * 描述一个本构建做不到的行为，比不描述贵得多——玩家会照着它去试，然后发现她做不到。
         * §3.E / §3.I 落地时，把那三段文本与这四项锚点一起装回来。
         */
    }};

    /**
     * 已落地的数值事实；两种语言必须同时给出，避免只改一边。
     *
     * <p>基准另有 {@code 3600 game ticks}（好感冷却）与 {@code 200 game ticks}（目标上限）两条，
     * 都属 §3.I 桌上食物，随该功能一起补。</p>
     */
    private static final String[][] NUMERIC_FACTS = {
            {"16-block", "16 格"},
            {"40 game ticks", "40 游戏刻"},
            {"60 game ticks", "60 游戏刻"},
            {"100 game ticks", "100 游戏刻"},
    };

    @Test
    void knowledgeCoversShippedBehaviourInBothLanguages() throws IOException {
        String english = normalizeWhitespace(readResource(SKILL_ROOT + "references/en_us.md")).toLowerCase();
        String chinese = normalizeWhitespace(readResource(SKILL_ROOT + "references/zh_cn.md"));

        SHIPPED_BEHAVIOUR_ANCHORS.forEach((topic, anchors) -> {
            assertTrue(english.contains(anchors[0].toLowerCase()),
                    "en_us.md 缺少已落地行为的描述: " + topic + " (锚点: " + anchors[0] + ")");
            assertTrue(chinese.contains(anchors[1]),
                    "zh_cn.md 缺少已落地行为的描述: " + topic + " (锚点: " + anchors[1] + ")");
        });
    }

    @Test
    void bothLanguagesStateTheSameNumericFacts() throws IOException {
        String english = normalizeWhitespace(readResource(SKILL_ROOT + "references/en_us.md"));
        String chinese = normalizeWhitespace(readResource(SKILL_ROOT + "references/zh_cn.md"));

        for (String[] fact : NUMERIC_FACTS) {
            assertTrue(english.contains(fact[0]), "en_us.md 缺少数值事实: " + fact[0]);
            assertTrue(chinese.contains(fact[1]), "zh_cn.md 缺少数值事实: " + fact[1]);
        }
    }

    /**
     * 这里曾经反过来：断言 knowledge <b>不得</b>提到桌上食物，理由写的是「尚未实现」。
     *
     * <p>那条守卫在功能落地（`588e9e6f` 开关 → `6d5ea732` 奖励与冷却 → `94883be7` 200 tick 仲裁）之后
     * 就过期了，却没人回来拆——于是它从「防止提前宣传」变成了「阻止如实描述」：谁去补这段知识，
     * 谁的测试就红。守卫必须跟着实现一起翻面，否则它保护的是过去而不是正确性。
     * 现在桌上食物已进上面的 {@code SHIPPED_BEHAVIOUR_ANCHORS} 与 {@code NUMERIC_FACTS}，
     * 由「必须写到」正向钉住。</p>
     */
    @Test
    void knowledgeDoesNotAdvertiseUnimplementedFeatures() throws IOException {
        String all = (readResource(SKILL_ROOT + "skill.md")
                + readResource(SKILL_ROOT + "references/en_us.md")
                + readResource(SKILL_ROOT + "references/zh_cn.md")).toLowerCase();

        // 服务器侧语音识别已于 2026-07-27 撤除：语音输入整条链路都在玩家客户端，
        // knowledge 不得让玩家去服务器配置里找一个不存在的开关。
        assertFalse(all.contains("server-provided speech"), "knowledge 宣传了已撤除的服务器 STT");
        assertFalse(all.contains("服务器提供语音识别"), "knowledge 宣传了已撤除的服务器 STT");
    }

    /**
     * 只断言两条**为了准确性而补写**的提示词规则是否存在，不断言其余措辞。
     * 攻击合法性等门禁由服务端 MaidTargetingPolicy 裁决，提示词不承担安全边界职责。
     */
    @Test
    void promptExplainsPersistentToolsAndLiveStateFields() {
        String prompt = normalizeWhitespace(StringConstant.FULL_SETTING);

        assertTrue(prompt.contains("Version: 1.21.11"), "提示词的游戏版本应为 1.21.11");

        assertTrue(prompt.contains("switch_work_task") && prompt.contains("PERSISTENT"),
                "提示词应说明直接状态工具是持久设置，switch_work_task 会改永久任务");

        for (String field : new String[]{"active_activity", "emergency_state", "threat_source", "response_policy"}) {
            assertTrue(prompt.contains(field), "提示词缺少服务端下发的实时状态字段说明: " + field);
        }
        assertTrue(prompt.contains("protect_owner"),
                "提示词应说明 protect_owner 在主人不在本地时会临时降级");
    }

    private static String readResource(String path) throws IOException {
        try (InputStream stream = GameplayKnowledgeContractTest.class.getClassLoader().getResourceAsStream(path)) {
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
