package com.github.tartaricacid.touhoulittlemaid.config;

import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 攻击面测试：{@code SaveServerRulesPacket} 的载荷最终落到两个店的 {@code applyJson}——
 * 这里绕过网络直接打它们，钉住「畸形/错型/越界/超长/未知键一律整批拒绝，拒绝后运行时值不变」。
 *
 * <p>GUI 端的 512 输入上限只是君子锁；改装客户端能塞任何东西（载荷解码上限 1MB）。
 * 服务端字符串封顶 4096（{@code ServerRuleConfig#decode} 共享防线，两店同吃），
 * 否则超长串会写进 TOML、进公开快照、再广播给每个进服玩家。</p>
 */
class AiServerRuleAttackTest {
    @TempDir
    Path temporaryDirectory;

    @BeforeEach
    void initialize() throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        WorldRuleTestHarness.loadDefaults(temporaryDirectory);
    }

    private static String key(net.neoforged.neoforge.common.ModConfigSpec.ConfigValue<?> value) {
        return ServerRuleConfig.key(value);
    }

    @Test
    void malformedJsonIsRejected() {
        assertFalse(AiServerRuleConfig.applyJson("not json at all"));
        assertFalse(AiServerRuleConfig.applyJson("[1,2,3]"));
        assertFalse(AiServerRuleConfig.applyJson(""));
    }

    @Test
    void unknownKeysAloneAreRejected() {
        assertFalse(AiServerRuleConfig.applyJson("{\"ai.NoSuchRule\":true}"),
                "只含未知键的提交没有任何可应用项，必须拒绝而不是静默成功");
    }

    @Test
    void wrongTypesAreRejectedAtomically() {
        boolean before = AiServerRuleConfig.get(AIConfig.LLM_ENABLED);
        // 数字塞给字符串键会被字符串化（Gson 宽松），但字符串塞给整数键必须炸
        assertFalse(AiServerRuleConfig.applyJson(
                "{\"%s\":\"definitely-not-a-number\"}".formatted(key(AIConfig.MAX_TOKENS_PER_PLAYER))));
        assertEquals(before, AiServerRuleConfig.get(AIConfig.LLM_ENABLED), "拒绝后运行时值不得改变");
    }

    @Test
    void outOfRangeIntegersAreRejected() {
        assertFalse(AiServerRuleConfig.applyJson(
                "{\"%s\":0}".formatted(key(AIConfig.MAX_TOKENS_PER_PLAYER))), "低于 defineInRange 下限必须拒绝");
        assertFalse(AiServerRuleConfig.applyJson(
                "{\"%s\":-5}".formatted(key(AIConfig.MAID_HISTORY_COMPRESS_TOKEN_LIMIT))));
    }

    @Test
    void oversizedStringsAreRejectedByTheSharedDecodeCap() {
        String huge = "x".repeat(5000);
        assertFalse(AiServerRuleConfig.applyJson(
                "{\"%s\":\"%s\"}".formatted(key(AIConfig.LLM_PROXY_ADDRESS), huge)),
                "超过 4096 的字符串必须拒绝——它会进公开快照并广播给每个进服玩家");
        // 世界规则店吃同一条防线
        assertFalse(ServerRuleConfig.applyJson(
                "{\"%s\":\"%s\"}".formatted(key(MaidConfig.MAID_TAMED_ITEM), huge), true));
        // 界限内照常接受
        assertTrue(AiServerRuleConfig.applyJson(
                "{\"%s\":\"%s\"}".formatted(key(AIConfig.LLM_PROXY_ADDRESS), "x".repeat(512))));
    }

    @Test
    void aRejectedBatchLeavesEveryValueUntouched() {
        assertTrue(AiServerRuleConfig.applyJson(
                "{\"%s\":\"acme\"}".formatted(key(AIConfig.DEFAULT_LLM_SITE))));
        // 合法键 + 非法键混合：整批拒绝，前面的合法键也不许落
        assertFalse(AiServerRuleConfig.applyJson(
                "{\"%s\":\"gemini\",\"%s\":0}".formatted(
                        key(AIConfig.DEFAULT_LLM_SITE), key(AIConfig.MAX_TOKENS_PER_PLAYER))));
        assertEquals("acme", AiServerRuleConfig.get(AIConfig.DEFAULT_LLM_SITE),
                "整批拒绝必须是事务性的：混入一个非法值，合法的那半也不能生效");
    }
}
