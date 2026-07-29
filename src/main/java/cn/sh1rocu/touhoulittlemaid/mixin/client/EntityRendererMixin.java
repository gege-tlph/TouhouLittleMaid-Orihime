package cn.sh1rocu.touhoulittlemaid.mixin.client;

import cn.sh1rocu.touhoulittlemaid.api.mixin.IEntityRenderStatePartialTick;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Carries the frame partial tick into vanilla's render-state snapshot.  The
 * vendored Gecko renderer consumes the snapshot after entity extraction, so
 * reading Minecraft's timer later would associate it with the wrong frame.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void tlm$storePartialTick(T entity, S state, float partialTick, CallbackInfo ci) {
        ((IEntityRenderStatePartialTick) state).tlm$setPartialTick(partialTick);
    }
}
