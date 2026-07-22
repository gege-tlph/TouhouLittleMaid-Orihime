package cn.sh1rocu.touhoulittlemaid.mixin.common;

import cn.sh1rocu.touhoulittlemaid.util.forge.EventHooks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 来源：Porting Lib（ProjectileImpactEvent）
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin extends Entity {
    public AbstractArrowMixin(EntityType<?> variant, Level world) {
        super(variant, world);
    }


    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
    private void tlm$onProjectileImpact(EntityHitResult result, CallbackInfo ci) {
        if (EventHooks.onProjectileImpact((AbstractArrow) (Object) this, result)) {
            ci.cancel();
        }
    }
}
