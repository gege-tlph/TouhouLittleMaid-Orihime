package com.github.tartaricacid.touhoulittlemaid.network.message.ai;

import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.AvailableSites;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSConfig;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSSystemServices;
import com.github.tartaricacid.touhoulittlemaid.network.client.ai.TTSSystemAudioToClientPackageProxy;
import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.apache.commons.lang3.tuple.Pair;

import static com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil.modLoc;

public record TTSSystemAudioToClientPackage(String siteName, String chatText, TTSConfig config,
                                            TTSSystemServices services) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TTSSystemAudioToClientPackage> TYPE = new CustomPacketPayload.Type<>(modLoc("tts_system_audio_to_client"));
    public static final StreamCodec<ByteBuf, TTSSystemAudioToClientPackage> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public TTSSystemAudioToClientPackage decode(ByteBuf byteBuf) {
            FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
            String siteName = buf.readUtf();
            TTSSite ttsSite = AvailableSites.getTTSSite(siteName);
            // 客户端站点表可能是空的或缺这一项：解析失败会断开连接，报清楚是哪个站点，
            // 别让它退化成一条什么都不说的 NullPointerException
            if (ttsSite == null) {
                throw new IllegalArgumentException("Unknown TTS site: " + siteName
                        + ", the client site registry has no entry for it");
            }
            if (ttsSite.client() instanceof TTSSystemServices services) {
                Pair<String, TTSConfig> pair = services.readFromNetwork(buf);
                return new TTSSystemAudioToClientPackage(siteName, pair.getLeft(), pair.getRight(), services);
            }
            throw new IllegalArgumentException("Invalid TTS site: " + siteName);
        }

        @Override
        public void encode(ByteBuf byteBuf, TTSSystemAudioToClientPackage message) {
            FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
            buf.writeUtf(message.siteName);
            message.services.writeToNetwork(message.chatText, message.config, buf);
        }
    };

    @Environment(EnvType.CLIENT)
    public static void handle(TTSSystemAudioToClientPackage message, ClientPlayNetworking.Context context) {
        context.client().execute(() -> TTSSystemAudioToClientPackageProxy.handle(message));
    }


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
