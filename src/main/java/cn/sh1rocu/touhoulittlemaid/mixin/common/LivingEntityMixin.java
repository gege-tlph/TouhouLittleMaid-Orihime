package cn.sh1rocu.touhoulittlemaid.mixin.common;

import cn.sh1rocu.touhoulittlemaid.api.event.LivingAttackEvent;
import cn.sh1rocu.touhoulittlemaid.util.forge.EventHooks;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    @Shadow
    public abstract ItemStack getUseItem();

    @Shadow
    public abstract int getUseItemRemainingTicks();

    @Shadow
    public abstract void setLastHurtByPlayer(UUID player, int timeToRemember);

    // 1.21.11: 字段类型 Player→EntityReference<Player>，lastHurtByPlayerTime→lastHurtByPlayerMemoryTime
    @Shadow
    @Nullable
    protected EntityReference<Player> lastHurtByPlayer;

    @Shadow
    protected int lastHurtByPlayerMemoryTime;

    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @WrapOperation(method = "completeUsingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"))
    public ItemStack tlm$onItemUseFinish(ItemStack instance, Level level, LivingEntity livingEntity, Operation<ItemStack> original) {
        return EventHooks.onItemUseFinish((LivingEntity) (Object) this, this.getUseItem().copy(), this.getUseItemRemainingTicks(), original.call(instance, level, livingEntity));
    }

    // 女仆攻击完成后，给受伤实体设置最近的玩家伤害归属，便于经验/掉落计算。
    // 1.21.11: HEAD 原注入 hurt 里的 DamageSource.getEntity() 处；hurt 现为 final，归属结算移到
    // resolvePlayerResponsibleForDamage（javap 确认；同 origin/26.1）→ 注入其 HEAD。
    @Inject(method = "resolvePlayerResponsibleForDamage", at = @At("HEAD"))
    private void tlm$hurt(DamageSource source, CallbackInfoReturnable<Player> cir) {
        Entity attacker = source.getEntity();
        if (attacker instanceof EntityMaid maid && maid.isTame()) {
            if (maid.getOwnerReference() != null) {
                this.setLastHurtByPlayer(maid.getOwnerReference().getUUID(), 100);
            } else {
                this.lastHurtByPlayer = null;
                this.lastHurtByPlayerMemoryTime = 0;
            }
        }
    }

    // 1.21.11: hurt 现为 final void；服务端伤害入口为 hurtServer。HEAD 在 hurt HEAD 触发 LivingAttackEvent，
    // 现移至 hurtServer HEAD（服务端等价点，返回 boolean → 匹配 CallbackInfoReturnable<Boolean>）。
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    public void tlm$attackEvent(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player)) {
            LivingAttackEvent event = new LivingAttackEvent(self, source, amount);
            LivingAttackEvent.CALLBACK.invoker().onLivingAttack(event);
            if (event.isCanceled())
                cir.setReturnValue(false);
        }
    }
}
