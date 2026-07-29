package com.github.tartaricacid.touhoulittlemaid.network.client.config;

import com.github.tartaricacid.touhoulittlemaid.config.ServerConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import com.github.tartaricacid.touhoulittlemaid.config.AiServerRuleConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 规则暂存（{@link ServerRulesClientCache.Session}）的语义，即设置枢纽「改了没保存」那套的内核。
 *
 * <p>验收矩阵里「带 * 快速来回切四页，改动不丢」那一格靠的就是这些性质。屏本身要真实客户端才能
 * 构造，但暂存是纯 JSON 逻辑，可以直接单测——把那一格里**可判定**的部分从人工搬到自动化。</p>
 *
 * <p>历史病灶：每个控件曾各自 {@code createSession()} 并立即 {@code save()}，于是界面 init 重读的是
 * 还没等到服务器回包的旧缓存（「点两遍才切换」），改动还绕过保存按钮直接生效。修法是每屏一个长命
 * Session、并把它挂到跨标签页共享的 SharedState 上——下面第三条正是「各 Session 互相隔离」，
 * 它同时说明了为什么必须共享同一个而不是各建一个。</p>
 */
class RuleStagingSessionTest {
    @BeforeEach
    void initialize() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        ServerConfig.init();
        AiServerRuleConfig.init();
        // 没有服务器快照时，会话读到的是 spec 默认值
        ServerRulesClientCache.clear();
    }

    @Test
    void aFreshSessionIsCleanAndReadsDefaults() {
        ServerRulesClientCache.Session session = ServerRulesClientCache.createSession();
        assertFalse(session.isDirty(), "没改过的暂存不得报脏，否则保存按钮上的 * 永远亮着");
        assertEquals(AIConfig.LLM_ENABLED.getDefault(), session.getBoolean(AIConfig.LLM_ENABLED));
        assertEquals(AIConfig.DEFAULT_LLM_SITE.getDefault(), session.getString(AIConfig.DEFAULT_LLM_SITE));
    }

    @Test
    void stagedValuesReadBackImmediatelyAndMarkDirty() {
        ServerRulesClientCache.Session session = ServerRulesClientCache.createSession();
        session.set(AIConfig.DEFAULT_LLM_SITE, "acme");
        session.set(AIConfig.LLM_ENABLED, false);

        // 本地回显：不等服务器回包就能读回来——这正是「点一下立刻可见」的机制
        assertEquals("acme", session.getString(AIConfig.DEFAULT_LLM_SITE));
        assertFalse(session.getBoolean(AIConfig.LLM_ENABLED));
        assertTrue(session.isDirty(), "有未提交改动必须报脏，保存按钮据此点亮 *");
    }

    @Test
    void separateSessionsDoNotSeeEachOthersStagedValues() {
        ServerRulesClientCache.Session first = ServerRulesClientCache.createSession();
        first.set(AIConfig.DEFAULT_TTS_SITE, "minimax");

        ServerRulesClientCache.Session second = ServerRulesClientCache.createSession();
        assertEquals(AIConfig.DEFAULT_TTS_SITE.getDefault(), second.getString(AIConfig.DEFAULT_TTS_SITE),
                "两个 Session 互不可见——所以四个标签页必须共享同一个（挂在 SharedState 上），"
                        + "各建一个就会出现「切页后改动看不见了」");
        assertFalse(second.isDirty());
    }

    @Test
    void stagingTheSameValueTwiceStillCountsAsAChange() {
        ServerRulesClientCache.Session session = ServerRulesClientCache.createSession();
        session.set(AIConfig.DEFAULT_LLM_SITE, AIConfig.DEFAULT_LLM_SITE.getDefault());
        assertTrue(session.isDirty(),
                "写入即记账，不做「与当前值相同就不算改」的优化：那种优化会让显式重置回默认值"
                        + "变成一次无声的空操作。界面侧由调用方在冲洗输入框时自行比较。");
    }
}
