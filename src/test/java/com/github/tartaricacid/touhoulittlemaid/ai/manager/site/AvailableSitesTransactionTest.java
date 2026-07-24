package com.github.tartaricacid.touhoulittlemaid.ai.manager.site;

import com.github.tartaricacid.touhoulittlemaid.ai.service.SerializerRegister;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ServiceType;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.openai.LLMOpenAISite;
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

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(path)));
    }
}
