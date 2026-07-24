package com.github.tartaricacid.touhoulittlemaid.network.message.ai;

import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.AvailableSites;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.SiteConfigStorage;
import com.github.tartaricacid.touhoulittlemaid.command.subcommand.AIChatCommand;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import java.util.Map;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SerializableSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SerializerRegister;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ServiceType;
import com.github.tartaricacid.touhoulittlemaid.ai.service.Site;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSSite;
import com.github.tartaricacid.touhoulittlemaid.util.GameModeUtil;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import static com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil.modLoc;

public record SaveTTSSitePacket(Action action, @Nullable String siteId, boolean enabled,
                                @Nullable TTSSite site) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SaveTTSSitePacket> TYPE = new CustomPacketPayload.Type<>(modLoc("save_tts_site"));
    public static final StreamCodec<ByteBuf, SaveTTSSitePacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SaveTTSSitePacket decode(ByteBuf byteBuf) {
            FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
            Action action = Action.valueOf(buf.readUtf());
            String siteId = StringUtils.trimToNull(buf.readUtf());
            boolean enabled = buf.readBoolean();
            TTSSite site = null;

            if (buf.readBoolean()) {
                String apiType = buf.readUtf();
                SerializableSite<TTSSite> serializer = getSerializer(apiType);
                site = serializer == null ? null : serializer.fromNetwork(buf);
            }
            return new SaveTTSSitePacket(action, siteId, enabled, site);
        }

        @Override
        public void encode(ByteBuf byteBuf, SaveTTSSitePacket message) {
            FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
            buf.writeUtf(message.action.name());
            buf.writeUtf(StringUtils.defaultString(message.siteId));
            buf.writeBoolean(message.enabled);

            boolean writeSite = message.site != null && message.action == Action.UPDATE;
            buf.writeBoolean(writeSite);
            if (writeSite) {
                buf.writeUtf(message.site.getApiType());
                SerializableSite<TTSSite> serializer = getSerializer(message.site.getApiType());
                if (serializer != null) {
                    serializer.writeToNetwork(message.site, buf);
                }
            }
        }
    };

    public static SaveTTSSitePacket update(TTSSite site) {
        return new SaveTTSSitePacket(Action.UPDATE, site.id(), site.enabled(), site);
    }

    public static SaveTTSSitePacket toggle(String siteId, boolean enabled) {
        return new SaveTTSSitePacket(Action.TOGGLE, siteId, enabled, null);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SaveTTSSitePacket message, ServerPlayNetworking.Context context) {
        context.server().execute(() -> onHandle(message, context.player()));
    }

    private static void onHandle(SaveTTSSitePacket message, @Nullable ServerPlayer player) {
        if (!GameModeUtil.canEditSite(player)) {
            return;
        }

        final Map<String, TTSSite> sites;
        try {
            sites = SiteConfigStorage.readTTS();
        } catch (IllegalStateException exception) {
            return;
        }
        boolean changed = switch (message.action) {
            case UPDATE -> updateSite(sites, message.site);
            case TOGGLE -> toggleSite(sites, message.siteId, message.enabled);
        };
        if (!changed) {
            return;
        }

        if (!SiteConfigStorage.writeTTS(sites)) {
            return;
        }
        if (player.level().getServer().isDedicatedServer()) {
            SyncAISitesPacket.syncToSiteEditors(player.level().getServer());
            player.displayClientMessage(Component.translatable("config.touhou_little_maid.ai_sites.save.reload_required")
                    .withStyle(ChatFormatting.YELLOW), false);
        } else {
            AIChatCommand.reload(player.level().getServer());
        }
    }

    private static boolean updateSite(Map<String, TTSSite> sites, @Nullable TTSSite site) {
        if (site == null || StringUtils.isBlank(site.id())) {
            return false;
        }
        sites.put(site.id(), site);
        return true;
    }

    private static boolean toggleSite(Map<String, TTSSite> sites, @Nullable String siteId, boolean enabled) {
        if (StringUtils.isBlank(siteId)) {
            return false;
        }
        TTSSite site = sites.get(siteId);
        if (site == null) {
            return false;
        }
        site.setEnabled(enabled);
        return true;
    }

    @SuppressWarnings("unchecked")
    private static @Nullable SerializableSite<TTSSite> getSerializer(String apiType) {
        SerializableSite<? extends Site> serializer = SerializerRegister.getSerializer(ServiceType.TTS, apiType);
        if (serializer == null) {
            return null;
        }
        return (SerializableSite<TTSSite>) serializer;
    }

    public enum Action {
        UPDATE,
        TOGGLE
    }
}
