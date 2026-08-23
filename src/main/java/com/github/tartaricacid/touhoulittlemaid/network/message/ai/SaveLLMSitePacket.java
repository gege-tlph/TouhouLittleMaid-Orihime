package com.github.tartaricacid.touhoulittlemaid.network.message.ai;

import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.AvailableSites;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.SiteConfigStorage;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.SiteRuntimeActivation;
import com.github.tartaricacid.touhoulittlemaid.command.subcommand.AIChatCommand;
import java.util.Map;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SerializableSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SerializerRegister;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ServiceType;
import com.github.tartaricacid.touhoulittlemaid.ai.service.Site;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMSite;
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

public record SaveLLMSitePacket(Action action, @Nullable String siteId, boolean enabled,
                                @Nullable LLMSite site) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SaveLLMSitePacket> TYPE = new CustomPacketPayload.Type<>(modLoc("save_llm_site"));
    public static final StreamCodec<ByteBuf, SaveLLMSitePacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SaveLLMSitePacket decode(ByteBuf byteBuf) {
            FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
            Action action = Action.valueOf(buf.readUtf());
            String siteId = StringUtils.trimToNull(buf.readUtf());
            boolean enabled = buf.readBoolean();
            LLMSite site = null;

            if (buf.readBoolean()) {
                String apiType = buf.readUtf();
                SerializableSite<LLMSite> serializer = getSerializer(apiType);
                site = serializer == null ? null : serializer.fromNetwork(buf);
            }
            return new SaveLLMSitePacket(action, siteId, enabled, site);
        }

        @Override
        public void encode(ByteBuf byteBuf, SaveLLMSitePacket message) {
            FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
            buf.writeUtf(message.action.name());
            buf.writeUtf(StringUtils.defaultString(message.siteId));
            buf.writeBoolean(message.enabled);

            boolean writeSite = message.site != null && (message.action == Action.CREATE || message.action == Action.UPDATE);
            buf.writeBoolean(writeSite);
            if (writeSite) {
                buf.writeUtf(message.site.getApiType());
                SerializableSite<LLMSite> serializer = getSerializer(message.site.getApiType());
                if (serializer != null) {
                    serializer.writeToNetwork(message.site, buf);
                }
            }
        }
    };

    public static SaveLLMSitePacket create(LLMSite site) {
        return new SaveLLMSitePacket(Action.CREATE, site.id(), site.enabled(), site);
    }

    public static SaveLLMSitePacket update(LLMSite site) {
        return new SaveLLMSitePacket(Action.UPDATE, site.id(), site.enabled(), site);
    }

    public static SaveLLMSitePacket delete(String siteId) {
        return new SaveLLMSitePacket(Action.DELETE, siteId, false, null);
    }

    public static SaveLLMSitePacket toggle(String siteId, boolean enabled) {
        return new SaveLLMSitePacket(Action.TOGGLE, siteId, enabled, null);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SaveLLMSitePacket message, ServerPlayNetworking.Context context) {
        context.server().execute(() -> onHandle(message, context.player()));
    }

    private static void onHandle(SaveLLMSitePacket message, @Nullable ServerPlayer player) {
        if (player == null) {
            return;
        }
        if (!GameModeUtil.canEditSite(player)) {
            return;
        }

        final Map<String, LLMSite> sites;
        try {
            sites = SiteConfigStorage.readLLM();
        } catch (IllegalStateException exception) {
            return;
        }
        // 磁盘上被严格读取跳过的未知站点不在 sites 里，但那个 id **是被占用的**——
        // 只看 sites 判重会让新建的内置站点顶掉别人的扩展配置，装回扩展也回不去
        if (message.action == Action.CREATE && message.site != null
                && SiteConfigStorage.foreignLLMIds().contains(message.site.id())) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                            "ai.touhou_little_maid.chat.settings.hub.site_id_reserved_by_extension",
                            message.site.id())
                    .withStyle(net.minecraft.ChatFormatting.RED));
            return;
        }
        boolean changed = switch (message.action) {
            case CREATE -> createSite(sites, message.site);
            case UPDATE -> updateSite(sites, message.site);
            case DELETE -> deleteSite(sites, message.siteId);
            case TOGGLE -> toggleSite(sites, message.siteId, message.enabled);
        };
        if (!changed) {
            return;
        }

        if (!SiteConfigStorage.writeLLM(sites)) {
            return;
        }
        SiteRuntimeActivation.activate(player);
    }

    private static boolean createSite(Map<String, LLMSite> sites, @Nullable LLMSite site) {
        if (site == null || StringUtils.isBlank(site.id()) || sites.containsKey(site.id())) {
            return false;
        }
        sites.put(site.id(), restoreSecrets(site, null));
        return true;
    }

    /**
     * 更新一个已有站点。**密钥哨兵在这里被换回服务端现有的值。**
     *
     * <p>客户端拿到的密钥永远是哨兵（明文不下行），所以管理员只改地址、根本没碰密钥框时，
     * 回传的还是哨兵——不填回去就等于把密钥清空了。这正是「只改一个 URL 却让 LLM 失效」
     * 那类回归的成因，故填回动作必须紧贴写盘，不能散在调用方。</p>
     */
    private static boolean updateSite(Map<String, LLMSite> sites, @Nullable LLMSite site) {
        if (site == null || StringUtils.isBlank(site.id())) {
            return false;
        }
        sites.put(site.id(), restoreSecrets(site, sites.get(site.id())));
        return true;
    }

    @SuppressWarnings("unchecked")
    private static LLMSite restoreSecrets(LLMSite incoming, @Nullable LLMSite existing) {
        SerializableSite<LLMSite> serializer = (SerializableSite<LLMSite>) incoming.serializer();
        return serializer == null ? incoming : serializer.restoreKeptSecrets(incoming, existing);
    }

    private static boolean deleteSite(Map<String, LLMSite> sites, @Nullable String siteId) {
        if (StringUtils.isBlank(siteId)) {
            return false;
        }
        return sites.remove(siteId) != null;
    }

    private static boolean toggleSite(Map<String, LLMSite> sites, @Nullable String siteId, boolean enabled) {
        if (StringUtils.isBlank(siteId)) {
            return false;
        }
        LLMSite site = sites.get(siteId);
        if (site == null) {
            return false;
        }
        site.setEnabled(enabled);
        return true;
    }

    @SuppressWarnings("unchecked")
    private static @Nullable SerializableSite<LLMSite> getSerializer(String apiType) {
        SerializableSite<? extends Site> serializer = SerializerRegister.getSerializer(ServiceType.LLM, apiType);
        if (serializer == null) {
            return null;
        }
        return (SerializableSite<LLMSite>) serializer;
    }

    public enum Action {
        CREATE,
        UPDATE,
        DELETE,
        TOGGLE
    }
}
