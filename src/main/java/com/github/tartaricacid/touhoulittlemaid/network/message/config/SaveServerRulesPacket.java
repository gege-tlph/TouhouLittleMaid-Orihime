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

            // §17 v2：一张扁平提交按键归属分拣到两个店。世界规则维持原语义（专服写文件、等
            // /tlm config reload）；AI 规则**任何服务器形态保存即激活**——与站点同权逻辑，
            // 漏跑命令玩家侧毫无线索。两店键名不重叠，未知键丢弃（与旧行为的静默忽略一致）。
            final JsonObject root;
            try {
                root = JsonParser.parseString(message.rulesJson).getAsJsonObject();
            } catch (RuntimeException exception) {
                rejectInvalid(context);
                return;
            }
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

            // 世界半先行：它失败时整包拒绝，AI 半一个字也没落。反向（AI 失败于世界之后）无法回滚
            // 世界半——跨文件没有原子性；界面提交的值都过了本地校验，走到那一步只剩伪造包。
            boolean activateWorld = !context.server().isDedicatedServer();
            if (!worldPart.isEmpty() && !ServerRuleConfig.applyJson(worldPart.toString(), activateWorld)) {
                rejectInvalid(context);
                return;
            }
            boolean aiApplied = false;
            // 激活前先记住四条默认值：变更告知的另一半在 AIChatCommand 的 reload 路径（手改文件），
            // 两条路缺一条都会出现「配置已生效但玩家不知情」——契约测试钉着两处。
            DefaultAiSnapshot defaults = DefaultAiSnapshot.capture();
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
                // 专服的纯世界规则保存只写文件；提示由配置菜单的 server_rules.dedicated_reload 承担。
                SyncServerRulesPacket.syncToEditors(context.server());
            }
            if (aiApplied) {
                defaults.diffAndNotify(context.server());
            }
        });
    }

    private static void rejectInvalid(ServerPlayNetworking.Context context) {
        context.player().displayClientMessage(
                Component.translatable("config.touhou_little_maid.server_rules.save.invalid")
                        .withStyle(ChatFormatting.RED), false);
        SyncServerRulesPacket.sendTo(context.player());
    }
}
