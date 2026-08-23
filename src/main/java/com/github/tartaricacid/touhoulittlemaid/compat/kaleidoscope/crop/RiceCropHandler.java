package com.github.tartaricacid.touhoulittlemaid.compat.kaleidoscope.crop;

import com.github.tartaricacid.touhoulittlemaid.api.task.ISpecialCropHandler;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.crop.SpecialCropManager;
import com.github.ysbbbbbb.kaleidoscopecookery.block.crop.RiceCropBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.NotNull;

public class RiceCropHandler implements ISpecialCropHandler {
    public static void addCropHandlers(SpecialCropManager manager) {
        RiceCropHandler handler = new RiceCropHandler();
        // 本方法在 TLM 的 mod initializer 里跑，而可选兼容不能施加 Fabric 依赖顺序，
        // 因此 TLM 可能先于森罗厨房初始化。在这里直接读这几个常量会连带触发它整个
        // ModItems 的静态初始化，而此时它的 ModEffects.registerEffects() 还没跑——
        // 食物组件会永久捕获空的效果 Holder，随后创造栏/JEI 一遍历该页就 NPE。
        // 交给 SERVER_STARTING 求值：那时所有 mod initializer 都已完成，
        // 且早于任何女仆农作行为查这两张表。
        manager.addLazySeed(() -> ModItems.RICE_SEED, handler);
        manager.addLazySeed(() -> ModItems.WILD_RICE_SEED, handler);
        manager.addLazyCrop(() -> ModBlocks.RICE_CROP, handler);
    }

    @Override
    public boolean isSeed(@NotNull ItemStack stack) {
        // 由于种植特殊，暂时无法种植，只能玩家种植
        return false;
    }

    @Override
    public boolean canHarvest(@NotNull EntityMaid maid, @NotNull BlockPos cropPos, @NotNull BlockState cropState) {
        int location = cropState.getValue(RiceCropBlock.LOCATION);
        return location == RiceCropBlock.DOWN && cropState.getValue(RiceCropBlock.AGE) >= RiceCropBlock.MAX_AGE;
    }

    @Override
    public void harvest(@NotNull EntityMaid maid, @NotNull BlockPos cropPos, @NotNull BlockState cropState, boolean isDestroyMode) {
        // 无视 isDestroyMode，直接收获
        maid.dropResourcesToMaidInv(cropState, maid.level, cropPos, null, maid.getMainHandItem());
        maid.level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, cropPos, Block.getId(cropState));
        // 直接设置 Age 为 0
        cropState = cropState.setValue(RiceCropBlock.AGE, 0);
        maid.level.setBlock(cropPos, cropState, Block.UPDATE_ALL);
        maid.level.gameEvent(maid, GameEvent.BLOCK_CHANGE, cropPos);
    }

    @Override
    public boolean canPlant(@NotNull EntityMaid maid, @NotNull BlockPos basePos, @NotNull BlockState baseState, @NotNull ItemStack seed) {
        // 由于种植特殊，暂时无法种植，只能玩家种植后，女仆右键收获
        return false;
    }
}