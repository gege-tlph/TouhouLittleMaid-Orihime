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

    /**
     * 同一个 id 上，磁盘里是本安装读不懂的扩展站点，要写的是我们的内置站点——**必须拒绝整次保存**。
     *
     * <p>严格读取会跳过未知 {@code api_type}，于是那个 id 不在运行时表里，判重放行；
     * 写盘时旧对象与新对象同 id，旧的字段被「补进」新的，而 {@code api_type} 因为总会被写入所以补不回来
     * ——扩展站点就这样被改写成了内置站点，装回扩展也回不去。写入层是最后一道，必须硬拒。</p>
     */
    @Test
    void aCollisionWithAnUnreadableExtensionSiteMustNotConvertIt() throws Exception {
        SerializerRegister.LLM_SERIALIZER = new HashMap<>();
        new SerializerRegister().register(ServiceType.LLM, LLMOpenAISite.API_TYPE,
                new LLMOpenAISite.Serializer());
        Path target = temporaryDirectory.resolve("llm.json");
        String extensionOwned = """
                {"x": {"api_type": "extension_provider", "secret": "opaque"}}
                """;
        Files.writeString(target, extensionOwned, StandardCharsets.UTF_8);

        LLMOpenAISite ours = new LLMOpenAISite.Serializer().defaultSite();
        Map<String, LLMSite> sites = new LinkedHashMap<>();
        sites.put("x", ours);

        assertFalse(LLMSite.writeSites(target, sites),
                "撞上读不懂的扩展站点必须让整次保存失败，而不是挑一个赢家");
        JsonObject onDisk = JsonParser.parseString(
                Files.readString(target, StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals("extension_provider",
                onDisk.getAsJsonObject("x").get("api_type").getAsString(),
                "扩展站点的类型不得被改写");
        assertEquals("opaque", onDisk.getAsJsonObject("x").get("secret").getAsString(),
                "扩展站点自己的字段也不得丢失");
    }

    /**
     * 与上一条相反的那一半：**同一个已知站点上的字段级合并必须继续工作**。
     * 少了它，将来有人会用「干脆别合并了」来「修」上面那条，把扩展往我们站点里加的私有字段一并抹掉。
     */
    @Test
    void fieldLevelMergeOnAKnownSiteStillWorks() throws Exception {
        SerializerRegister.LLM_SERIALIZER = new HashMap<>();
        new SerializerRegister().register(ServiceType.LLM, LLMOpenAISite.API_TYPE,
                new LLMOpenAISite.Serializer());
        Path target = temporaryDirectory.resolve("llm.json");
        LLMOpenAISite ours = new LLMOpenAISite.Serializer().defaultSite();
        Files.writeString(target,
                """
                        {"%s": {"api_type": "openai", "extension_field": 7}}
                        """.formatted(ours.id()),
                StandardCharsets.UTF_8);

        Map<String, LLMSite> sites = new LinkedHashMap<>();
        sites.put(ours.id(), ours);
        assertTrue(LLMSite.writeSites(target, sites));

        JsonObject onDisk = JsonParser.parseString(
                Files.readString(target, StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals(7, onDisk.getAsJsonObject(ours.id()).get("extension_field").getAsInt(),
                "已知站点上的扩展私有字段必须被保留");
    }

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
