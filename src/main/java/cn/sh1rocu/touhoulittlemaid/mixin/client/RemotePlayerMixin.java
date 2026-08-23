package cn.sh1rocu.touhoulittlemaid.mixin.client;

import cn.sh1rocu.touhoulittlemaid.api.event.LivingAttackEvent;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RemotePlayer.class)
public abstract class RemotePlayerMixin {
    // RemotePlayer 覆写 hurtClient(DamageSource) 且不调 super（26.1.2 javap 复验），
    // 客户端 LivingAttackEvent 触发点单独承接；伤害数值客户端不暴露，amount 传 0
    //（现存消费方 gun compat 仅读 entity/source）。
    @Inject(method = "hurtClient", at = @At("HEAD"))
    public void tlm$attackEvent(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        LivingAttackEvent event = new LivingAttackEvent((LivingEntity) (Object) this, source, 0);
        LivingAttackEvent.CALLBACK.invoker().onLivingAttack(event);
    }
}
