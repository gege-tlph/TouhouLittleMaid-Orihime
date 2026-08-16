package com.github.tartaricacid.touhoulittlemaid.network.message.config;

import com.github.tartaricacid.touhoulittlemaid.config.AiServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.config.DefaultAiSnapshot;
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

            // 按键归属分拣到两个店，未知键丢弃。键名两店不重叠，故顺序只影响可读性。
            JsonObject worldPart = new JsonObject();
            JsonObject aiPart = new JsonObject();
            var aiKeys = AiServerRuleConfig.jsonKeys();
            var worldKeys = ServerRuleConfig.jsonKeys();
            root.entrySet().forEach(entry -> {
                if (aiKeys.contains(entry.getKey())) {
                    aiPart.add(entry.getKey(), entry.getValue());
                } else if (worldKeys.contains(entry.getKey())) {
                    worldPart.add(entry.getKey(), entry.getValue());
                }
            });
            if (worldPart.isEmpty() && aiPart.isEmpty()) {
                rejectInvalid(context);
                return;
            }

            // 专服上世界规则保存只写文件、不激活，要等 /tlm config reload；单人/局域网保存即生效。
            // 这条差异是有意的：专服的运行期值切换会影响所有在线玩家，交给管理员显式触发。
            // **AI 店没有这条差异**：它保存即激活，专服也一样（见 AiServerRuleConfig 的类注释）。
            //
            // 世界半先行：它失败时整包拒绝，AI 半一个字也没落。反向（AI 失败于世界之后）无法回滚
            // 世界半——跨文件没有原子性；界面提交的值都过了本地校验，走到那一步只剩伪造包。
            boolean activateWorld = !context.server().isDedicatedServer();
            if (!worldPart.isEmpty() && !ServerRuleConfig.applyJson(worldPart.toString(), activateWorld)) {
                rejectInvalid(context);
                return;
            }

            // 激活前先记住四条默认值：变更告知的另一半在 AIChatCommand 的 reload 路径（手改文件），
            // 两条路缺一条都会出现「配置已生效但玩家不知情」。
            DefaultAiSnapshot defaults = DefaultAiSnapshot.capture();
            boolean aiApplied = false;
            if (!aiPart.isEmpty()) {
                if (!AiServerRuleConfig.applyJson(aiPart.toString())) {
                    rejectInvalid(context);
                    return;
                }
                aiApplied = true;
            }

            if (aiApplied || (!worldPart.isEmpty() && activateWorld)) {
                SyncServerRulesPacket.syncToAll(context.server());
            } else {
                // 专服的纯世界规则保存只写文件，回发只给编辑者：其余玩家的运行期值没有变，发了反而是噪音。
                // 「保存了但还没生效」的提示由菜单的 server_rules.dedicated_reload 承担。
                SyncServerRulesPacket.syncToEditors(context.server());
            }
            if (aiApplied) {
                defaults.diffAndNotify(context.server());
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
