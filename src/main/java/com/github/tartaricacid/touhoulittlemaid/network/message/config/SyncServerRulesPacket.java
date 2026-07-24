package com.github.tartaricacid.touhoulittlemaid.network.message.config;

import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.SiteConfigStorage;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SerializableSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SerializerRegister;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ServiceType;
import com.github.tartaricacid.touhoulittlemaid.ai.service.Site;
import com.github.tartaricacid.touhoulittlemaid.ai.service.stt.STTSite;
import com.github.tartaricacid.touhoulittlemaid.client.download.ClientPackDownloadManager;
import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.network.client.config.ServerRulesClientCache;
import com.github.tartaricacid.touhoulittlemaid.util.GameModeUtil;
import com.google.common.collect.Maps;
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
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

import static com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil.modLoc;

public record SyncServerRulesPacket(String runtimeRulesJson, boolean integratedServer, boolean initialSync,
                                    boolean canEdit, String editableRulesJson,
                                    Map<String, STTSite> serverSttSites) implements CustomPacketPayload {
    private static final int MAX_JSON_LENGTH = 1_048_576;
    private static final int MAX_SITE_COUNT = 16;
    private static final int MAX_SITE_ID_LENGTH = 64;
    public static final Type<SyncServerRulesPacket> TYPE = new Type<>(modLoc("sync_server_rules"));
    public static final StreamCodec<ByteBuf, SyncServerRulesPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SyncServerRulesPacket decode(ByteBuf byteBuf) {
            FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
            String runtimeRulesJson = buf.readUtf(MAX_JSON_LENGTH);
            boolean integratedServer = buf.readBoolean();
            boolean initialSync = buf.readBoolean();
            boolean canEdit = buf.readBoolean();
            String editableRulesJson = buf.readUtf(MAX_JSON_LENGTH);
            int size = buf.readVarInt();
            if (size < 0 || size > MAX_SITE_COUNT) {
                throw new IllegalArgumentException("Invalid editable STT site count: " + size);
            }
            Map<String, STTSite> sites = Maps.newLinkedHashMap();
            for (int i = 0; i < size; i++) {
                String id = buf.readUtf(MAX_SITE_ID_LENGTH);
                String apiType = buf.readUtf(MAX_SITE_ID_LENGTH);
                STTSite site = readSite(apiType, buf);
                if (site != null) {
                    sites.put(id, site);
                }
            }
            return new SyncServerRulesPacket(runtimeRulesJson, integratedServer, initialSync,
                    canEdit, editableRulesJson, sites);
        }

        @Override
        public void encode(ByteBuf byteBuf, SyncServerRulesPacket message) {
            FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
            buf.writeUtf(message.runtimeRulesJson, MAX_JSON_LENGTH);
            buf.writeBoolean(message.integratedServer);
            buf.writeBoolean(message.initialSync);
            buf.writeBoolean(message.canEdit);
            buf.writeUtf(message.editableRulesJson, MAX_JSON_LENGTH);
            if (message.serverSttSites.size() > MAX_SITE_COUNT) {
                throw new IllegalArgumentException("Too many editable STT sites");
            }
            buf.writeVarInt(message.serverSttSites.size());
            message.serverSttSites.forEach((id, site) -> {
                buf.writeUtf(id, MAX_SITE_ID_LENGTH);
                buf.writeUtf(site.getApiType(), MAX_SITE_ID_LENGTH);
                writeSite(site, buf);
            });
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Environment(EnvType.CLIENT)
    public static void handle(SyncServerRulesPacket message, ClientPlayNetworking.Context context) {
        context.client().execute(() -> {
            if (ServerRuleConfig.applyRuntimeJson(message.runtimeRulesJson())) {
                ClientPackDownloadManager.downloadClientPack();
            }
            ServerRulesClientCache.update(message);
        });
    }

    public static void sendTo(ServerPlayer player) {
        sendTo(player, false);
    }

    public static void sendInitialTo(ServerPlayer player) {
        sendTo(player, true);
    }

    private static void sendTo(ServerPlayer player, boolean initialSync) {
        boolean canEdit = GameModeUtil.canEditSite(player);
        ServerPlayNetworking.send(player, new SyncServerRulesPacket(
                ServerRuleConfig.runtimeSnapshotJson(),
                !player.level().getServer().isDedicatedServer(),
                initialSync,
                canEdit,
                canEdit ? ServerRuleConfig.snapshotJson() : "{}",
                canEdit ? editableSttSites() : Collections.emptyMap()
        ));
    }

    private static Map<String, STTSite> editableSttSites() {
        try {
            return SiteConfigStorage.readSTT();
        } catch (IllegalStateException exception) {
            return Collections.emptyMap();
        }
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

    @SuppressWarnings("unchecked")
    private static void writeSite(STTSite site, FriendlyByteBuf buf) {
        ((SerializableSite<STTSite>) site.serializer()).writeToNetwork(site, buf);
    }

    @SuppressWarnings("unchecked")
    private static @Nullable STTSite readSite(String apiType, FriendlyByteBuf buf) {
        SerializableSite<? extends Site> serializer = SerializerRegister.getSerializer(ServiceType.STT, apiType);
        if (serializer == null) {
            return null;
        }
        return ((SerializableSite<STTSite>) serializer).fromNetwork(buf);
    }
}
