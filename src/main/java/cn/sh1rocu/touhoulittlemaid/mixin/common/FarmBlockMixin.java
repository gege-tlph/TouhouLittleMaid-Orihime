package cn.sh1rocu.touhoulittlemaid.mixin.common;

import cn.sh1rocu.touhoulittlemaid.util.forge.CommonHooks;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FarmBlock.class)
public class FarmBlockMixin {
    // 1.21.11: FarmBlock.fallOn 的 fallDistance 由 float 变为 double → @Local 需匹配 double；事件 API 仍用 float（下坠距离用 float 精度足够，同 HEAD）→ 强转。
    @WrapWithCondition(method = "fallOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/FarmBlock;turnToDirt(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
    private boolean tlm$onFarmlandTrample(Entity entity, BlockState state, Level level, BlockPos pos, @Local(argsOnly = true) double fallDistance) {
        return CommonHooks.onFarmlandTrample(level, pos, Blocks.DIRT.defaultBlockState(), (float) fallDistance, entity);
    }
}
