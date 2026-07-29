package com.github.tartaricacid.touhoulittlemaid.network.message;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ItemPickupParticle;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.CompletableFuture;

import static com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil.modLoc;

public record TeleportItemParticlePackage(
        int entityId, BlockPos chestPos, ItemStack itemStack, boolean chestToMaid, int delayTicks
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TeleportItemParticlePackage> TYPE = new CustomPacketPayload.Type<>(modLoc("teleport_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TeleportItemParticlePackage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TeleportItemParticlePackage::entityId,
            BlockPos.STREAM_CODEC, TeleportItemParticlePackage::chestPos,
            ItemStack.STREAM_CODEC, TeleportItemParticlePackage::itemStack,
            ByteBufCodecs.BOOL, TeleportItemParticlePackage::chestToMaid,
            ByteBufCodecs.VAR_INT, TeleportItemParticlePackage::delayTicks,
            TeleportItemParticlePackage::new
    );

    public static void handle(TeleportItemParticlePackage message, ClientPlayNetworking.Context context) {
        context.client().execute(() -> clientHandle(message));
    }

    // B7b: 还原 HEAD 的 @Environment(CLIENT) 内联模式（移植期外提的 proxy 已 P5 排除）。逻辑不变。
    @Environment(EnvType.CLIENT)
    private static void clientHandle(TeleportItemParticlePackage message) {
        if (message.delayTicks() <= 0) {
            handleSpawnParticle(message);
        } else {
            CompletableFuture.runAsync(() -> handleSpawnParticleDelay(message, message.delayTicks()), Util.backgroundExecutor());
        }
    }

    @Environment(EnvType.CLIENT)
    private static void handleSpawnParticleDelay(TeleportItemParticlePackage message, int delayTicks) {
        try {
            Thread.sleep(delayTicks * 50L);
            Minecraft.getInstance().submitAsync(() -> handleSpawnParticle(message));
        } catch (InterruptedException e) {
            TouhouLittleMaid.LOGGER.error(e.getMessage());
        }
    }

    @Environment(EnvType.CLIENT)
    private static void handleSpawnParticle(TeleportItemParticlePackage message) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        Entity e = level.getEntity(message.entityId());
        if (!(e instanceof EntityMaid maid) || !e.isAlive()) {
            return;
        }

        level.playLocalSound(
                maid.getX(), maid.getY(), maid.getZ(),
                SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.2F,
                (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 1.4F + 2.0F,
                false
        );

        float eyeHeight = maid.getEyeHeight(maid.getPose()) / 2f;

        Vec3 fromPos;
        Vec3 toPos;
        if (message.chestToMaid()) {
            fromPos = maid.position().add(0, eyeHeight, 0);
            toPos = Vec3.atCenterOf(message.chestPos());
        } else {
            fromPos = Vec3.atCenterOf(message.chestPos());
            toPos = maid.position().add(0, eyeHeight, 0);
        }

        ItemEntity fromEntity = new ItemEntity(level, fromPos.x, fromPos.y, fromPos.z, ItemStack.EMPTY);
        ItemEntity toEntity = new ItemEntity(level, toPos.x, toPos.y, toPos.z, message.itemStack());
        toEntity.setId(-1);

        EntityRenderState itemState = mc.getEntityRenderDispatcher().extractEntity(toEntity, 1.0F);
        mc.particleEngine.add(new ItemPickupParticle(level, itemState, fromEntity, toPos.subtract(fromPos)));
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
