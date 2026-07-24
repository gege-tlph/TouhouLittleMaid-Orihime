package com.github.tartaricacid.touhoulittlemaid.network.message.ai;

import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.AvailableSites;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.SiteConfigStorage;
import com.github.tartaricacid.touhoulittlemaid.util.GameModeUtil;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;

import static com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil.modLoc;

public record OpenAIConfigPacket() implements CustomPacketPayload {
    public static final Type<OpenAIConfigPacket> TYPE = new Type<>(modLoc("open_ai_config"));
    public static final StreamCodec<ByteBuf, OpenAIConfigPacket> STREAM_CODEC = StreamCodec.unit(new OpenAIConfigPacket());

    public static void sendToServer() {
        ClientPlayNetworking.send(new OpenAIConfigPacket());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenAIConfigPacket message, ServerPlayNetworking.Context context) {
        context.server().execute(() -> sendSites(context.player()));
    }

    private static void sendSites(ServerPlayer player) {
        boolean canEdit = GameModeUtil.canEditSite(player);
        ServerPlayNetworking.send(player, new SyncAISitesPacket(
                canEdit ? SiteConfigStorage.readLLM() : Collections.emptyMap(),
                canEdit ? SiteConfigStorage.readTTS() : Collections.emptyMap(),
                !canEdit,
                true));
    }
}
