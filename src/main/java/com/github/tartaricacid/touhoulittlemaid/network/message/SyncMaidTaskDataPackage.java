package com.github.tartaricacid.touhoulittlemaid.network.message;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;

import static com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil.modLoc;

/**
 * 携带以前存储在同步实体数据字段中的任务数据复合物。这使用标准命名空间自定义有效负载协议，因此代理可以在不了解自定义实体元数据序列化器的情况下转发它。
 */
public record SyncMaidTaskDataPackage(int entityId, CompoundTag taskData) implements CustomPacketPayload {
    public static final Type<SyncMaidTaskDataPackage> TYPE = new Type<>(modLoc("sync_maid_task_data"));
    public static final StreamCodec<ByteBuf, SyncMaidTaskDataPackage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncMaidTaskDataPackage::entityId,
            ByteBufCodecs.COMPOUND_TAG, SyncMaidTaskDataPackage::taskData,
            SyncMaidTaskDataPackage::new
    );

    public static void handle(SyncMaidTaskDataPackage message, ClientPlayNetworking.Context context) {
        context.client().execute(() -> apply(message));
    }

    @Environment(EnvType.CLIENT)
    private static void apply(SyncMaidTaskDataPackage message) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        Entity entity = level.getEntity(message.entityId);
        if (entity instanceof EntityMaid maid) {
            maid.readTaskDataFromServer(message.taskData);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
