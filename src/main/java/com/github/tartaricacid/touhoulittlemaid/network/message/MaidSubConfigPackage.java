package com.github.tartaricacid.touhoulittlemaid.network.message;

import com.github.tartaricacid.touhoulittlemaid.entity.data.ConfigData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitBrains;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import static com.github.tartaricacid.touhoulittlemaid.init.InitDataAttachment.CONFIG;
import static com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil.modLoc;

public record MaidSubConfigPackage(int id, ConfigData configData) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MaidSubConfigPackage> TYPE = new CustomPacketPayload.Type<>(modLoc("maid_sub_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MaidSubConfigPackage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, MaidSubConfigPackage::id,
            ConfigData.STREAM_CODEC, MaidSubConfigPackage::configData,
            MaidSubConfigPackage::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MaidSubConfigPackage message, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer sender = context.player();
            Entity entity = sender.level.getEntity(message.id);
            if (entity instanceof EntityMaid maid && maid.isOwnedBy(sender)) {
                apply(maid, message.configData);
            }
        });
    }

    /**
     * 服务端应用一份子配置。威胁响应策略的**变更**是显式玩家指令：
     * 经应战管理器路由（取消当前应战 + 开重入抑制窗口）；其余子配置项不触碰应战状态。
     */
    public static void apply(EntityMaid maid, ConfigData configData) {
        var oldPolicy = maid.getAttachedOrCreate(CONFIG).combatResponsePolicy();
        maid.setAttached(CONFIG, configData);
        if (!configData.isTableFoodAllowed()) {
            // 关掉开关要立刻停下正在进行的偷吃/摆盘，否则她会把手上这一轮走完才生效。
            // ⚠️ 只擦 ACTION，不碰 WALK_TARGET——那是共享槽，此刻可能正被某个
            // 与桌上食物无关的工作行为占着，擦了等于顺手取消别人的移动。
            maid.getBrain().eraseMemory(InitBrains.MAID_EDIBLE_BLOCK_ACTION);
        }
        if (oldPolicy != configData.combatResponsePolicy()) {
            maid.getEmergencyCombatManager().setResponsePolicy(configData.combatResponsePolicy());
        }
    }
}
