package com.github.tartaricacid.touhoulittlemaid.ai.service.stt;

import com.github.tartaricacid.touhoulittlemaid.ai.service.ConfigProxySelector;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SerializerRegister;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ServiceType;
import com.github.tartaricacid.touhoulittlemaid.ai.service.Site;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SiteJsonConfigWriter;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.layout.STTSiteFormLayout;
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

public interface STTSite extends Site {
    Logger LOGGER = LogManager.getLogger(STTSite.class);
    HttpClient STT_HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .proxy(new ConfigProxySelector(AIConfig.STT_PROXY_ADDRESS))
            .version(HttpClient.Version.HTTP_1_1)
            .build();


    static Map<String, STTSite> readSitesStrict(Path file) throws IOException {
        Map<String, STTSite> output = Maps.newHashMap();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = GsonHelper.parse(reader);
            for (String id : root.keySet()) {
                JsonElement value = root.get(id);
                if (!(value instanceof JsonObject jsonObject)) {
                    continue;
                }
                String apiType = GsonHelper.getAsString(jsonObject, API_TYPE);
                var serializer = SerializerRegister.getSTTSerializer(apiType);
                if (serializer == null) {
                    LOGGER.error("Unknown STT site type: {}", apiType);
                    continue;
                }
                var decoded = serializer.codec().decode(JsonOps.INSTANCE, value).result()
                        .orElseThrow(() -> new IllegalArgumentException("Invalid STT site: " + id));
                output.put(id, decoded.getFirst());
            }
        }
        return output;
    }

    static boolean writeSites(Path file, Map<String, STTSite> sites) {
        try {
            JsonObject root = new JsonObject();
            for (String id : sites.keySet()) {
                STTSite site = sites.get(id);
                var serializer = SerializerRegister.getSTTSerializer(site.getApiType());
                JsonElement json = serializer.codec()
                        .encodeStart(JsonOps.INSTANCE, site)
                        .resultOrPartial(LOGGER::error)
                        .orElseThrow();
                json.getAsJsonObject().addProperty(API_TYPE, site.getApiType());
                root.add(id, json);
            }
            SiteJsonConfigWriter.write(file, root,
                    apiType -> SerializerRegister.getSTTSerializer(apiType) != null);
            return true;
        } catch (RuntimeException | IOException e) {
            LOGGER.error("Failed to save sites", e);
            return false;
        }
    }

    @Override
    STTClient client();

    @Override
    default ServiceType getServiceType() {
        return ServiceType.STT;
    }

    /**
     * 本站点是否具备可用于发起识别的凭据。
     *
     * <p>用于把「服务器没配好」和「网络不通」分开。服务端站点表在配置文件损坏时会降级到内置默认
     * 站点，那些站点的凭据是空的；若不加区分，玩家只会看到「连接失败，请检查网络」，管理员就会
     *
     * <p>默认返回 true：不需要凭据的站点（如本地 Player2）本来就总是「配好了」。</p>
     */
    default boolean hasUsableCredentials() {
        return true;
    }

    STTSiteFormLayout formLayout();
}
