package com.github.tartaricacid.touhoulittlemaid.network.message.config;

import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.AvailableSites;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SerializableSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SerializerRegister;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ServiceType;
import com.github.tartaricacid.touhoulittlemaid.ai.service.Site;
import com.github.tartaricacid.touhoulittlemaid.ai.service.stt.STTSite;
import com.github.tartaricacid.touhoulittlemaid.config.ServerConfig;
import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.config.ServerSTTApiType;
import com.github.tartaricacid.touhoulittlemaid.network.client.config.ServerRulesClientCache;
import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import static com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil.modLoc;

/** Carries at most one active server-paid STT credential and is never persisted client-side. */
public record SyncServerSTTSitePacket(String siteId, @Nullable STTSite site)
        implements CustomPacketPayload {
    private static final int MAX_ID_LENGTH = 64;
    public static final Type<SyncServerSTTSitePacket> TYPE = new Type<>(modLoc("sync_server_stt_site"));
    public static final StreamCodec<ByteBuf, SyncServerSTTSitePacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SyncServerSTTSitePacket decode(ByteBuf byteBuf) {
            FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
            if (!buf.readBoolean()) {
                return new SyncServerSTTSitePacket("", null);
            }
            String siteId = buf.readUtf(MAX_ID_LENGTH);
            String apiType = buf.readUtf(MAX_ID_LENGTH);
            SerializableSite<STTSite> serializer = getSerializer(apiType);
            if (serializer == null) {
                throw new IllegalArgumentException("Unknown server STT site type: " + apiType);
            }
            return new SyncServerSTTSitePacket(siteId, serializer.fromNetwork(buf));
        }

        @Override
        public void encode(ByteBuf byteBuf, SyncServerSTTSitePacket message) {
            FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
            SerializableSite<STTSite> serializer =
                    message.site == null ? null : getSerializer(message.site.getApiType());
            buf.writeBoolean(serializer != null);
            if (serializer != null) {
                buf.writeUtf(message.siteId, MAX_ID_LENGTH);
                buf.writeUtf(message.site.getApiType(), MAX_ID_LENGTH);
                serializer.writeToNetwork(message.site, buf);
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Environment(EnvType.CLIENT)
    public static void handle(SyncServerSTTSitePacket message, ClientPlayNetworking.Context context) {
        context.client().execute(() -> ServerRulesClientCache.updateServerSttSite(message));
    }

    public static void sendTo(ServerPlayer player) {
        if (!ServerRuleConfig.get(ServerConfig.PROVIDE_SERVER_STT)) {
            ServerPlayNetworking.send(player, new SyncServerSTTSitePacket("", null));
            return;
        }
        ServerSTTApiType type = ServerRuleConfig.get(ServerConfig.SERVER_STT_TYPE);
        STTSite site = AvailableSites.getSTTSite(type.siteId());
        ServerPlayNetworking.send(player, new SyncServerSTTSitePacket(type.siteId(), site));
    }

    @SuppressWarnings("unchecked")
    private static @Nullable SerializableSite<STTSite> getSerializer(String apiType) {
        SerializableSite<? extends Site> serializer =
                SerializerRegister.getSerializer(ServiceType.STT, apiType);
        return serializer == null ? null : (SerializableSite<STTSite>) serializer;
    }
}
