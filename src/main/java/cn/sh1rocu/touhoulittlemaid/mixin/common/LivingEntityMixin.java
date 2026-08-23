package cn.sh1rocu.touhoulittlemaid.mixin.common;

import cn.sh1rocu.touhoulittlemaid.api.event.LivingAttackEvent;
import cn.sh1rocu.touhoulittlemaid.util.neoforge.EventHooks;
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
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    @Shadow
    public abstract void setLastHurtByPlayer(UUID player, int timeToRemember);

    @Shadow
    public abstract ItemStack getUseItem();

    @Shadow
    public abstract int getUseItemRemainingTicks();

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

    // 女仆攻击完成后，调用setLastHurtByPlayer，便于经验掉落等计算
    @Inject(
            method = "resolvePlayerResponsibleForDamage",
            at = @At("HEAD")
    )
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

    // 服务端伤害入口 hurtServer HEAD 触发 LivingAttackEvent（Forge 形态壳，消费者：TACZ 兼容层
    // 「女仆子弹不伤主人/队友」）。玩家由 PlayerMixin 单独承接，此处排除避免双发。
    // 注入点与 1.21.11 分支同款，26.1.2 javap 复验 hurtServer(ServerLevel,DamageSource,float) 仍在。
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
