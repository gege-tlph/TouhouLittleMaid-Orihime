package com.github.tartaricacid.touhoulittlemaid.network.message.config;

import com.github.tartaricacid.touhoulittlemaid.client.download.ClientPackDownloadManager;
import com.github.tartaricacid.touhoulittlemaid.config.AiServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.network.client.config.ServerRulesClientCache;
import com.github.tartaricacid.touhoulittlemaid.util.GameModeUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;


import static com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil.modLoc;

/**
 * 曾经还带一个 {@code initialSync} 布尔，用来让客户端识别「同一连接上再次收到首包 = 代理换了后端」。
 * 那个判定唯一的用途是作废下发到客户端的 STT 凭据；「服务器提供 STT」撤除后没有凭据可作废，
 * 该字段随之失去全部读取方，一并删除——留着的协议字段迟早会被当成还有意义的东西。
 */
public record SyncServerRulesPacket(String runtimeRulesJson, boolean integratedServer,
                                    boolean canEdit, String editableRulesJson) implements CustomPacketPayload {
    private static final int MAX_JSON_LENGTH = 1_048_576;
    public static final Type<SyncServerRulesPacket> TYPE = new Type<>(modLoc("sync_server_rules"));
    public static final StreamCodec<ByteBuf, SyncServerRulesPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SyncServerRulesPacket decode(ByteBuf byteBuf) {
            FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
            String runtimeRulesJson = buf.readUtf(MAX_JSON_LENGTH);
            boolean integratedServer = buf.readBoolean();
            boolean canEdit = buf.readBoolean();
            String editableRulesJson = buf.readUtf(MAX_JSON_LENGTH);
            return new SyncServerRulesPacket(runtimeRulesJson, integratedServer,
                    canEdit, editableRulesJson);
        }

        @Override
        public void encode(ByteBuf byteBuf, SyncServerRulesPacket message) {
            FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
            buf.writeUtf(message.runtimeRulesJson, MAX_JSON_LENGTH);
            buf.writeBoolean(message.integratedServer);
            buf.writeBoolean(message.canEdit);
            buf.writeUtf(message.editableRulesJson, MAX_JSON_LENGTH);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Environment(EnvType.CLIENT)
    public static void handle(SyncServerRulesPacket message, ClientPlayNetworking.Context context) {
        context.client().execute(() -> {
            // 合流快照喂两个店：各自只认领自己的键，互不干扰
            if (ServerRuleConfig.applyRuntimeJson(message.runtimeRulesJson())) {
                ClientPackDownloadManager.downloadClientPack();
            }
            AiServerRuleConfig.applyRuntimeJson(message.runtimeRulesJson());
            ServerRulesClientCache.update(message);
        });
    }

    public static void sendTo(ServerPlayer player) {
        boolean canEdit = GameModeUtil.canEditSite(player);
        ServerPlayNetworking.send(player, new SyncServerRulesPacket(
                mergeJson(ServerRuleConfig.runtimeSnapshotJson(), AiServerRuleConfig.runtimeSnapshotJson()),
                !player.level().getServer().isDedicatedServer(),
                canEdit,
                canEdit ? mergeJson(ServerRuleConfig.snapshotJson(), AiServerRuleConfig.snapshotJson()) : "{}"
        ));
    }

    /**
     * 世界规则与 AI 规则（§17 v2 拆到实例级的 {@code AiServerRuleConfig}）在网络上仍是**一张**
     * 扁平 kv 快照——客户端缓存、Session 与全部 GUI 因此零改动。键名两店不重叠（各自的 values 表）。
     */
    private static String mergeJson(String first, String second) {
        JsonObject merged = JsonParser.parseString(first).getAsJsonObject();
        JsonObject extra = JsonParser.parseString(second).getAsJsonObject();
        extra.entrySet().forEach(entry -> merged.add(entry.getKey(), entry.getValue()));
        return merged.toString();
    }

    public static void syncToEditors(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (GameModeUtil.canEditSite(player)) {
                sendTo(player);
            }
        }
    }

    public static void syncToAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendTo(player);
        }
    }

}
