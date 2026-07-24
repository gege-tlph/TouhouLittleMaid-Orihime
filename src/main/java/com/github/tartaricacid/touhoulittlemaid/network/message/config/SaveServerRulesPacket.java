package com.github.tartaricacid.touhoulittlemaid.network.message.config;

import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.util.GameModeUtil;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import static com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil.modLoc;

public record SaveServerRulesPacket(String rulesJson) implements CustomPacketPayload {
    private static final int MAX_JSON_LENGTH = 1_048_576;
    public static final Type<SaveServerRulesPacket> TYPE = new Type<>(modLoc("save_server_rules"));
    public static final StreamCodec<ByteBuf, SaveServerRulesPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SaveServerRulesPacket decode(ByteBuf byteBuf) {
            return new SaveServerRulesPacket(new FriendlyByteBuf(byteBuf).readUtf(MAX_JSON_LENGTH));
        }

        @Override
        public void encode(ByteBuf byteBuf, SaveServerRulesPacket message) {
            new FriendlyByteBuf(byteBuf).writeUtf(message.rulesJson, MAX_JSON_LENGTH);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SaveServerRulesPacket message, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            if (!GameModeUtil.canEditSite(context.player())) {
                context.player().displayClientMessage(
                        Component.translatable("config.touhou_little_maid.server_rules.save.no_permission")
                                .withStyle(ChatFormatting.RED), false);
                SyncServerRulesPacket.sendTo(context.player());
                return;
            }
            boolean activate = !context.server().isDedicatedServer();
            if (!ServerRuleConfig.applyJson(message.rulesJson, activate)) {
                context.player().displayClientMessage(
                        Component.translatable("config.touhou_little_maid.server_rules.save.invalid")
                                .withStyle(ChatFormatting.RED), false);
                SyncServerRulesPacket.sendTo(context.player());
                return;
            }
            if (activate) {
                SyncServerRulesPacket.syncToAll(context.server());
            } else {
                SyncServerRulesPacket.syncToEditors(context.server());
                context.player().displayClientMessage(
                        Component.translatable("config.touhou_little_maid.server_rules.save.reload_required")
                                .withStyle(ChatFormatting.YELLOW), false);
            }
        });
    }
}
