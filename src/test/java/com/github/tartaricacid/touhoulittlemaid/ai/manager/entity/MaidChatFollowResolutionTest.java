package com.github.tartaricacid.touhoulittlemaid.ai.manager.entity;

import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.AvailableSites;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.DefaultLLMSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.openai.LLMOpenAISite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.system.TTSSystemSite;
import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 两层继承链的可执行规范：覆盖 → 世界默认 → 内置兜底。
 *
 * <p>五条用例对应设计里的五句话，其中「同值覆盖仍是覆盖」是最容易被好心人优化掉的一条——
 * 它看起来冗余，实际是「钉住不跟」语义的全部：默认再变，这只女仆不动。</p>
 */
class MaidChatFollowResolutionTest {
    @TempDir
    Path temporaryDirectory;

    @BeforeEach
    void initialize() throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        com.github.tartaricacid.touhoulittlemaid.config.WorldRuleTestHarness.loadDefaults(temporaryDirectory);

        AvailableSites.LLM_SITES.clear();
        AvailableSites.TTS_SITES.clear();
        AvailableSites.LLM_SITES.put("acme", llmSite("acme", true, "acme-large", "acme-small"));
        AvailableSites.LLM_SITES.put("deepseek", llmSite("deepseek", true, "deepseek-chat"));
        AvailableSites.LLM_SITES.put("closed", llmSite("closed", false, "closed-model"));
        AvailableSites.TTS_SITES.put(TTSSystemSite.API_TYPE,
                new TTSSystemSite.Serializer().defaultSite());
    }

    private static LLMOpenAISite llmSite(String id, boolean enabled, String... models) {
        LLMOpenAISite site = new LLMOpenAISite(id, com.github.tartaricacid.touhoulittlemaid.ai.service.SerializableSite.defaultIcon(id),
                "https://example.invalid/v1", enabled, "", Map.of(),
                java.util.Arrays.stream(models).map(m -> new LLMOpenAISite.ModelEntry(m, false)).toList());
        return site;
    }

    private void setDefaults(String site, String model) {
        // §17 v2：默认值住在实例级 AI 店，写入口是它的 applyJson（保存即激活）
        String json = "{\"%s\":\"%s\",\"%s\":\"%s\"}".formatted(
                ServerRuleConfig.key(AIConfig.DEFAULT_LLM_SITE), site,
                ServerRuleConfig.key(AIConfig.DEFAULT_LLM_MODEL), model);
        assertTrue(com.github.tartaricacid.touhoulittlemaid.config.WorldRuleTestHarness.applyAiJson(json),
                "设置默认失败：" + json);
    }

    @Test
    void followingResolvesToTheWorldDefault() {
        setDefaults("acme", "acme-small");
        LLMSite site = MaidAIChatData.resolveLLMSite("");
        assertNotNull(site);
        assertEquals("acme", site.id(), "空覆盖 = 跟随，应解析到世界默认站点");
        assertEquals("acme-small", MaidAIChatData.resolveLLMModel("", ""),
                "跟随且落在默认站点上时，应使用世界默认模型");
    }

    @Test
    void aBrokenWorldDefaultFallsBackToTheBuiltin() {
        setDefaults("closed", "closed-model");
        LLMSite site = MaidAIChatData.resolveLLMSite("");
        assertNotNull(site);
        assertEquals(DefaultLLMSite.DEEPSEEK.id(), site.id(),
                "默认站点被禁用时应滑到内置兜底，而不是返回禁用站点");
        assertEquals("deepseek-chat", MaidAIChatData.resolveLLMModel("", ""),
                "滑到兜底后不得把默认模型套在兜底站点头上");
    }

    @Test
    void anOverrideBeatsTheDefault() {
        setDefaults("deepseek", "");
        LLMSite site = MaidAIChatData.resolveLLMSite("acme");
        assertNotNull(site);
        assertEquals("acme", site.id());
        assertEquals("acme-large", MaidAIChatData.resolveLLMModel("acme", "acme-large"));
    }

    @Test
    void anOverrideEqualToTheDefaultStillPins() {
        setDefaults("acme", "acme-large");
        // 覆盖值与默认相同——随后默认改走别家，这只女仆必须原地不动
        assertEquals("acme", MaidAIChatData.resolveLLMSite("acme").id());
        setDefaults("deepseek", "");
        LLMSite site = MaidAIChatData.resolveLLMSite("acme");
        assertNotNull(site);
        assertEquals("acme", site.id(), "同值覆盖也是覆盖：默认变化不得带走它");
    }

    @Test
    void aDeadOverrideFallsBackToTheDefaultNotTheBuiltin() {
        setDefaults("acme", "acme-small");
        LLMSite site = MaidAIChatData.resolveLLMSite("closed");
        assertNotNull(site);
        assertEquals("acme", site.id(),
                "覆盖失效应回落到世界默认——直接跳内置兜底会让管理员配好的默认形同虚设");
    }

    @Test
    void ttsChainSharesTheSameShape() {
        assertNotNull(MaidAIChatData.resolveTTSSite(""), "TTS 跟随应至少解析到 system 兜底");
        assertEquals(TTSSystemSite.API_TYPE, MaidAIChatData.resolveTTSSite("missing-site").id(),
                "TTS 覆盖失效且无默认时回落 system");
    }

}
