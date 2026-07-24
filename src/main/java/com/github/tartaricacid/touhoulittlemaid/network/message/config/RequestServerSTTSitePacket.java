package com.github.tartaricacid.touhoulittlemaid.network.message.config;

import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import static com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil.modLoc;

/** Requests the currently active server-paid STT credential on demand. */
public record RequestServerSTTSitePacket() implements CustomPacketPayload {
    public static final RequestServerSTTSitePacket INSTANCE = new RequestServerSTTSitePacket();
    public static final Type<RequestServerSTTSitePacket> TYPE = new Type<>(modLoc("request_server_stt_site"));
    public static final StreamCodec<ByteBuf, RequestServerSTTSitePacket> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestServerSTTSitePacket message, ServerPlayNetworking.Context context) {
        context.server().execute(() -> SyncServerSTTSitePacket.sendTo(context.player()));
    }
}
