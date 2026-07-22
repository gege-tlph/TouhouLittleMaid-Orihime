package cn.sh1rocu.touhoulittlemaid.mixin.common;

import cn.sh1rocu.touhoulittlemaid.util.forge.EventHooks;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({AbstractHurtingProjectile.class})
public class AbstractHurtingProjectileMixin {
    public AbstractHurtingProjectileMixin() {
    }

    // 阻止女仆发射的伤害投射物点燃自己的主人。
    @WrapWithCondition(
            method = {"tick"},
            at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/projectile/hurtingprojectile/AbstractHurtingProjectile;hitTargetOrDeflectSelf(Lnet/minecraft/world/phys/HitResult;)Lnet/minecraft/world/entity/projectile/ProjectileDeflection;"
            )}
    )
    private boolean tlm$onProjectileImpact(AbstractHurtingProjectile projectile, HitResult result) {
        return !EventHooks.onProjectileImpact(projectile, result);
    }
}
