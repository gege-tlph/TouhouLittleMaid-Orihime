package com.github.tartaricacid.touhoulittlemaid.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 攻击面测试：{@code SaveServerRulesPacket} 的载荷最终落到两个店的 {@code applyJson}——
 * 这里绕过网络直接打它们，钉住「畸形 / 错型 / 越界 / 超长 / 未知键一律整批拒绝，
 * 拒绝后运行时值不变」。
 *
 * <p>GUI 端的输入上限只是君子锁；改装客户端能塞任何东西（载荷解码上限 1MB）。
 * 服务端字符串封顶 4096（{@link ServerRuleConfig#decode} 共享防线，<b>两店同吃</b>），
 * 否则超长串会写进 TOML、进公开快照、再广播给每个进服玩家。</p>
 *
 * <p>⚠️ 与行为基准 {@code port/1.21.11-fabric} 的同名用例两处差异：① 那边经
 * {@code WorldRuleTestHarness} 引导（本分支未搬那个 helper，改为就地建两份默认文件并加载，
 * <b>并显式断言加载成功</b>——写进去没读出来会让期望 false 的断言集体假绿）；
 * ② 验「世界规则店吃同一条防线」的那一项，那边用 {@code MaidConfig.MAID_TAMED_ITEM}，
 * 而<b>本树没有这个配置项</b>（宿主 26.1 改成了物品标签），改用
 * {@code ServerConfig.CLIENT_PACK_DOWNLOAD_URLS}——它的 spec 校验器不挑内容，
 * 于是「被拒」只可能来自 4096 那条防线，判据更干净。</p>
 */
class AiServerRuleAttackTest {
    @TempDir
    Path temporaryDirectory;

    @BeforeEach
    void initialize() throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        ServerConfig.init();
        AiServerRuleConfig.init();

        // 两个店各建一份纯默认文件并加载。**必须断言加载成功**：加载失败时 activeValues 会停在
        // 上一条用例的残留或空表，随后所有 assertFalse 都会因为「本来就没变」而假绿。
        assertTrue(ServerRuleConfig.loadFromPath(
                writeDefaults(ServerConfig.CONFIG, temporaryDirectory
                        .resolve("world/serverconfig/" + ConfigFileMigration.SERVER_FILE_NAME))));
        assertTrue(AiServerRuleConfig.loadFromPath(
                writeDefaults(AiServerRuleConfig.SPEC, temporaryDirectory
                        .resolve(ConfigFileMigration.AI_SERVER_FILE_NAME))));
    }

    private static Path writeDefaults(ModConfigSpec spec, Path target) throws Exception {
        CommentedConfig config = ConfigFileMigration.emptyConfig();
        spec.correct(config);
        ConfigFileMigration.writeAtomically(config, target);
        return target;
    }

    private static String key(ModConfigSpec.ConfigValue<?> value) {
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
        // 世界规则店吃同一条防线：该键的 spec 校验器不挑内容，故「被拒」只可能来自 4096 那条
        assertFalse(ServerRuleConfig.applyJson(
                "{\"%s\":[\"%s\"]}".formatted(key(ServerConfig.CLIENT_PACK_DOWNLOAD_URLS), huge), true));
        // 界限内照常接受——证明上面两条拒绝来自长度而非类型/校验器
        assertTrue(AiServerRuleConfig.applyJson(
                "{\"%s\":\"%s\"}".formatted(key(AIConfig.LLM_PROXY_ADDRESS), "x".repeat(512))));
        assertTrue(ServerRuleConfig.applyJson(
                "{\"%s\":[\"%s\"]}".formatted(key(ServerConfig.CLIENT_PACK_DOWNLOAD_URLS), "x".repeat(512)), true));
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

    @Test
    void theRoutingSendsAiKeysToTheAiStoreAndNotTheWorldStore() {
        // 唯一读口的路由判据。这条一旦失效，AI 键会落到世界规则店的 activeValues 查表里，
        // 查不到就退回 spec 默认值——症状是「改了配置没反应」，而不是任何异常。
        assertTrue(AiServerRuleConfig.applyJson(
                "{\"%s\":\"routed\"}".formatted(key(AIConfig.DEFAULT_TTS_SITE))));
        assertEquals("routed", ServerRuleConfig.get(AIConfig.DEFAULT_TTS_SITE),
                "经唯一读口读 AI 键，必须路由到 AI 店并拿到刚写入的值");
    }
}
