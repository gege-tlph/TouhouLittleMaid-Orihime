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

    public void addLazySeed(Supplier<Item> seed, ISpecialCropHandler handler) {
        LAZY_ITEM_SEED_HANDLERS.add(new DeferredHandler<>(seed, handler));
    }

    public void addLazyCrop(Supplier<Block> crop, ISpecialCropHandler handler) {
        LAZY_BLOCK_CROP_HANDLERS.add(new DeferredHandler<>(crop, handler));
    }

    private static void resolveDeferredHandlers() {
        if (!LAZY_ITEM_SEED_HANDLERS.isEmpty()) {
            Map<Item, ISpecialCropHandler> resolvedItems = Maps.newHashMap();
            for (DeferredHandler<Item> deferred : LAZY_ITEM_SEED_HANDLERS) {
                resolvedItems.put(deferred.value().get(), deferred.handler());
            }
            // Keep the original registration order: built-ins, optional compatibility, then
            // extension callbacks. Direct registrations therefore retain final override rights.
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
