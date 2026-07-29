package com.github.tartaricacid.touhoulittlemaid.ai.manager.site;

import com.github.tartaricacid.touhoulittlemaid.ai.service.SerializerRegister;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ServiceType;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.openai.LLMOpenAISite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.stt.aliyun.STTAliyunSite;
import com.github.tartaricacid.touhoulittlemaid.config.GeneralConfig;
import com.github.tartaricacid.touhoulittlemaid.config.ServerConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvailableSitesTransactionTest {
    @TempDir
    Path temporaryDirectory;

    @BeforeAll
    static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        ServerConfig.init();
        GeneralConfig.getConfigSpec();
    }

    @Test
    void damagedReloadKeepsActiveSnapshotAndRestoresCompleteFiles() throws Exception {
        SerializerRegister.LLM_SERIALIZER = new HashMap<>();
        SerializerRegister.TTS_SERIALIZER = Map.of();
        SerializerRegister.STT_SERIALIZER = Map.of();
        new SerializerRegister().register(ServiceType.LLM, LLMOpenAISite.API_TYPE,
                new LLMOpenAISite.Serializer());
        assertTrue(AvailableSites.init(temporaryDirectory, false));
        Map<String, LLMSite> activeBefore = new HashMap<>(AvailableSites.LLM_SITES);
        Path llmFile = temporaryDirectory.resolve("llm.json");
        String lastGoodHash = sha256(llmFile);

        Files.writeString(llmFile, "{\"truncated\":", StandardCharsets.UTF_8);
        assertFalse(AvailableSites.init(temporaryDirectory, false));
        assertEquals(activeBefore, AvailableSites.LLM_SITES);
        assertEquals(lastGoodHash, sha256(llmFile));
        assertTrue(LLMSite.readSitesStrict(llmFile).size() > 0);

        assertTrue(AvailableSites.init(temporaryDirectory, false));
        assertTrue(AvailableSites.LLM_SITES.size() > 0);
    }

    /**
     * P0 回归：启动时 stt.json 不可解析且无 last-good 可回退。
     *
     * <p>原实现在任一文件读取失败时整批放弃，启动场景下 {@code STT_SITES} 因此保持为空，
     * {@code getSTTSite} 恒返回 null，服务端下发空站点，玩家看到「服务器不提供语音识别」——
     * 而真相是服务器提供了、只是站点表是空的。现在该服务降级到内置默认站点而不是消失，
     * 且 llm 不受牵连。</p>
     */
    @Test
    void corruptSttAtFirstLoadDegradesToDefaultsAndDoesNotTakeLlmDownWithIt() throws Exception {
        registerLlmAndSttSerializers();
        Path sttFile = temporaryDirectory.resolve("stt.json");
        Files.writeString(sttFile, "{ \"aliyun\": { \"id\": \"ali", StandardCharsets.UTF_8);
        assertFalse(Files.exists(temporaryDirectory.resolve("stt.json.last-good")),
                "本用例要求没有 last-good，否则走的是恢复路径而不是降级路径");

        assertFalse(AvailableSites.init(temporaryDirectory, false),
                "损坏的 stt.json 必须让本次加载报告为不完整");

        assertFalse(AvailableSites.STT_SITES.isEmpty(),
                "STT 站点表为空正是 P0：getSTTSite 恒返回 null，玩家被告知服务器不提供语音识别");
        assertTrue(AvailableSites.STT_SITES.containsKey(STTAliyunSite.API_TYPE),
                "降级后必须留下内置默认站点");
        assertFalse(AvailableSites.LLM_SITES.isEmpty(),
                "一份 stt.json 损坏不得连带让 llm 失效");
    }

    /**
     * 同样是损坏 + 无 last-good，但发生在已经跑起来之后：此时内存里是管理员填好的凭据，
     * 降级绝不能把它们换成空的内置默认值——那会比不修更糟。
     */
    @Test
    void corruptSttOnReloadKeepsTheRunningSitesInsteadOfDowngradingToDefaults() throws Exception {
        registerLlmAndSttSerializers();
        assertTrue(AvailableSites.init(temporaryDirectory, false));
        Path sttFile = temporaryDirectory.resolve("stt.json");
        Map<String, ?> runningBefore = new HashMap<>(AvailableSites.STT_SITES);
        assertFalse(runningBefore.isEmpty(), "fixture 未能建立可用的运行时站点表");

        Files.writeString(sttFile, "{ \"aliyun\": { \"id\": \"ali", StandardCharsets.UTF_8);
        Files.deleteIfExists(temporaryDirectory.resolve("stt.json.last-good"));

        assertFalse(AvailableSites.init(temporaryDirectory, false));
        assertEquals(runningBefore, AvailableSites.STT_SITES,
                "reload 读取失败时必须原样保住正在使用的站点，不得回落到内置默认值");
    }

    private static void registerLlmAndSttSerializers() {
        SerializerRegister.LLM_SERIALIZER = new HashMap<>();
        SerializerRegister.TTS_SERIALIZER = Map.of();
        SerializerRegister.STT_SERIALIZER = new HashMap<>();
        SerializerRegister register = new SerializerRegister();
        register.register(ServiceType.LLM, LLMOpenAISite.API_TYPE, new LLMOpenAISite.Serializer());
        register.register(ServiceType.STT, STTAliyunSite.API_TYPE, new STTAliyunSite.Serializer());
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(path)));
    }
}
