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

// Porting Lib（ProjectileImpactEvent）
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin extends Entity {
    public AbstractArrowMixin(EntityType<?> variant, Level world) {
        super(variant, world);
    }

    // 1.21.11 迁移：AbstractArrow 把命中逻辑从 tick() 重构进私有 stepMoveAndHit/hitTargetsOrDeflectSelf →
    //   原「WrapOperation tick 里的 hitTargetOrDeflectSelf」无目标。改为注入 protected onHitEntity(EntityHitResult) HEAD 可取消：
    //   触发 ProjectileImpactEvent，若被取消（EntityHurtEvent.onArrowImpact：主人友军/同队/配置忽略实体的远程免伤）则跳过命中伤害。
    //   onArrowImpact 只处理 EntityHitResult（实体命中）→ 覆盖 onHitEntity 即覆盖唯一消费方；方块命中不触发亦无行为变化。
    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
    private void tlm$onProjectileImpact(EntityHitResult result, CallbackInfo ci) {
        if (EventHooks.onProjectileImpact((AbstractArrow) (Object) this, result)) {
            ci.cancel();
        }
    }
}
