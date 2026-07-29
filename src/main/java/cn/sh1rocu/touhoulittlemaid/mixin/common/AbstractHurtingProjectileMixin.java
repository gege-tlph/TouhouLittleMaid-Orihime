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

    // 1.21.11: AbstractHurtingProjectile 移至 ...projectile.hurtingprojectile 子包。
    // @At.target 的 owner 必须同步改成新包路径 —— 注解里的字符串不受编译器检查，
    // 只改 import 而漏改字符串会在运行时 Mixin apply 失败（javap -c 确认 invokevirtual 的 owner）。
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
