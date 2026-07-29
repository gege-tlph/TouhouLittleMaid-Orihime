package com.github.tartaricacid.touhoulittlemaid.util;

import cn.sh1rocu.touhoulittlemaid.api.event.EntityJoinLevelEvent;
import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.client.entity.GeckoMaidEntity;
import com.github.tartaricacid.touhoulittlemaid.entity.backpack.BackpackManager;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityChair;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

@Environment(EnvType.CLIENT)
public final class EntityCacheUtil {
    // [Codex] Preview entities must not share the vanilla default id (0), especially for Gecko update keys.
    private static final AtomicInteger PREVIEW_ENTITY_ID = new AtomicInteger(-10000);
    /**
     * 实体缓存，在客户端会大量运用实体渲染，这个缓存可以减少重复创建实体带来的性能问题
     */
    public static final Cache<EntityType<?>, Entity> ENTITY_CACHE = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build();
    /**
     * 女仆实体缓存，用于雕像，因为雕像如果共用一个实体，会导致 GeckoLib 动画渲染错误
     */
    public static final Cache<Long, EntityMaid> STATUE_CACHE = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.SECONDS).build();
    /**
     * 女仆实体缓存，用于物品形态的手办，因为如果共用一个实体，会导致 GeckoLib 动画渲染错误
     */
    public static final Cache<ItemStack, EntityMaid> GARAGE_KIT_CACHE = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.SECONDS).build();
    private static ResourceKey<Level> dimAt;

    public static EntityMaid getMaid(Level level, EntitySpawnReason reason) {
        return getEntity(EntityMaid.TYPE, (l, ignored) -> new EntityMaid(l), level, reason);
    }

    public static EntityChair getChair(Level level, EntitySpawnReason reason) {
        return getEntity(EntityChair.TYPE, (l, ignored) -> new EntityChair(l), level, reason);
    }

    @SuppressWarnings("unchecked")
    public static <E extends Entity> E getEntity(EntityType<E> type,
                                                  BiFunction<Level, EntitySpawnReason, E> fallback,
                                                  Level level, EntitySpawnReason reason) {
        try {
            return (E) ENTITY_CACHE.get(type, () -> assignPreviewId(
                    Objects.requireNonNullElseGet(type.create(level, reason), () -> fallback.apply(level, reason))));
        } catch (ExecutionException e) {
            TouhouLittleMaid.LOGGER.error("Failed to create preview entity", e);
            return assignPreviewId(fallback.apply(level, reason));
        }
    }

    public static EntityMaid getMaidInStatue(long key, Level level) throws ExecutionException {
        return STATUE_CACHE.get(key, () -> assignPreviewId(new EntityMaid(level)));
    }

    public static EntityMaid getMaidInGarageKit(ItemStack stack, Level level) throws ExecutionException {
        return GARAGE_KIT_CACHE.get(stack, () -> assignPreviewId(new EntityMaid(level)));
    }

    private static <E extends Entity> E assignPreviewId(E entity) {
        entity.setId(PREVIEW_ENTITY_ID.getAndDecrement());
        return entity;
    }

    public static void clearMaidDataResidue(EntityMaid maid, boolean clearEquipmentData) {
        GeckoMaidEntity<?> gecko = maid.getAttached(GeckoMaidEntity.TYPE);
        if (gecko != null) {
            gecko.waitForAsyncUpdate();
        }
        maid.hurtDuration = 0;
        maid.hurtTime = 0;
        maid.deathTime = 0;
        maid.xRotO = 0;
        maid.yRotO = 0;
        maid.yBodyRotO = 0;
        maid.yHeadRotO = 0;
        maid.setXRot(0);
        maid.setYRot(0);
        maid.setYBodyRot(0);
        maid.setYHeadRot(0);
        maid.setOnGround(true);
        maid.setInSittingPoseWithoutPlayerCommand(false);
        maid.setMaidBackpackType(BackpackManager.getEmptyBackpack());
        maid.setCustomName(Component.empty());
        if (clearEquipmentData) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                maid.setItemSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    public static void onChangeDim(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() && event.getEntity() == Minecraft.getInstance().player) {
            ResourceKey<Level> dim = event.getEntity().level.dimension();
            if (!dim.equals(dimAt)) {
                dimAt = dim;
                EntityCacheUtil.ENTITY_CACHE.invalidateAll();
            }
        }
    }
}
