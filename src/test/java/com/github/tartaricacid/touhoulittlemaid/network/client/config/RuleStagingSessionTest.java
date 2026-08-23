package com.github.tartaricacid.touhoulittlemaid.network.client.config;

import com.github.tartaricacid.touhoulittlemaid.config.ServerConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MiscConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 规则暂存（{@link ServerRulesClientCache.Session}）的语义。
 *
 * <p>菜单本身要真实客户端才能构造，但暂存是纯 JSON 逻辑，可以直接单测——
 * 把「改了几项、切走再回来、点保存」这套里**可判定**的部分从人工验收搬到自动化。</p>
 *
 * <p>行为基准 {@code port/1.21.11-fabric} 的同名用例断言的是 AI 那一店的值
 * （{@code AIConfig.LLM_ENABLED} 等）。AI 店属审计 §3.C 尚未搬入，故这里改用世界规则的值——
 * **被断言的是 Session 的语义，不是具体哪个配置项**，换值不影响这些性质的有效性。</p>
 */
class RuleStagingSessionTest {
    @BeforeEach
    void initialize() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        ServerConfig.init();
        // 没有服务器快照时，会话读到的是 spec 默认值
        ServerRulesClientCache.clear();
    }

    @Test
    void aFreshSessionIsCleanAndReadsDefaults() {
        ServerRulesClientCache.Session session = ServerRulesClientCache.createSession();
        assertFalse(session.isDirty(), "没改过的暂存不得报脏，否则保存按钮上的未保存标记永远亮着");
        assertEquals(MaidConfig.MAID_WORK_RANGE.getDefault(), session.getInt(MaidConfig.MAID_WORK_RANGE));
        assertEquals(MaidConfig.MAID_CHANGE_MODEL.getDefault(), session.getBoolean(MaidConfig.MAID_CHANGE_MODEL));
        assertEquals(MiscConfig.MAID_FAIRY_POWER_POINT.getDefault(),
                session.getDouble(MiscConfig.MAID_FAIRY_POWER_POINT));
    }

    @Test
    void stagedValuesReadBackImmediatelyAndMarkDirty() {
        ServerRulesClientCache.Session session = ServerRulesClientCache.createSession();
        session.set(MaidConfig.MAID_WORK_RANGE, 33);
        session.set(MaidConfig.MAID_CHANGE_MODEL, false);

        // 本地回显：不等服务器回包就能读回来——这正是「点一下立刻可见」的机制
        assertEquals(33, session.getInt(MaidConfig.MAID_WORK_RANGE));
        assertFalse(session.getBoolean(MaidConfig.MAID_CHANGE_MODEL));
        assertTrue(session.isDirty(), "有未提交改动必须报脏，保存按钮据此点亮未保存标记");
    }

    @Test
    void separateSessionsDoNotSeeEachOthersStagedValues() {
        ServerRulesClientCache.Session first = ServerRulesClientCache.createSession();
        first.set(MaidConfig.MAID_IDLE_RANGE, 29);

        ServerRulesClientCache.Session second = ServerRulesClientCache.createSession();
        assertEquals(MaidConfig.MAID_IDLE_RANGE.getDefault(), second.getInt(MaidConfig.MAID_IDLE_RANGE),
                "两个 Session 互不可见——所以一次开菜单只能建一个，"
                        + "每个控件各建一个就会出现「改了切页回来又没了」");
        assertFalse(second.isDirty());
    }

    @Test
    void stagingTheSameValueTwiceStillCountsAsAChange() {
        ServerRulesClientCache.Session session = ServerRulesClientCache.createSession();
        session.set(MaidConfig.MAID_WORK_RANGE, MaidConfig.MAID_WORK_RANGE.getDefault());
        assertTrue(session.isDirty(),
                "写入即记账，不做「与当前值相同就不算改」的优化：那种优化会让显式重置回默认值"
                        + "变成一次无声的空操作");
    }

    /**
     * 容器对是唯一需要在「JSON 里的二元数组」与「界面上的 a,b 字符串」之间来回翻译的规则，
     * 翻译两边必须自洽——否则玩家保存一次就把这份列表清空了。
     */
    @Test
    void containerPairsSurviveTheStringRoundTrip() {
        ServerRulesClientCache.Session session = ServerRulesClientCache.createSession();
        session.setContainerPairs(MaidConfig.MAID_EATEN_RETURN_CONTAINER_LIST,
                java.util.List.of("minecraft:beetroot_soup,minecraft:bowl", "malformed_without_comma"));
        assertEquals(java.util.List.of("minecraft:beetroot_soup,minecraft:bowl"),
                session.getContainerPairs(MaidConfig.MAID_EATEN_RETURN_CONTAINER_LIST),
                "格式不对的那条应当被丢弃，而不是写成半条进配置");
        assertTrue(session.isDirty());
    }
}
