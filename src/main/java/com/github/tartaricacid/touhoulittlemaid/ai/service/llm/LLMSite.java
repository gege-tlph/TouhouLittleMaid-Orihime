package com.github.tartaricacid.touhoulittlemaid.ai.service.llm;

import com.github.tartaricacid.touhoulittlemaid.ai.service.ConfigProxySelector;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SerializerRegister;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ServiceType;
import com.github.tartaricacid.touhoulittlemaid.ai.service.Site;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SiteJsonConfigWriter;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.JsonOps;
import net.minecraft.util.GsonHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

public interface LLMSite extends Site {
    Logger LOGGER = LogManager.getLogger(LLMSite.class);
    HttpClient LLM_HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .proxy(new ConfigProxySelector(AIConfig.LLM_PROXY_ADDRESS))
            .version(HttpClient.Version.HTTP_1_1)
            .build();


    static Map<String, LLMSite> readSitesStrict(Path file) throws IOException {
        Map<String, LLMSite> output = Maps.newHashMap();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = GsonHelper.parse(reader);
            for (String id : root.keySet()) {
                JsonElement value = root.get(id);
                if (!(value instanceof JsonObject jsonObject)) {
                    continue;
                }
                String apiType = GsonHelper.getAsString(jsonObject, API_TYPE);
                var serializer = SerializerRegister.getLLMSerializer(apiType);
                if (serializer == null) {
                    LOGGER.error("Unknown LLM site type: {}", apiType);
                    continue;
                }
                var decoded = serializer.codec().decode(JsonOps.INSTANCE, value).result()
                        .orElseThrow(() -> new IllegalArgumentException("Invalid LLM site: " + id));
                output.put(id, decoded.getFirst());
            }
        }
        return output;
    }

    static boolean writeSites(Path file, Map<String, LLMSite> sites) {
        try {
            JsonObject root = new JsonObject();
            for (String id : sites.keySet()) {
                LLMSite site = sites.get(id);
                var serializer = SerializerRegister.getLLMSerializer(site.getApiType());
                JsonElement json = serializer.codec()
                        .encodeStart(JsonOps.INSTANCE, site)
                        .resultOrPartial(LOGGER::error)
                        .orElseThrow();
                json.getAsJsonObject().addProperty(API_TYPE, site.getApiType());
                root.add(id, json);
            }
            SiteJsonConfigWriter.write(file, root,
                    apiType -> SerializerRegister.getLLMSerializer(apiType) != null);
            return true;
        } catch (RuntimeException | IOException e) {
            LOGGER.error("Failed to save sites", e);
            return false;
        }
    }

    @Override
    LLMClient client();

    @Override
    default ServiceType getServiceType() {
        return ServiceType.LLM;
    }
}
