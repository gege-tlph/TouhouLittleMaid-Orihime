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
 * 服务器 → 客户端的世界规则快照。
 *
 * <p>两份 JSON 是有意分开的：{@code runtimeRulesJson} 是**当前生效值**，发给所有人，
 * 客户端拿它喂 {@link ServerRuleConfig#applyRuntimeJson}，让客户端侧的读点（区域渲染、
 * 指南针范围等）看到的是服务器的值而不是自己那份默认值；{@code editableRulesJson} 是
 * **文件里的待编辑值**，只发给有编辑权的人，配置菜单拿它做编辑基线。
 * 专服上两者会不同——保存只写文件，要等 {@code /tlm config reload} 才激活。</p>
 *
 * <p>世界规则与实例级 AI 规则（{@link AiServerRuleConfig}）在网络上仍是**一张**扁平 kv 快照
 * ——客户端缓存、Session 与全部 GUI 因此零改动。键名两店不重叠（各自的 {@code values()} 表），
 * 合流与分拣都是安全的。</p>
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
            return new SyncServerRulesPacket(runtimeRulesJson, integratedServer, canEdit, editableRulesJson);
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
        MinecraftServer server = player.level().getServer();
        ServerPlayNetworking.send(player, new SyncServerRulesPacket(
                mergeJson(ServerRuleConfig.runtimeSnapshotJson(), AiServerRuleConfig.runtimeSnapshotJson()),
                server == null || !server.isDedicatedServer(),
                canEdit,
                canEdit ? mergeJson(ServerRuleConfig.snapshotJson(), AiServerRuleConfig.snapshotJson()) : "{}"
        ));
    }

    /** 两店的快照拼成一张扁平 kv；键名不重叠，后者不会覆盖前者的任何键。 */
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
