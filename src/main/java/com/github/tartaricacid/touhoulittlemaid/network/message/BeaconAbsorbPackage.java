package com.github.tartaricacid.touhoulittlemaid.network.message;

import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityPowerPoint;
import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import static com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil.modLoc;

public record BeaconAbsorbPackage(float x, float y, float z) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BeaconAbsorbPackage> TYPE = new CustomPacketPayload.Type<>(modLoc("beacon_absorb"));
    public static final StreamCodec<ByteBuf, BeaconAbsorbPackage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT,
            BeaconAbsorbPackage::x,
            ByteBufCodecs.FLOAT,
            BeaconAbsorbPackage::y,
            ByteBufCodecs.FLOAT,
            BeaconAbsorbPackage::z,
            BeaconAbsorbPackage::new
    );

    public static void handle(BeaconAbsorbPackage message, ClientPlayNetworking.Context context) {
        context.client().execute(() -> spawnParticle(message));
    }

    // B7b: 还原 HEAD 的 @Environment(CLIENT) 内联模式（移植期外提的 network/client/*Proxy 已 P5 排除）。
    //   逻辑与 HEAD 逐字一致，仅带入 proxy 已做的 1.21.11 API 迁移（level.random→getRandom()）。服务端由 Fabric 剥离本方法。
    @Environment(EnvType.CLIENT)
    private static void spawnParticle(BeaconAbsorbPackage message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            EntityPowerPoint.spawnExplosionParticle(mc.level, message.x(), message.y(), message.z(), mc.level.getRandom());
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
