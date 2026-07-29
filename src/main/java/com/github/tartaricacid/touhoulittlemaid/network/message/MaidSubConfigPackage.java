package com.github.tartaricacid.touhoulittlemaid.network.message;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.MaidConfigManager;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import static com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil.modLoc;

// B8: 移植期误抄 26.1 版（用 26.1 专有的 entity/data/ConfigData + InitDataAttachment.CONFIG，二者均未创建 → 悬空引用，编译崩）。
//   HEAD 用 MaidConfigManager.SyncNetwork（当前树存在，EntityMaid 在用）。按方法论还原 HEAD 形态（getResourceLocation→modLoc，context.player() 用当前 Fabric 形态）。
public record MaidSubConfigPackage(int id, MaidConfigManager.SyncNetwork syncNetwork) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MaidSubConfigPackage> TYPE = new CustomPacketPayload.Type<>(modLoc("maid_sub_config"));
    public static final StreamCodec<ByteBuf, MaidSubConfigPackage> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(ByteBuf buffer, MaidSubConfigPackage message) {
            FriendlyByteBuf buf = new FriendlyByteBuf(buffer);
            buf.writeVarInt(message.id);
            MaidConfigManager.SyncNetwork.encode(message.syncNetwork, buf);
        }

        @Override
        public MaidSubConfigPackage decode(ByteBuf buffer) {
            FriendlyByteBuf buf = new FriendlyByteBuf(buffer);
            int entityId = buf.readVarInt();
            MaidConfigManager.SyncNetwork network = MaidConfigManager.SyncNetwork.decode(buf);
            return new MaidSubConfigPackage(entityId, network);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MaidSubConfigPackage message, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer sender = context.player();
            Entity entity = sender.level.getEntity(message.id);
            if (entity instanceof EntityMaid maid && maid.isOwnedBy(sender)) {
                MaidConfigManager.SyncNetwork.handle(message.syncNetwork, maid);
            }
        });
    }
}
