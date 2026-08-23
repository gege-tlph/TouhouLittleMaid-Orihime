package com.github.tartaricacid.touhoulittlemaid.ai.manager.setting.papi;

import com.github.tartaricacid.touhoulittlemaid.ai.agent.tool.implement.QueryGameContextTool;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

        // §3.I 桌上食物（2026-08-19 落地，本行随之补回）
        put("table-food", new String[]{"table food", "桌上食物"});

        /*
         * ⚠️ 恢复锚点（剩余三项）：行为基准这张表还有 follow / shallow-water / farm-stand-node，
         * 属审计 §3.E 寻路·跟随手感。
         *
         * ⚠️ **这三项的延后理由已经过期**：§3.E 本身早在 2026-08-17 就落地并实机验收过了
         * （平滑跟随、浅水泳姿、农场站位收割三项都在那批 11 项里），只是知识文档那三段
         * 一直没人回来装。这正是本仓库反复栽的 R7 形态——延后注释里的障碍早已消除，
         * 却因为没人回收而让一个功能白白「对模型不存在」。
         *
         * 之所以本轮**仍不补**：知识文档是直接喂给模型当事实的，写进去的每一句女仆都会
         * 当真说出去，所以每一句都得对着实现逐条核过才能写。桌上食物那段是本轮亲手实现的，
         * 五项断言（默认开、随档保存、1~3 点、3600 刻冷却、200 刻让位）都当场核对过才敢装；
         * 这三项没核，就不写——**宁可少说，不可说错**。
         */
    }};

    /**
     * 已落地的数值事实；两种语言必须同时给出，避免只改一边。
     *
     * <p>{@code 3600 game ticks}（好感冷却）与 {@code 200 game ticks}（目标上限）属 §3.I 桌上食物，
     * 已随该功能于 2026-08-19 补回。两个数都对着实现核过：冷却取
     * {@code Type.STEAL_EDIBLE_BLOCK} 的 3*60*20，让位上限取
     * {@code MaidStealEdibleMoveBlockTask.MAX_TARGET_HOLD_TICKS}。</p>
     */
    private static final String[][] NUMERIC_FACTS = {
            {"16-block", "16 格"},
            {"40 game ticks", "40 游戏刻"},
            {"60 game ticks", "60 游戏刻"},
            {"100 game ticks", "100 游戏刻"},
            {"3600 game ticks", "3600 游戏刻"},
            {"200 game ticks", "200 游戏刻"},
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
    void promptExplainsPersistentToolsAndLiveStateFields() throws IOException {
        String prompt = normalizeWhitespace(StringConstant.FULL_SETTING);

        // 版本号不写死字面量：从 gradle.properties 的 minecraft_version 读，随版本自动跟走。
        // 写死的代价本轮亲历——从行为基准照搬来的 "1.21.11" 在 26.1.2 上是错的，
        // 而断言同样照搬，于是它绿着把一句错事实喂给了模型。
        String mcVersion = gradleProperty("minecraft_version");
        assertTrue(prompt.contains("Version: " + mcVersion),
                "提示词的游戏版本应为 " + mcVersion + "，实际提示词未包含它");

        assertTrue(prompt.contains("switch_work_task") && prompt.contains("PERSISTENT"),
                "提示词应说明直接状态工具是持久设置，switch_work_task 会改永久任务");

        for (String field : new String[]{"active_activity", "emergency_state", "threat_source", "response_policy"}) {
            assertTrue(prompt.contains(field), "提示词缺少服务端下发的实时状态字段说明: " + field);
        }
        assertTrue(prompt.contains("protect_owner"),
                "提示词应说明 protect_owner 在主人不在本地时会临时降级");
    }

    /**
     * skill.md 是 UseSkillTool 交给模型的那份技能说明的抬头，负责界定「这份知识管什么、不管什么」。
     *
     * <p>本树此前与代码宿主 origin/26.1 逐字相同，缺的正是安全范围那一段。没有它，模型会把这份
     * 静态知识当成实时状态去回答「你现在拿着什么」「附近有什么」，而正确做法是去调工具。行为基准
     * port/1.21.11-fabric 的 f8e900d93 / 195600c42 早已补上，本树 2026-08-20 补回。</p>
     *
     * <p>判据刻意<b>不取自 skill.md 自身</b>：工具名从 {@link QueryGameContextTool#TOOL_ID} 读，
     * 工具改名时这条会红而不是跟着一起漂。契约有两个动词，两个都断言——
     * ① 声明自己不是权威实时状态 ② 指出实时状态该去问谁；只钉前者时后者可以整句消失而测试全绿。</p>
     */
    @Test
    void skillHeaderScopesItselfAsReferenceKnowledgeAndPointsAtTheLiveStateTool() throws IOException {
        String skill = normalizeWhitespace(readResource(SKILL_ROOT + "skill.md"));

        // 活性断言，与上面两条结论正交：没有它，「资源没读到」与「读到了但没写这段」输出完全一样
        assertTrue(skill.length() > 400, "skill.md 内容过短，疑似没读到: " + skill.length() + " 字符");

        // 动词一：声明这是参考知识，不是权威实时状态
        assertTrue(skill.contains("not authoritative live state"),
                "skill.md 缺少「这是参考知识、不是实时状态」的范围声明");

        // 动词二：指出实时状态该去调哪个工具。工具名来自实现侧常量，不是本文件里抄一遍
        assertTrue(skill.contains("`" + QueryGameContextTool.TOOL_ID + "`"),
                "skill.md 未指向实时状态查询工具 " + QueryGameContextTool.TOOL_ID);
    }
    /**
     * 从项目根的 {@code gradle.properties} 取一个键。
     *
     * <p>{@code ..\..} 是本仓库既有约定：test 任务的 workingDir 被设为
     * {@code build/test-working}（见 build.gradle），故上溯两级即项目根，
     * 与 ToolDispatchWiringContractTest 等既有契约测试同款。</p>
     */
    private static String gradleProperty(String key) throws IOException {
        Path properties = Path.of("..", "..", "gradle.properties");
        for (String line : Files.readAllLines(properties, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (trimmed.startsWith(key + "=")) {
                return trimmed.substring(key.length() + 1).trim();
            }
        }
        throw new IOException("gradle.properties 缺少键: " + key);
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
