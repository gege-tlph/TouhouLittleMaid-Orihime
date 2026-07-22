package cn.sh1rocu.touhoulittlemaid.mixin.common;

import cn.sh1rocu.touhoulittlemaid.api.extension.IRedstoneConnect;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 来自 Porting_Lib
 */
@Mixin(RedStoneWireBlock.class)
public class RedStoneWireBlockMixin {
    @Inject(
            method = "shouldConnectTo(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void tlm$shouldConnectTo(BlockState state, Direction side, CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof IRedstoneConnect connect) {
            // 在这里为 world 和 pos 传递 null 只是为了额外的上游兼容性，没有正确实现它，因为 1. world 和 pos 从未在 Create 中使用 2. 额外的工作 :help_me:
            cir.setReturnValue(connect.tlm$canConnectRedstone(state, null, null, side));
        }
    }
}