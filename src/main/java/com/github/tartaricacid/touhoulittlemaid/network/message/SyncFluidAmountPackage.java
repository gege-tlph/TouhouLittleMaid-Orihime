package com.github.tartaricacid.touhoulittlemaid.network.message;

import com.github.tartaricacid.touhoulittlemaid.inventory.container.backpack.TankBackpackContainer;
import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;

/**
 * 液体背包储罐量的 S2C 同步：储罐变化时发给主人、开 GUI 时发给打开者，
 * 客户端只在正开着储罐容器时消费（行为基准同款）。流体种类另走实体数据
 * {@code BACKPACK_FLUID}（GUI 渲染要在没有包到达时也画得出流体贴图）。
 */
public record SyncFluidAmountPackage(long amount) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncFluidAmountPackage> TYPE = new CustomPacketPayload.Type<>(IdentifierUtil.modLoc("client_sync_fluid_amount"));
    public static final StreamCodec<ByteBuf, SyncFluidAmountPackage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG,
            SyncFluidAmountPackage::amount,
            SyncFluidAmountPackage::new
    );

    @Environment(EnvType.CLIENT)
    public static void handle(SyncFluidAmountPackage message, ClientPlayNetworking.Context context) {
        context.client().execute(() -> {
            if (context.player().containerMenu instanceof TankBackpackContainer tankBackpackContainer) {
                tankBackpackContainer.setClientFluidCount(message.amount);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
