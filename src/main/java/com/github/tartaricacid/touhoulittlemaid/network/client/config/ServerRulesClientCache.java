package com.github.tartaricacid.touhoulittlemaid.network.client.config;

import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.network.message.config.SaveServerRulesPacket;
import com.github.tartaricacid.touhoulittlemaid.network.message.config.SyncServerRulesPacket;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;

import static com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig.key;

public final class ServerRulesClientCache {
    private static final Gson GSON = new Gson();
    private static boolean canEdit;
    private static boolean integratedServer;
    private static JsonObject values = new JsonObject();

    private ServerRulesClientCache() {
    }

    public static void update(SyncServerRulesPacket packet) {
        canEdit = packet.canEdit();
        integratedServer = packet.integratedServer();
        try {
            values = JsonParser.parseString(packet.editableRulesJson()).getAsJsonObject();
        } catch (RuntimeException exception) {
            values = new JsonObject();
            canEdit = false;
        }
    }

    public static void clear() {
        canEdit = false;
        integratedServer = false;
        values = new JsonObject();
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

        /** 有没有「改了但还没保存」的项——设置屏用它在保存按钮上画未保存标记 */
        public boolean isDirty() {
            return !changed.isEmpty();
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
