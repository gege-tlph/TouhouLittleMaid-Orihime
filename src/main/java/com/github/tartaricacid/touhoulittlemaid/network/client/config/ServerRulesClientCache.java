package com.github.tartaricacid.touhoulittlemaid.network.client.config;

import com.github.tartaricacid.touhoulittlemaid.ai.service.stt.STTSite;
import com.github.tartaricacid.touhoulittlemaid.client.input.STTChatKey;
import com.github.tartaricacid.touhoulittlemaid.config.ServerConfig;
import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.config.ServerSTTApiType;
import com.github.tartaricacid.touhoulittlemaid.network.message.config.RequestServerSTTSitePacket;
import com.github.tartaricacid.touhoulittlemaid.network.message.config.SaveServerRulesPacket;
import com.github.tartaricacid.touhoulittlemaid.network.message.config.SyncServerRulesPacket;
import com.github.tartaricacid.touhoulittlemaid.network.message.config.SyncServerSTTSitePacket;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig.key;

public final class ServerRulesClientCache {
    private static final Gson GSON = new Gson();
    private static boolean canEdit;
    private static boolean integratedServer;
    private static JsonObject values = new JsonObject();
    private static final Map<String, STTSite> SERVER_STT_SITES = Maps.newLinkedHashMap();
    private static boolean serverRulesReceived;
    private static boolean disconnected = true;
    private static boolean useServerStt;
    private static String connectionKey = "";
    private static @Nullable STTSite runtimeServerSttSite;

    private ServerRulesClientCache() {
    }

    public static void update(SyncServerRulesPacket packet) {
        String nextConnectionKey = currentConnectionKey();
        boolean sameConnection = Objects.equals(connectionKey, nextConnectionKey);
        boolean proxyBackendSwitch = packet.initialSync() && serverRulesReceived
                && !disconnected && sameConnection;
        boolean proxyCompatibility = ServerRuleConfig.get(ServerConfig.PROXY_SERVER_COMPATIBILITY);
        if (!sameConnection || (proxyBackendSwitch && !proxyCompatibility)) {
            useServerStt = false;
            clearRuntimeServerSttSite();
        }
        connectionKey = nextConnectionKey;
        disconnected = false;
        serverRulesReceived = true;

        canEdit = packet.canEdit();
        integratedServer = packet.integratedServer();
        try {
            values = JsonParser.parseString(packet.editableRulesJson()).getAsJsonObject();
        } catch (RuntimeException exception) {
            values = new JsonObject();
            canEdit = false;
        }
        SERVER_STT_SITES.clear();
        SERVER_STT_SITES.putAll(packet.serverSttSites());
        if (!ServerRuleConfig.get(ServerConfig.PROVIDE_SERVER_STT)) {
            clearRuntimeServerSttSite();
        } else if (useServerStt) {
            requestServerSttSite();
        }
    }

    public static void clear() {
        canEdit = false;
        integratedServer = false;
        values = new JsonObject();
        SERVER_STT_SITES.clear();
        serverRulesReceived = false;
        disconnected = true;
        clearRuntimeServerSttSite();
    }

    public static boolean canEdit() {
        return canEdit;
    }

    public static boolean isIntegratedServer() {
        return integratedServer;
    }

    public static Session createSession() {
        return new Session(values.deepCopy());
    }

    public static Map<String, STTSite> serverSttSites() {
        return Collections.unmodifiableMap(SERVER_STT_SITES);
    }

    public static boolean isUsingServerStt() {
        return useServerStt;
    }

    public static boolean isServerSttOffered() {
        return serverRulesReceived && ServerRuleConfig.get(ServerConfig.PROVIDE_SERVER_STT);
    }

    public static void setUsingServerStt(boolean useServer) {
        useServerStt = useServer;
        if (useServer) {
            requestServerSttSite();
        } else {
            clearRuntimeServerSttSite();
        }
    }

    public static @Nullable STTSite runtimeServerSttSite() {
        return runtimeServerSttSite;
    }

    public static void requestServerSttSite() {
        if (!useServerStt || !isServerSttOffered() || Minecraft.getInstance().getConnection() == null) {
            return;
        }
        ClientPlayNetworking.send(RequestServerSTTSitePacket.INSTANCE);
    }

