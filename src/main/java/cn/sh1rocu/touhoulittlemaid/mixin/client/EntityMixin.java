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
    // 客户端实体生成时为女仆赋予 GeckoMaidEntity 附件（EntityMaidRenderer.getGeckoEntity 读取它 → GECKO 渲染路径）。
    // 适配：3e-substrate 后 GeckoMaidEntity 的 ctor 为单参 GeckoMaidEntity(EntityMaid)，故直接判 EntityMaid（其 T extends EntityMaid）。
    @Inject(method = "<init>", at = @At("TAIL"))
    private void tlm$attachGeckoMaid(CallbackInfo ci) {
        var self = (Entity) (Object) this;
        if (self.level().isClientSide() && self instanceof EntityMaid maid && !maid.hasAttached(GeckoMaidEntity.TYPE)) {
            maid.setAttached(GeckoMaidEntity.TYPE, new GeckoMaidEntity<>(maid));
        }
    }

    // 1.21.11：LocalPlayer 不再声明客户端 hurt（1.21.1 的 LocalPlayer.hurt 已删），本地玩家受击走继承的
    // Entity.hurtClient——原 LocalPlayerMixin 的 LivingAttackEvent 触发点迁此，instanceof 守卫保持原作用域。
    // RemotePlayer 覆写 hurtClient 且不调 super，由 RemotePlayerMixin 单独承接。
    // 1.21.11 客户端不再暴露伤害数值，amount 传 0（现存消费方 gun compat 仅读 entity/source）。
    @Inject(method = "hurtClient", at = @At("HEAD"))
    private void tlm$attackEvent(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof LocalPlayer player) {
            LivingAttackEvent.CALLBACK.invoker().onLivingAttack(new LivingAttackEvent(player, source, 0));
        }
    }
}
