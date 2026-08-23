package com.github.tartaricacid.touhoulittlemaid.mixin.client;

import com.github.tartaricacid.touhoulittlemaid.api.mixin.ICarryMaidRenderState;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 抽取渲染状态时把「正扛着女仆」记下来，供 {@link HumanoidModelMixin} 摆姿势用。
 *
 * <p>26.1.2 的 {@code AvatarRenderer} 是泛型
 * {@code <AvatarlikeEntity extends Avatar & ClientAvatarEntity>}，
 * 但泛型擦除取第一个上界，javap 实查描述符仍是
 * {@code (Lnet/minecraft/world/entity/Avatar;…AvatarRenderState;F)V}，
 * 与行为基准一致；另两个同名重载是父类的桥接方法，写全描述符即可精确命中。</p>
 */
@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"))
    private void tlm$extractCarriedMaid(Avatar avatar, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        ((ICarryMaidRenderState) state).tlm$setCarryingMaid(avatar.getFirstPassenger() instanceof EntityMaid);
    }
}