    public static void updateServerSttSite(SyncServerSTTSitePacket packet) {
        if (!useServerStt || !isServerSttOffered() || packet.site() == null) {
            clearRuntimeServerSttSite();
            return;
        }
        ServerSTTApiType expected = ServerRuleConfig.get(ServerConfig.SERVER_STT_TYPE);
        if (!expected.siteId().equals(packet.siteId())
                || !expected.apiType().getName().equals(packet.site().getApiType())) {
            clearRuntimeServerSttSite();
            return;
        }
        runtimeServerSttSite = packet.site();
    }

    private static void clearRuntimeServerSttSite() {
        runtimeServerSttSite = null;
        STTChatKey.cancelServerRecording();
    }

    private static String currentConnectionKey() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getSingleplayerServer() != null) {
            return "singleplayer:" + minecraft.getSingleplayerServer().getWorldData().getLevelName();
        }
        if (minecraft.getCurrentServer() != null) {
            return "server:" + minecraft.getCurrentServer().ip;
        }
        return "";
    }

    public static final class Session {
        private final JsonObject pending;
        private final JsonObject changed = new JsonObject();

        private Session(JsonObject pending) {
            this.pending = pending;
        }

        public boolean getBoolean(ModConfigSpec.ConfigValue<Boolean> value) {
            JsonElement element = pending.get(key(value));
            return element == null ? value.getDefault() : element.getAsBoolean();
        }

        public int getInt(ModConfigSpec.ConfigValue<Integer> value) {
            JsonElement element = pending.get(key(value));
            return element == null ? value.getDefault() : element.getAsInt();
        }

        public double getDouble(ModConfigSpec.ConfigValue<Double> value) {
            JsonElement element = pending.get(key(value));
            return element == null ? value.getDefault() : element.getAsDouble();
        }

        public String getString(ModConfigSpec.ConfigValue<String> value) {
            JsonElement element = pending.get(key(value));
            return element == null ? value.getDefault() : element.getAsString();
        }

        public ServerSTTApiType getServerSttType() {
            JsonElement element = pending.get(key(com.github.tartaricacid.touhoulittlemaid.config.ServerConfig.SERVER_STT_TYPE));
            if (element == null) {
                return com.github.tartaricacid.touhoulittlemaid.config.ServerConfig.SERVER_STT_TYPE.getDefault();
            }
            try {
                return ServerSTTApiType.valueOf(element.getAsString().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                return com.github.tartaricacid.touhoulittlemaid.config.ServerConfig.SERVER_STT_TYPE.getDefault();
            }
        }

        public List<String> getStringList(ModConfigSpec.ConfigValue<? extends List<? extends String>> value) {
            JsonElement element = pending.get(key(value));
            if (element == null) {
                List<String> defaults = new ArrayList<>();
                value.getDefault().forEach(defaults::add);
                return defaults;
            }
            List<String> result = new ArrayList<>();
            for (JsonElement item : element.getAsJsonArray()) {
                result.add(item.getAsString());
            }
            return result;
        }

        public List<String> getContainerPairs(ModConfigSpec.ConfigValue<List<List<String>>> value) {
            JsonElement element = pending.get(key(value));
            List<List<String>> source = element == null
                    ? value.getDefault()
                    : GSON.fromJson(element, List.class);
            List<String> result = new ArrayList<>();
            for (List<?> pair : source) {
                if (pair.size() == 2) {
                    result.add(String.valueOf(pair.get(0)) + "," + pair.get(1));
                }
            }
            return result;
        }

        public void set(ModConfigSpec.ConfigValue<?> value, Object newValue) {
            JsonElement jsonValue = GSON.toJsonTree(newValue);
            pending.add(key(value), jsonValue);
            changed.add(key(value), jsonValue.deepCopy());
        }

        public void setContainerPairs(ModConfigSpec.ConfigValue<List<List<String>>> value, List<String> pairs) {
            JsonArray array = new JsonArray();
            for (String pairText : pairs) {
                String[] split = pairText.split(",", 2);
                if (split.length != 2) {
                    continue;
                }
                JsonArray pair = new JsonArray();
                pair.add(split[0]);
                pair.add(split[1]);
                array.add(pair);
            }
            pending.add(key(value), array);
            changed.add(key(value), array.deepCopy());
        }

        public void save() {
            if (changed.isEmpty()) {
                return;
            }
            ClientPlayNetworking.send(new SaveServerRulesPacket(GSON.toJson(changed)));
            changed.entrySet().clear();
        }
    }
}
