package com.github.tartaricacid.touhoulittlemaid.network.message.ai;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
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

    /**
     * Pushes the latest LLM/TTS snapshot without forcing another editor's screen open.
     *
     * <p>{@link SiteConfigStorage} 读的是<b>严格</b>版本：文件损坏且 {@code .last-good} 也不可用时抛异常。
     * 那是编辑器该有的语义——不能让管理员在残缺数据上编辑——但异常必须在这里接住。调用方
     * {@code AIChatCommand.reload} 在这一行之后还要同步世界规则、并向管理员返回「部分站点文件加载失败」
     * 的回执；让异常穿过去会把这两件事一起吃掉，而三个 {@code Save*SitePacket} 早就各自 catch 了它。</p>
     *
     * <p>读取提到循环外：每位可编辑者各读一遍文件，只是在重复解析同一份 JSON。</p>
     */
    public static void syncToSiteEditors(MinecraftServer server) {
        Map<String, LLMSite> llmSites;
        Map<String, TTSSite> ttsSites;
        try {
            llmSites = SiteConfigStorage.readLLM();
            ttsSites = SiteConfigStorage.readTTS();
        } catch (IllegalStateException exception) {
            // 宁可不刷新编辑器，也不能推一份残缺清单过去——那会让管理员以为站点真的没了
            TouhouLittleMaid.LOGGER.warn("Skipping AI site sync to editors, site config is unreadable: {}",
                    exception.getMessage());
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (canEditSite(player)) {
                ServerPlayNetworking.send(player, new SyncAISitesPacket(llmSites, ttsSites, false, false));
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

    /**
     * **下行一律脱敏**：已配置的密钥换成哨兵，明文永不离开服务端。
     *
     * <p>哨兵同时告诉客户端「这一项已配置」，所以不需要再并行下发一个 hasSecret 布尔；
     * 而客户端原样回传哨兵时，服务端会把真实值填回去（见 {@code Save*SitePacket.updateSite}），
     * 于是「不改密钥」成了默认行为，而不是需要客户端主动表达的特例。</p>
     */
    @SuppressWarnings("unchecked")
    private static <T extends Site> void writeSiteToNetwork(T site, FriendlyByteBuf buf) {
        ((SerializableSite<T>) site.serializer()).writeRedactedToNetwork(site, buf);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <T extends Site> T readSiteFromNetwork(ServiceType type, String apiType, FriendlyByteBuf buf) {
        SerializableSite<? extends Site> serializer = SerializerRegister.getSerializer(type, apiType);
        return serializer == null ? null : ((SerializableSite<T>) serializer).fromNetwork(buf);
    }
}
