package com.github.tartaricacid.touhoulittlemaid.ai.service.tts;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ConfigProxySelector;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SerializerRegister;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ServiceType;
import com.github.tartaricacid.touhoulittlemaid.ai.service.Site;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SiteJsonConfigWriter;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.layout.TTSSiteFormLayout;
import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
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

public interface TTSSite extends Site {
    Logger LOGGER = LogManager.getLogger(TTSSite.class);
    HttpClient TTS_HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            // 实例级 AI 规则，经唯一读口路由到 AI 店
            .proxy(new ConfigProxySelector(() -> ServerRuleConfig.get(AIConfig.TTS_PROXY_ADDRESS)))
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    /**
     * 严格读取：任何一个站点解不出来就抛，由调用方决定怎么降级。语义与
     * {@code LLMSite.readSitesStrict} 完全一致，含「未知 api_type 跳过而非抛出」那条。
     */
    static Map<String, TTSSite> readSitesStrict(Path file) throws IOException {
        Map<String, TTSSite> output = Maps.newHashMap();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = GsonHelper.parse(reader);
            for (String id : root.keySet()) {
                JsonElement value = root.get(id);
                if (!(value instanceof JsonObject jsonObject)) {
                    continue;
                }
                String apiType = GsonHelper.getAsString(jsonObject, API_TYPE);
                var serializer = SerializerRegister.getTTSSerializer(apiType);
                if (serializer == null) {
                    LOGGER.error("Unknown TTS site type: {}", apiType);
                    continue;
                }
                var decoded = serializer.codec().decode(JsonOps.INSTANCE, value).result()
                        .orElseThrow(() -> new IllegalArgumentException("Invalid TTS site: " + id));
                output.put(id, decoded.getFirst());
            }
        }
        return output;
    }

    /** @return 是否写成功；失败时调用方据此把保存报告为失败，而不是静默丢改动 */
    static boolean writeSites(Path file, Map<String, TTSSite> sites) {
        try {
            JsonObject root = new JsonObject();
            for (String id : sites.keySet()) {
                TTSSite site = sites.get(id);
                var serializer = SerializerRegister.getTTSSerializer(site.getApiType());
                JsonElement json = serializer.codec()
                        .encodeStart(JsonOps.INSTANCE, site)
                        .resultOrPartial(LOGGER::error)
                        .orElseThrow();
                json.getAsJsonObject().addProperty(API_TYPE, site.getApiType());
                root.add(id, json);
            }
            SiteJsonConfigWriter.write(file, root,
                    apiType -> SerializerRegister.getTTSSerializer(apiType) != null);
            return true;
        } catch (RuntimeException | IOException e) {
            LOGGER.error("Failed to save sites", e);
            return false;
        }
    }

    @Override
    TTSClient client();

    @Override
    default ServiceType getServiceType() {
        return ServiceType.TTS;
    }

    TTSSiteFormLayout formLayout();
}
