package com.github.tartaricacid.touhoulittlemaid.network.message.ai;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

import static com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil.modLoc;

/**
 * 「检查配置」的结果（S2C）。
 *
 * <p>检查本身跑在服务端——密钥只在那一侧，见 {@link CheckSiteConfigPackage}。结果原先由服务端
 * {@code displayClientMessage} 打进聊天栏，而**发起检查的那个界面此刻正开着，聊天栏被它挡住**：
 * 管理员点完按钮屏幕上什么也没有。回执必须回到发起它的那个屏。</p>
 *
 * <p>传的是翻译键与参数而不是成品 {@code Component}：客户端按自己的语言渲染，管理员和玩家
 * 各看各的语言。参数带长度上限——{@code unreachable} 那条会把底层异常原文塞进来。</p>
 */
public record SiteCheckResultPackage(String messageKey, List<String> args, int argb) implements CustomPacketPayload {
    public static final Type<SiteCheckResultPackage> TYPE = new Type<>(modLoc("site_check_result"));

    private static final int MAX_KEY = 128;
    private static final int MAX_ARG = 256;
    private static final int MAX_ARGS = 4;

    public static final StreamCodec<ByteBuf, SiteCheckResultPackage> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SiteCheckResultPackage decode(ByteBuf byteBuf) {
            FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
            String key = buf.readUtf(MAX_KEY);
            int count = Math.min(buf.readVarInt(), MAX_ARGS);
            List<String> args = Lists.newArrayListWithCapacity(count);
            for (int i = 0; i < count; i++) {
                args.add(buf.readUtf(MAX_ARG));
            }
            return new SiteCheckResultPackage(key, args, buf.readInt());
        }

        @Override
        public void encode(ByteBuf byteBuf, SiteCheckResultPackage message) {
            FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
            buf.writeUtf(message.messageKey, MAX_KEY);
            List<String> args = message.args.size() > MAX_ARGS ? message.args.subList(0, MAX_ARGS) : message.args;
            buf.writeVarInt(args.size());
            for (String arg : args) {
                buf.writeUtf(truncate(arg), MAX_ARG);
            }
            buf.writeInt(message.argb);
        }
    };

    private static String truncate(String value) {
        return value.length() <= MAX_ARG ? value : value.substring(0, MAX_ARG);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Environment(EnvType.CLIENT)
    public static void handle(SiteCheckResultPackage message, ClientPlayNetworking.Context context) {
        context.client().execute(() -> com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.editor
                .SiteCheckResultDisplay.dispatch(message.messageKey,
                        Component.translatable(message.messageKey, message.args.toArray()), message.argb));
    }
}
