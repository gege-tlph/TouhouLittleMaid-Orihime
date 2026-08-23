package cn.sh1rocu.touhoulittlemaid.mixin.client;

import cn.sh1rocu.touhoulittlemaid.api.event.LivingAttackEvent;
import com.github.tartaricacid.touhoulittlemaid.client.entity.GeckoMaidEntity;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        var self = (Entity) (Object) this;
        if (self.level().isClientSide() && self instanceof EntityMaid maid && !maid.hasAttached(GeckoMaidEntity.TYPE)) {
            maid.setAttached(GeckoMaidEntity.TYPE, new GeckoMaidEntity<>(maid));
        }
    }

    // LocalPlayer 不声明客户端 hurt，本地玩家受击走继承的 Entity.hurtClient——LivingAttackEvent 的
    // 客户端触发点挂此，instanceof 守卫保持 LocalPlayer 作用域（与 1.21.11 分支同款，26.1.2 javap 复验）。
    // RemotePlayer 覆写 hurtClient 且不调 super，由 RemotePlayerMixin 单独承接。
    // 客户端不暴露伤害数值，amount 传 0（现存消费方 gun compat 仅读 entity/source）。
    @Inject(method = "hurtClient", at = @At("HEAD"))
    private void tlm$attackEvent(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof LocalPlayer player) {
            LivingAttackEvent.CALLBACK.invoker().onLivingAttack(new LivingAttackEvent(player, source, 0));
        }
    }
}
