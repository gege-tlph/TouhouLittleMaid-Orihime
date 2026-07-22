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
 * 将当前帧的插值量写入原版渲染状态快照。
 * 内置 Gecko 渲染器会在实体状态提取完成后使用该快照；若稍后再读取客户端计时器，可能会取得另一帧的插值量。
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void tlm$storePartialTick(T entity, S state, float partialTick, CallbackInfo ci) {
        ((IEntityRenderStatePartialTick) state).tlm$setPartialTick(partialTick);
    }
}
