package com.github.tartaricacid.touhoulittlemaid.network.message.config;

import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.util.GameModeUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import static com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil.modLoc;

/**
 * 客户端 → 服务器的世界规则提交。**只带改动过的键**，不是整份快照——
 * 这样两个同时开着菜单的管理员改不同字段时不会互相回滚（{@code RuleStagingSessionTest} 钉着）。
 *
 * <p>服务端是唯一权威：权限、JSON 合法性、键归属、每个值的 spec 校验都在这里重做一遍，
 * 任何一条不过就整批拒绝并把权威快照回发给提交者，让界面退回真实值。</p>
 */
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
                // 26.1.2 上 ServerPlayer.displayClientMessage 已不存在；新基一律用 sendSystemMessage，
                // 玩家侧同样是一条聊天消息。
                context.player().sendSystemMessage(
                        Component.translatable("config.touhou_little_maid.server_rules.save.no_permission")
                                .withStyle(ChatFormatting.RED));
                SyncServerRulesPacket.sendTo(context.player());
                return;
            }

            final JsonObject root;
            try {
                root = JsonParser.parseString(message.rulesJson).getAsJsonObject();
            } catch (RuntimeException exception) {
                rejectInvalid(context);
                return;
            }

            // 按键归属分拣，未知键丢弃。行为基准那边这里分拣到「世界规则」与「实例级 AI 规则」两个店；
            // AI 店属审计 §3.C 尚未搬入，故本刀只有一个店。恢复锚点：SyncServerRulesPacket 的类注释。
            JsonObject worldPart = new JsonObject();
            var worldKeys = ServerRuleConfig.jsonKeys();
            root.entrySet().forEach(entry -> {
                if (worldKeys.contains(entry.getKey())) {
                    worldPart.add(entry.getKey(), entry.getValue());
                }
            });
            if (worldPart.isEmpty()) {
                rejectInvalid(context);
                return;
            }

            // 专服上保存只写文件、不激活，要等 /tlm config reload；单人/局域网保存即生效。
            // 这条差异是有意的：专服的运行期值切换会影响所有在线玩家，交给管理员显式触发。
            boolean activate = !context.server().isDedicatedServer();
            if (!ServerRuleConfig.applyJson(worldPart.toString(), activate)) {
                rejectInvalid(context);
                return;
            }

            if (activate) {
                SyncServerRulesPacket.syncToAll(context.server());
            } else {
                // 纯写文件的那条路只回发给编辑者：其余玩家的运行期值没有变，发了反而是噪音。
                // 「保存了但还没生效」的提示由菜单的 server_rules.dedicated_reload 承担。
                SyncServerRulesPacket.syncToEditors(context.server());
            }
        });
    }

    private static void rejectInvalid(ServerPlayNetworking.Context context) {
        context.player().sendSystemMessage(
                Component.translatable("config.touhou_little_maid.server_rules.save.invalid")
                        .withStyle(ChatFormatting.RED));
        SyncServerRulesPacket.sendTo(context.player());
    }
}
