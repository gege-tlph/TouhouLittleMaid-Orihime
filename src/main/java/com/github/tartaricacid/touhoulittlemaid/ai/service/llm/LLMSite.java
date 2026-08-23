package com.github.tartaricacid.touhoulittlemaid.ai.service.llm;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ConfigProxySelector;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SerializerRegister;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ServiceType;
import com.github.tartaricacid.touhoulittlemaid.ai.service.Site;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SiteJsonConfigWriter;
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

public interface LLMSite extends Site {
    Logger LOGGER = LogManager.getLogger(LLMSite.class);
    HttpClient LLM_HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            // 实例级 AI 规则，经唯一读口路由到 AI 店
            .proxy(new ConfigProxySelector(() -> ServerRuleConfig.get(AIConfig.LLM_PROXY_ADDRESS)))
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    /**
     * 严格读取：任何一个站点解不出来就抛，由调用方决定怎么降级。
     *
     * <p>原实现把解码失败 {@code resultOrPartial} 掉、然后**继续**——于是一份坏文件会静默
     * 变成一张缺项的站点表，而缺项在下游被翻译成「服务器不提供该服务」。失败必须能被上层看见。</p>
     *
     * <p>{@code api_type} 未知的条目仍是跳过而非抛出：那是**扩展 mod 的站点**，不是坏数据。
     * 这条跳过判据必须与 {@link SiteJsonConfigWriter#foreignIds} 收下的那批完全一致，
     * 否则扩展站点会掉进「既不被读、也不被保留」的缝里，一次保存就没了。</p>
     */
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

    /** @return 是否写成功；失败时调用方据此把保存报告为失败，而不是静默丢改动 */
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
