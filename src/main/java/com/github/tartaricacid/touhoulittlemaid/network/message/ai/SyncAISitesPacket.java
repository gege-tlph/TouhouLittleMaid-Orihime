package com.github.tartaricacid.touhoulittlemaid.network.message.ai;

import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.AvailableSites;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.SiteConfigStorage;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SerializableSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SerializerRegister;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ServiceType;
import com.github.tartaricacid.touhoulittlemaid.ai.service.Site;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSSite;
import com.github.tartaricacid.touhoulittlemaid.network.client.ai.SyncAISitesPacketProxy;
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

import java.util.Map;

import static com.github.tartaricacid.touhoulittlemaid.util.GameModeUtil.canEditSite;
import static com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil.modLoc;

public record SyncAISitesPacket(Map<String, LLMSite> llmSites,
                                Map<String, TTSSite> ttsSites,
                                boolean insufficientPermissions,
                                boolean openScreen) implements CustomPacketPayload {
    public static final Type<SyncAISitesPacket> TYPE = new Type<>(modLoc("sync_ai_sites"));
    public static final StreamCodec<ByteBuf, SyncAISitesPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SyncAISitesPacket decode(ByteBuf byteBuf) {
            FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
            Map<String, LLMSite> llmSites = readSites(ServiceType.LLM, buf);
            Map<String, TTSSite> ttsSites = readSites(ServiceType.TTS, buf);
            return new SyncAISitesPacket(llmSites, ttsSites, buf.readBoolean(), buf.readBoolean());
        }

        @Override
        public void encode(ByteBuf byteBuf, SyncAISitesPacket message) {
            FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
            writeSites(message.llmSites, buf);
            writeSites(message.ttsSites, buf);
            buf.writeBoolean(message.insufficientPermissions);
            buf.writeBoolean(message.openScreen);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Environment(EnvType.CLIENT)
    public static void handle(SyncAISitesPacket message, ClientPlayNetworking.Context context) {
        context.client().execute(() -> SyncAISitesPacketProxy.handle(message));
    }

    /** Pushes the latest LLM/TTS snapshot without forcing another editor's screen open. */
    public static void syncToSiteEditors(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (canEditSite(player)) {
                ServerPlayNetworking.send(player, new SyncAISitesPacket(
                        SiteConfigStorage.readLLM(), SiteConfigStorage.readTTS(), false, false));
            }
        }
    }

    private static <T extends Site> void writeSites(Map<String, T> sites, FriendlyByteBuf buf) {
        buf.writeInt(sites.size());
        sites.forEach((key, site) -> {
            buf.writeUtf(key);
            buf.writeUtf(site.getApiType());
            writeSiteToNetwork(site, buf);
        });
    }

    private static <T extends Site> Map<String, T> readSites(ServiceType type, FriendlyByteBuf buf) {
        int size = buf.readInt();
        Map<String, T> sites = Maps.newHashMap();
        for (int i = 0; i < size; i++) {
            String key = buf.readUtf();
            T site = readSiteFromNetwork(type, buf.readUtf(), buf);
            if (site != null) {
                sites.put(key, site);
            }
        }
        return sites;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Site> void writeSiteToNetwork(T site, FriendlyByteBuf buf) {
        ((SerializableSite<T>) site.serializer()).writeToNetwork(site, buf);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <T extends Site> T readSiteFromNetwork(ServiceType type, String apiType, FriendlyByteBuf buf) {
        SerializableSite<? extends Site> serializer = SerializerRegister.getSerializer(type, apiType);
        return serializer == null ? null : ((SerializableSite<T>) serializer).fromNetwork(buf);
    }
}
