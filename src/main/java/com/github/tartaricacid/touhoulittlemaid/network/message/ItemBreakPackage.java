package com.github.tartaricacid.touhoulittlemaid.network.message;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import static com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil.modLoc;

public record ItemBreakPackage(int id, ItemStack item) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ItemBreakPackage> TYPE = new CustomPacketPayload.Type<>(modLoc("item_break"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemBreakPackage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ItemBreakPackage::id,
            ItemStack.STREAM_CODEC,
            ItemBreakPackage::item,
            ItemBreakPackage::new
    );

    public static void handle(ItemBreakPackage message, ClientPlayNetworking.Context context) {
        context.client().execute(() -> breakItem(message));
    }

    // B7b: 还原 HEAD 的 @Environment(CLIENT) 内联模式（移植期外提的 proxy 已 P5 排除）。逻辑不变。
    @Environment(EnvType.CLIENT)
    private static void breakItem(ItemBreakPackage message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Entity e = mc.level.getEntity(message.id());
        if (e instanceof LivingEntity livingEntity && livingEntity.isAlive()) {
            livingEntity.breakItem(message.item());
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
