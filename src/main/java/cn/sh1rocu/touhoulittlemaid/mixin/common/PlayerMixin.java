package cn.sh1rocu.touhoulittlemaid.mixin.common;

import cn.sh1rocu.touhoulittlemaid.api.event.LivingAttackEvent;
import cn.sh1rocu.touhoulittlemaid.util.forge.EventHooks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {
    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void tlm$playerStartTickEvent(CallbackInfo ci) {
        EventHooks.firePlayerTickPre((Player) (Object) this);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void tlm$playerEndTickEvent(CallbackInfo ci) {
        EventHooks.firePlayerTickPost((Player) (Object) this);
    }

    // 1.21.11: hurt 现为 final void；玩家服务端伤害入口 Player.hurtServer（javap 确认 Player 覆盖之）。
    // HEAD 在 hurt HEAD 触发 LivingAttackEvent，现移至 hurtServer HEAD（服务端等价点）。
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    public void tlm$attackEvent(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingAttackEvent event = new LivingAttackEvent(this, source, amount);
        LivingAttackEvent.CALLBACK.invoker().onLivingAttack(event);
        if (event.isCanceled())
            cir.setReturnValue(false);
    }
}
