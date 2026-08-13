package com.github.tartaricacid.touhoulittlemaid.mixin.client;

import com.github.tartaricacid.touhoulittlemaid.api.mixin.ICarryMaidRenderState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 玩家扛着女仆时把两条手臂摆成抱姿。
 *
 * <p>`origin/1.21.1` 与行为基准 `port/1.21.11-fabric` 上都有这个功能；
 * 代码宿主 `origin/26.1` 在迁移期把它整段注释掉、`@Mixin` 靶点改指 `Dummy`、留了 `FIXME`，
 * 也没登记进 `mixins.json`——对行为基准而言是一处回归，本次按 26.1.2 的渲染状态形态复原。</p>
 *
 * <p>与 `origin/1.21.1` 的写法差异是渲染状态重构逼出来的：那边直接问
 * {@code player.getFirstPassenger() instanceof EntityMaid}，而这里 {@code setupAnim} 只拿得到
 * 渲染状态、拿不到实体，故经 {@link ICarryMaidRenderState} 传递（与 1.21.11 同法）。</p>
 */
@Mixin(HumanoidModel.class)
public class HumanoidModelMixin {
    @Shadow
    @Final
    public ModelPart leftArm;
    @Shadow
    @Final
    public ModelPart rightArm;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",
            at = @At("TAIL"))
    private void tlm$poseArmsForCarriedMaid(HumanoidRenderState state, CallbackInfo ci) {
        if (state instanceof ICarryMaidRenderState carryState && carryState.tlm$isCarryingMaid()) {
            leftArm.xRot = (float) Math.toRadians(-65);
            leftArm.yRot = (float) Math.toRadians(10);
            rightArm.xRot = (float) Math.toRadians(-65);
            rightArm.yRot = (float) Math.toRadians(-10);
        }
    }
}
