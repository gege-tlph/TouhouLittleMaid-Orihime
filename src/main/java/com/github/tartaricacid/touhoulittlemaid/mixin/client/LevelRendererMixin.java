package com.github.tartaricacid.touhoulittlemaid.mixin.client;

import com.github.tartaricacid.touhoulittlemaid.client.animation.gecko.GeckoUpdateManager;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.RenderContextManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在原版可见实体状态提取期间标记 Gecko 的不可变更新阶段，确保同一帧只读取一致的模型状态。
 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @WrapOperation(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;extractVisibleEntities(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;Lnet/minecraft/client/DeltaTracker;Lnet/minecraft/client/renderer/state/LevelRenderState;)V"
            )
    )
    private void tlm$extractVisibleEntities(LevelRenderer instance, Camera camera, Frustum frustum,
                                            DeltaTracker deltaTracker, LevelRenderState renderState,
                                            Operation<Void> original) {
        RenderContextManager.setRenderingLevel(true);
        try {
            original.call(instance, camera, frustum, deltaTracker, renderState);
            GeckoUpdateManager.updateRemaining(deltaTracker.getGameTimeDeltaPartialTick(true));
        } finally {
            RenderContextManager.setRenderingLevel(false);
        }
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void tlm$finalizeGeckoFrame(CallbackInfo ci) {
        GeckoUpdateManager.finalizeFrame();
    }
}
