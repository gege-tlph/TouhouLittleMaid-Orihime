package com.github.tartaricacid.touhoulittlemaid.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityBroom;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(
            method = "positionRider(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity$MoveFunction;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    protected void positionRider(Entity passenger, Entity.MoveFunction callback, CallbackInfo ci) {
        if (passenger instanceof EntityMaid maid && maid.getVehicle() instanceof Player player) {
            Vec3 position = player.position();
            float radians = (float) -Math.toRadians(player.yBodyRot);
            Vec3 offset = position.add(new Vec3(0, 0, 0.75).yRot(radians));
            double yOffset = 0.15;
            if (player.isDescending()) {
                yOffset = yOffset - 0.3;
            }
            callback.accept(passenger, offset.x(), offset.y() + yOffset, offset.z());
            ci.cancel();
        }
    }

    /**
     * 修改为在 Entity.move() 中 collide() 调用的前后设置 inPhysicalCheck 标志
     * 以避免其它 mod 对 collide() 的修改导致 inPhysicalCheck 永久卡在 true 的问题
     */
    @Inject(
            method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;")
    )
    private void beforeCollide(MoverType type, Vec3 movement, CallbackInfo ci) {
        if ((Object) this instanceof EntityBroom broom) {
            broom.inPhysicalCheck = true;
        }
    }

    @Inject(
            method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", shift = At.Shift.AFTER)
    )
    private void afterCollide(MoverType type, Vec3 movement, CallbackInfo ci) {
        if ((Object) this instanceof EntityBroom broom) {
            broom.inPhysicalCheck = false;
        }
    }

    @ModifyReturnValue(method = "getBoundingBox", at = @At("RETURN"))
    private AABB onGetBoundingBox(AABB original) {
        if ((Object) this instanceof EntityBroom broom && broom.inPhysicalCheck) {
            return broom.getPhysicalBoundingBox(original);
        }
        return original;
    }

    /**
     * 让女仆能骑乘不可序列化的实体——用鞍抱起女仆（她骑到玩家身上）依赖这一点。
     *
     * <p>Minecraft 1.21.11 在 {@code Entity.startRiding(Entity, boolean, boolean)} 里新增了一道闸：
     * {@code !level().isClientSide() && !vehicle.getType().canSerialize()} 成立就直接返回 false。
     * {@code EntityType.PLAYER} 构建时带 {@code noSave()}，{@code canSerialize()} 恒为 false，
     * 于是抱起女仆在客户端成功、在服务端被拒，两端状态从此不一致：服务端她仍站在原地，
     * 放下时服务端位置回写客户端，表现为女仆瞬移回抱起前的位置或悬在原地。
     *
     * <p>该分支是 1.21.11 新增的，Minecraft 1.21.1 的同一方法没有它，因此这不是原版行为变更的
     * 合理后果，而是版本迁移带来的回归；放行女仆是为了恢复 1.21.1 的表现。
     *
     * <p>只判断乘客是不是女仆，不额外要求载具必须是玩家：除玩家外可能还有别的实体类型在构建时
     * 调用了 {@code noSave()}，而 1.21.1 对任何载具都没有这道闸，限定成玩家反而比基准更严。
     */
    @WrapOperation(
            method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;canSerialize()Z")
    )
    private boolean allowMaidToRide(EntityType<?> instance, Operation<Boolean> original) {
        if ((Object) this instanceof EntityMaid) {
            return true;
        }
        return original.call(instance);
    }
}
