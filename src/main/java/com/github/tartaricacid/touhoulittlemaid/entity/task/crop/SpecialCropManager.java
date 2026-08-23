package com.github.tartaricacid.touhoulittlemaid.entity.task.crop;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.task.ISpecialCropHandler;
import com.github.tartaricacid.touhoulittlemaid.compat.kaleidoscope.KaleidoscopeCompat;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class SpecialCropManager {
    private static Map<Item, ISpecialCropHandler> ITEM_SEED_HANDLERS = Maps.newHashMap();
    private static Map<Block, ISpecialCropHandler> BLOCK_CROP_HANDLERS = Maps.newHashMap();
    private static List<DeferredHandler<Item>> LAZY_ITEM_SEED_HANDLERS = Lists.newArrayList();
    private static List<DeferredHandler<Block>> LAZY_BLOCK_CROP_HANDLERS = Lists.newArrayList();

    private SpecialCropManager() {
    }

    public static void init() {
        SpecialCropManager manager = new SpecialCropManager();

        manager.add(Items.NETHER_WART, Blocks.NETHER_WART, new NetherWartCropHandler());
        KaleidoscopeCompat.addCropHandlers(manager);
        for (ILittleMaid littleMaid : TouhouLittleMaid.EXTENSIONS) {
            littleMaid.registerSpecialCropHandler(manager);
        }

        ITEM_SEED_HANDLERS = ImmutableMap.copyOf(ITEM_SEED_HANDLERS);
        BLOCK_CROP_HANDLERS = ImmutableMap.copyOf(BLOCK_CROP_HANDLERS);
        LAZY_ITEM_SEED_HANDLERS = ImmutableList.copyOf(LAZY_ITEM_SEED_HANDLERS);
        LAZY_BLOCK_CROP_HANDLERS = ImmutableList.copyOf(LAZY_BLOCK_CROP_HANDLERS);
        ServerLifecycleEvents.SERVER_STARTING.register(server -> resolveDeferredHandlers());
    }

    public static Map<Item, ISpecialCropHandler> getItemSeedHandlers() {
        return ITEM_SEED_HANDLERS;
    }

    public static Map<Block, ISpecialCropHandler> getBlockCropHandlers() {
        return BLOCK_CROP_HANDLERS;
    }

    public void add(Item seed, Block crop, ISpecialCropHandler handler) {
        ITEM_SEED_HANDLERS.put(seed, handler);
        BLOCK_CROP_HANDLERS.put(crop, handler);
    }

    public void addSeed(Item seed, ISpecialCropHandler handler) {
        ITEM_SEED_HANDLERS.put(seed, handler);
    }

    public void addCrop(Block crop, ISpecialCropHandler handler) {
        BLOCK_CROP_HANDLERS.put(crop, handler);
    }

    /**
     * 可选模组的种子登记通道：只收 supplier，**不在这里求值**。
     *
     * <p>Fabric 对可选兼容不提供初始化先后保证，而急切求值一个第三方常量会连带触发
     * 它整个宿主类的静态初始化——若对方的效果注册还没跑完，它的食物组件就会永久捕获
     * 空的效果 Holder。用 supplier 把求值推到 {@code SERVER_STARTING}。</p>
     */
    public void addLazySeed(Supplier<Item> seed, ISpecialCropHandler handler) {
        LAZY_ITEM_SEED_HANDLERS.add(new DeferredHandler<>(seed, handler));
    }

    /** 见 {@link #addLazySeed}——作物方块侧的同款延迟通道。 */
    public void addLazyCrop(Supplier<Block> crop, ISpecialCropHandler handler) {
        LAZY_BLOCK_CROP_HANDLERS.add(new DeferredHandler<>(crop, handler));
    }

    /**
     * 在所有 mod initializer 跑完之后、任何女仆行为查这两张表之前，一次性求值并发布。
     *
     * <p>延迟项先落表、直接登记项后覆盖，从而保持原有的登记优先级：内置 → 可选兼容 →
     * 扩展回调，**直接登记仍持有最终覆盖权**。农作热路径仍是 O(1) 查表。</p>
     */
    private static void resolveDeferredHandlers() {
        if (!LAZY_ITEM_SEED_HANDLERS.isEmpty()) {
            Map<Item, ISpecialCropHandler> resolvedItems = Maps.newHashMap();
            for (DeferredHandler<Item> deferred : LAZY_ITEM_SEED_HANDLERS) {
                resolvedItems.put(deferred.value().get(), deferred.handler());
            }
            resolvedItems.putAll(ITEM_SEED_HANDLERS);
            ITEM_SEED_HANDLERS = ImmutableMap.copyOf(resolvedItems);
            LAZY_ITEM_SEED_HANDLERS = List.of();
        }
        if (!LAZY_BLOCK_CROP_HANDLERS.isEmpty()) {
            Map<Block, ISpecialCropHandler> resolvedBlocks = Maps.newHashMap();
            for (DeferredHandler<Block> deferred : LAZY_BLOCK_CROP_HANDLERS) {
                resolvedBlocks.put(deferred.value().get(), deferred.handler());
            }
            resolvedBlocks.putAll(BLOCK_CROP_HANDLERS);
            BLOCK_CROP_HANDLERS = ImmutableMap.copyOf(resolvedBlocks);
            LAZY_BLOCK_CROP_HANDLERS = List.of();
        }
    }

    private record DeferredHandler<T>(Supplier<T> value, ISpecialCropHandler handler) {
    }
}