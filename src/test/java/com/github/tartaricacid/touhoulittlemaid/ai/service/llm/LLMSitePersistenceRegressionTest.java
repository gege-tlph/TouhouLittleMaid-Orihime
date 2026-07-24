package com.github.tartaricacid.touhoulittlemaid.ai.service.llm;

import com.github.tartaricacid.touhoulittlemaid.ai.service.SerializerRegister;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ServiceType;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.openai.LLMOpenAISite;
import net.minecraft.resources.Identifier;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LLMSitePersistenceRegressionTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void serializationFailureMustNotTruncateLastGoodFile() throws Exception {
        SerializerRegister.LLM_SERIALIZER = new HashMap<>();
        new SerializerRegister().register(ServiceType.LLM, LLMOpenAISite.API_TYPE,
                new LLMOpenAISite.Serializer());
        Path target = temporaryDirectory.resolve("llm.json");
        String lastGood = "{\n  \"preserved\": {\"api_type\": \"openai\"}\n}\n";
        Files.writeString(target, lastGood, StandardCharsets.UTF_8);

        Map<String, LLMSite> sites = new LinkedHashMap<>();
        sites.put("valid", new LLMOpenAISite.Serializer().defaultSite());
        sites.put("invalid", new UnknownSite());

        assertFalse(LLMSite.writeSites(target, sites));
        assertEquals(lastGood, Files.readString(target, StandardCharsets.UTF_8),
                "a failed serialization must leave the complete last-good file untouched");
    }

    @Test
    void successfulRewritePreservesUnknownFieldsAndUnknownProviderSites() throws Exception {
        SerializerRegister.LLM_SERIALIZER = new HashMap<>();
        new SerializerRegister().register(ServiceType.LLM, LLMOpenAISite.API_TYPE,
                new LLMOpenAISite.Serializer());
        Path target = temporaryDirectory.resolve("llm.json");
        LLMOpenAISite site = new LLMOpenAISite.Serializer().defaultSite();
        assertTrue(LLMSite.writeSites(target, Map.of(site.id(), site)));

        JsonObject edited = JsonParser.parseString(Files.readString(target, StandardCharsets.UTF_8))
                .getAsJsonObject();
        edited.getAsJsonObject(site.id()).addProperty("extension_field", "preserve-me");
        JsonObject unknownProvider = new JsonObject();
        unknownProvider.addProperty("api_type", "extension_provider");
        unknownProvider.addProperty("secret", "opaque");
        edited.add("extension_site", unknownProvider);
        Files.writeString(target, edited.toString(), StandardCharsets.UTF_8);

        site.setEnabled(!site.enabled());
        assertTrue(LLMSite.writeSites(target, Map.of(site.id(), site)));
        JsonObject rewritten = JsonParser.parseString(Files.readString(target, StandardCharsets.UTF_8))
                .getAsJsonObject();
        assertEquals("preserve-me", rewritten.getAsJsonObject(site.id())
                .get("extension_field").getAsString());
        assertEquals("opaque", rewritten.getAsJsonObject("extension_site")
                .get("secret").getAsString());
    }

    private static final class UnknownSite implements LLMSite {
        @Override
        public LLMClient client() {
            return null;
        }

        @Override
        public String id() {
            return "invalid";
        }

        @Override
        public boolean enabled() {
            return true;
        }

        @Override
        public void setEnabled(boolean enabled) {
        }

        @Override
        public Identifier icon() {
            return Identifier.fromNamespaceAndPath("touhou_little_maid", "invalid");
        }

        @Override
        public String url() {
            return "https://invalid.example";
        }

        @Override
        public Map<String, String> headers() {
            return Map.of();
        }

        @Override
        public String getApiType() {
            return "unknown";
        }
    }
}
