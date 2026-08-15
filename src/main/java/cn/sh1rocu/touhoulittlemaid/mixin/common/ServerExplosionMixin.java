package cn.sh1rocu.touhoulittlemaid.mixin.common;

import cn.sh1rocu.touhoulittlemaid.api.event.ExplosionEvents;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * {@code ExplosionEvents.DETONATE} 的触发点。
 *
 * <p>origin/1.21.1 挂在 {@code Explosion.explode} 的局部变量捕获上；26.1.2 爆炸重构后
 * {@code Explosion} 是接口、实体伤害在 {@link ServerExplosion#hurtEntities()} 私有方法内，
 * 实体表来自 {@code this.level.getEntities(this.source, AABB)}（javap -c 实查
 * invokevirtual ServerLevel.getEntities:(Entity,AABB)List）。包住这一取表调用，
 * 把可变列表交给监听器——从列表移除实体即等价于对它免疫本次爆炸，与 origin 行为一致。
 * diameter 语义照 origin 的 {@code radius * 2}（唯一消费者 GunHurtMaidEvent 不读它）。</p>
 */
@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin {
    @Shadow
    @Final
    private ServerLevel level;

    @Shadow
    @Final
    private float radius;

    @WrapOperation(
            method = "hurtEntities",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"
            )
    )
    private List<Entity> tlm$onDetonate(ServerLevel instance, Entity source, AABB area, Operation<List<Entity>> original) {
        List<Entity> entities = original.call(instance, source, area);
        ExplosionEvents.DETONATE.invoker().onDetonate(this.level, (Explosion) (Object) this, entities, this.radius * 2.0F);
        return entities;
    }
}
