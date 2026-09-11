package com.github.tartaricacid.touhoulittlemaid.network.message;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import static com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil.modLoc;

/**
 * C2S：玩家在 YSM 模型选择界面（{@code OpenYsmMaidScreenEvent} 打开的那个）选定了模型/材质，
 * 请求把这只女仆切到（或切走）YSM 身体接管渲染。
 * <p>
 * 结构与语义照抄同目录 {@link MaidModelPackage}（TLM 自己既有的"切模型"C2S 包），
 * 只是目标字段换成 YSM 三元组。{@code modelId} 为空串表示"切回 TLM 自身模型"——
 * 对应 {@code EntityMaid#setIsYsmModel(false)}，这一步是 TLM 26.1.2 首次实现该功能，
 * 不是移植自哪条既有分支：{@code port/1.21.11-fabric} 分支的教训（见其 d604795e0）是
 * 切回 TLM 模型时若不清 isYsmModel 标记会卡在接管路径上，此处直接照那个教训写。
 */
public record YsmMaidModelPackage(int id, String modelId, String textureName, String displayName) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<YsmMaidModelPackage> TYPE = new CustomPacketPayload.Type<>(modLoc("ysm_maid_model"));
    public static final StreamCodec<ByteBuf, YsmMaidModelPackage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            YsmMaidModelPackage::id,
            ByteBufCodecs.STRING_UTF8,
            YsmMaidModelPackage::modelId,
            ByteBufCodecs.STRING_UTF8,
            YsmMaidModelPackage::textureName,
            ByteBufCodecs.STRING_UTF8,
            YsmMaidModelPackage::displayName,
            YsmMaidModelPackage::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(YsmMaidModelPackage message, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer sender = context.player();
            Entity entity = sender.level.getEntity(message.id);
            if (!(entity instanceof EntityMaid maid) || !maid.isOwnedBy(sender)) {
                return;
            }
            if (message.modelId.isEmpty()) {
                maid.setIsYsmModel(false);
                return;
            }
            maid.setYsmModelId(message.modelId);
            maid.setYsmModelTexture(message.textureName);
            maid.setYsmModelName(message.displayName);
            maid.setIsYsmModel(true);
        });
    }
}
