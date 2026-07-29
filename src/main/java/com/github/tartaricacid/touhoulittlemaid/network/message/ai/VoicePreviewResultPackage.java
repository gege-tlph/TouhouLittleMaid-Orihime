package com.github.tartaricacid.touhoulittlemaid.network.message.ai;

import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import static com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil.modLoc;

/**
 * 音色试听的结果（S2C）。
 *
 * <p>{@code requestId} 让客户端丢弃过期结果——玩家连点两次试听，先发的那次回来时不该出声。
 * {@code detail} 承载失败原因：以本 mod lang 前缀开头的按 translatable 处理（已知失败，如凭据缺失），
 * 否则按原文塞进「试听失败：%s」（HTTP 层的真实报错，对配置站点的管理员有用）。</p>
 */
public record VoicePreviewResultPackage(int requestId, Status status, String detail,
                                        byte[] audio) implements CustomPacketPayload {
    public static final Type<VoicePreviewResultPackage> TYPE = new Type<>(modLoc("voice_preview_result"));
    private static final int MAX_DETAIL = 512;

    public enum Status {
        OK,
        FAILED,
        UNAVAILABLE
    }

    public static final StreamCodec<ByteBuf, VoicePreviewResultPackage> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public VoicePreviewResultPackage decode(ByteBuf byteBuf) {
            FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
            int requestId = buf.readVarInt();
            Status status = buf.readEnum(Status.class);
            String detail = buf.readUtf(MAX_DETAIL);
            byte[] audio = buf.readByteArray();
            return new VoicePreviewResultPackage(requestId, status, detail, audio);
        }

        @Override
        public void encode(ByteBuf byteBuf, VoicePreviewResultPackage message) {
            FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
            buf.writeVarInt(message.requestId);
            buf.writeEnum(message.status);
            buf.writeUtf(message.detail, MAX_DETAIL);
            buf.writeByteArray(message.audio);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Environment(EnvType.CLIENT)
    public static void handle(VoicePreviewResultPackage message, ClientPlayNetworking.Context context) {
        context.client().execute(() ->
                com.github.tartaricacid.touhoulittlemaid.client.sound.VoicePreviewClient.handleResult(message));
    }
}
