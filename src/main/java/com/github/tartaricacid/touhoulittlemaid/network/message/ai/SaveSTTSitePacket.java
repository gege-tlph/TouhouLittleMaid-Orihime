package com.github.tartaricacid.touhoulittlemaid.network.message.ai;

import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.AvailableSites;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.SiteConfigStorage;
import com.github.tartaricacid.touhoulittlemaid.command.subcommand.AIChatCommand;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SerializableSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SerializerRegister;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ServiceType;
import com.github.tartaricacid.touhoulittlemaid.ai.service.Site;
import com.github.tartaricacid.touhoulittlemaid.ai.service.stt.STTSite;
import com.github.tartaricacid.touhoulittlemaid.network.message.config.SyncServerRulesPacket;
import com.github.tartaricacid.touhoulittlemaid.config.ServerSTTApiType;
import com.github.tartaricacid.touhoulittlemaid.util.GameModeUtil;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.Map;

import static com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil.modLoc;

public record SaveSTTSitePacket(@Nullable STTSite site) implements CustomPacketPayload {
    public static final Type<SaveSTTSitePacket> TYPE = new Type<>(modLoc("save_stt_site"));
    public static final StreamCodec<ByteBuf, SaveSTTSitePacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SaveSTTSitePacket decode(ByteBuf byteBuf) {
            FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
            if (!buf.readBoolean()) {
                return new SaveSTTSitePacket(null);
            }
            SerializableSite<STTSite> serializer = getSerializer(buf.readUtf());
            return new SaveSTTSitePacket(serializer == null ? null : serializer.fromNetwork(buf));
        }

        @Override
        public void encode(ByteBuf byteBuf, SaveSTTSitePacket message) {
            FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
            STTSite site = message.site;
            SerializableSite<STTSite> serializer = site == null ? null : getSerializer(site.getApiType());
            buf.writeBoolean(serializer != null);
            if (serializer != null) {
                buf.writeUtf(site.getApiType());
                serializer.writeToNetwork(site, buf);
            }
        }
    };

    public static SaveSTTSitePacket update(STTSite site) {
        return new SaveSTTSitePacket(site);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SaveSTTSitePacket message, ServerPlayNetworking.Context context) {
        context.server().execute(() -> save(message, context.player()));
    }

    private static void save(SaveSTTSitePacket message, ServerPlayer player) {
        STTSite site = message.site;
        if (!GameModeUtil.canEditSite(player) || !isAllowedServerSite(site)) {
            return;
        }
        site.setEnabled(true);
        final Map<String, STTSite> sites;
        try {
            sites = SiteConfigStorage.readSTT();
        } catch (IllegalStateException exception) {
            return;
        }
        sites.put(site.id(), site);
        if (!SiteConfigStorage.writeSTT(sites)) {
            return;
        }
        if (player.level().getServer().isDedicatedServer()) {
            SyncServerRulesPacket.syncToEditors(player.level().getServer());
            player.displayClientMessage(Component.translatable("config.touhou_little_maid.ai_sites.save.reload_required")
                    .withStyle(ChatFormatting.YELLOW), false);
        } else {
            AIChatCommand.reload(player.level().getServer());
        }
    }

    private static boolean isAllowedServerSite(@Nullable STTSite site) {
        if (site == null || StringUtils.isBlank(site.id())) {
            return false;
        }
        for (ServerSTTApiType type : ServerSTTApiType.values()) {
            if (type.siteId().equals(site.id()) && type.apiType().getName().equals(site.getApiType())) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static @Nullable SerializableSite<STTSite> getSerializer(String apiType) {
        SerializableSite<? extends Site> serializer = SerializerRegister.getSerializer(ServiceType.STT, apiType);
        return serializer == null ? null : (SerializableSite<STTSite>) serializer;
    }
}
