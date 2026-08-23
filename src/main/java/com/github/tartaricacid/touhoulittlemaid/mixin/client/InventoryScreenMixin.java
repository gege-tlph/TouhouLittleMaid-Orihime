package com.github.tartaricacid.touhoulittlemaid.mixin.client;

import com.github.tartaricacid.touhoulittlemaid.client.entity.GeckoMaidEntity;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityChair;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.RenderContextManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Supplies the inventory render context while vanilla extracts an entity preview.
 * <p>
 * The Gecko update task must NOT be started during extraction: vanilla writes the
 * mouse-follow angles (bodyRot/yRot/xRot) into the state only after
 * {@code extractRenderState} returns, so an async task started earlier races those
 * writes and intermittently renders the world pose inside the GUI (probe-confirmed
 * one-frame pose snaps, worst at the preview boundary). Matching the 26.1.2 branch,
 * the task is started at RETURN of {@code renderEntityInInventoryFollowsMouse},
 * after the angles are in place. {@code GeckoAsyncTask.getResult()} would also run
 * an unstarted task lazily at submit time, so the RETURN start is purely to keep
 * the update off the render thread.
 */
@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {
    @WrapOperation(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"
            )
    )
    private static EntityRenderState tlm$extractInventoryRenderState(EntityRenderer<?, ?> instance,
                                                                      Entity entity, float partialTick,
                                                                      Operation<EntityRenderState> original) {
        RenderContextManager.setRenderingInInventory(true);
        try {
            return original.call(instance, entity, partialTick);
        } finally {
            RenderContextManager.setRenderingInInventory(false);
        }
    }

    @Inject(method = "renderEntityInInventoryFollowsMouse", at = @At("RETURN"))
    private static void tlm$startGeckoTaskAfterAngles(GuiGraphics guiGraphics, int x0, int y0, int x1, int y1,
                                                      int scale, float yOffset, float mouseX, float mouseY,
                                                      LivingEntity entity, CallbackInfo ci) {
        if (entity instanceof EntityMaid maid) {
            var animatable = maid.getAttached(GeckoMaidEntity.TYPE);
            if (animatable != null && animatable.getLastUpdateTask() != null) {
                animatable.getLastUpdateTask().start();
            }
        } else if (entity instanceof EntityChair chair) {
            var animatable = chair.getAnimatableEntity();
            if (animatable != null && animatable.getLastUpdateTask() != null) {
                animatable.getLastUpdateTask().start();
            }
        }
    }
}
