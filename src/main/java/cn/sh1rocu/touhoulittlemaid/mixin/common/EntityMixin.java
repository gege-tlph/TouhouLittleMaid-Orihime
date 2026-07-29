package cn.sh1rocu.touhoulittlemaid.mixin.common;

import cn.sh1rocu.touhoulittlemaid.api.extension.IEntity;
import cn.sh1rocu.touhoulittlemaid.util.forge.EventHooks;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    @Shadow
    @Nullable
    private Entity vehicle;

    @Inject(method = "setPosRaw", at = @At("TAIL"))
    private void tlm$setPosRaw(double x, double y, double z, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof IEntity entity && entity.isAddedToLevel() && !self.level.isClientSide() && !self.isRemoved())
            //强加载区块
            self.level.getChunk((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4);
    }

    // 1.21.11: startRiding(Entity,boolean) 已移除，改为 startRiding(Entity,boolean,boolean)
    // （javap 确认：1 参版委托为 startRiding(e, false, true)）。3 参版内部仍调用 canRide(Entity)，
    // 故注入点不变；handler 参数须与新目标签名一致。
    @Inject(
            method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;canRide(Lnet/minecraft/world/entity/Entity;)Z",
                    shift = At.Shift.BEFORE
            ),
            cancellable = true
    )
    public void tlm$startRiding(Entity entity, boolean bl, boolean bl2, CallbackInfoReturnable<Boolean> cir) {
        if (!EventHooks.canMountEntity((Entity) (Object) this, entity, true))
            cir.setReturnValue(false);
    }

    @Inject(method = "removeVehicle", at = @At(value = "CONSTANT", args = "nullValue=true"), cancellable = true)
    public void tlm$removeRidingEntity(CallbackInfo ci) {
        if (!EventHooks.canMountEntity((Entity) (Object) this, this.vehicle, false))
            ci.cancel();
    }
}
