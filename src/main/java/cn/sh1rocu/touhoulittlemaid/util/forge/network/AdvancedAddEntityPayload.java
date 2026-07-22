package cn.sh1rocu.touhoulittlemaid.util.forge.network;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.ApiStatus;
// 来自 NeoForge && PortingLib

/**
 * 可以从服务器发送到客户端以使用自定义数据向世界添加实体的有效负载。
 *
 * @param entityId 要添加的实体的 ID。
 * @param customPayload 要添加的实体的自定义数据。
 */
@ApiStatus.Internal
public record AdvancedAddEntityPayload(int entityId, byte[] customPayload) implements CustomPacketPayload {
    public static final Type<AdvancedAddEntityPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "advanced_add_entity"));
    public static final StreamCodec<FriendlyByteBuf, AdvancedAddEntityPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            AdvancedAddEntityPayload::entityId,
            ByteBufCodecs.BYTE_ARRAY,
            AdvancedAddEntityPayload::customPayload,
            AdvancedAddEntityPayload::new);

    public AdvancedAddEntityPayload(Entity e) {
        this(e.getId(), writeCustomData(e));
    }

    private static byte[] writeCustomData(final Entity entity) {
        if (!(entity instanceof IEntityWithComplexSpawn additionalSpawnData)) {
            return new byte[0];
        }

        final RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), entity.registryAccess());
        try {
            additionalSpawnData.writeSpawnData(buf);
            return buf.array();
        } finally {
            buf.release();
        }
    }

    @Environment(EnvType.CLIENT)
    public static void handle(AdvancedAddEntityPayload message, ClientPlayNetworking.Context context) {
        context.client().execute(() -> {
            try {
                Entity entity = context.player().level().getEntity(message.entityId());
                if (entity instanceof IEntityWithComplexSpawn entityAdditionalSpawnData) {
                    final RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(message.customPayload()), entity.registryAccess());
                    try {
                        entityAdditionalSpawnData.readSpawnData(buf);
                    } finally {
                        buf.release();
                    }
                }
            } catch (Throwable t) {
                TouhouLittleMaid.LOGGER.error("Failed to handle advanced add entity from server.", t);
                context.responseSender().disconnect(Component.literal("Failed to send AdvancedAddEntityPayload to client"));
            }
        });
    }

    @Override
    public Type<AdvancedAddEntityPayload> type() {
        return TYPE;
    }
}
