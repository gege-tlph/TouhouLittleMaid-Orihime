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

@Mixin(HumanoidModel.class)
public class HumanoidModelMixin {
    @Shadow
    @Final
    public ModelPart leftArm;
    @Shadow
    @Final
    public ModelPart rightArm;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void tlm$poseArmsForCarriedMaid(HumanoidRenderState state, CallbackInfo ci) {
        if (state instanceof ICarryMaidRenderState carryState && carryState.tlm$isCarryingMaid()) {
            leftArm.xRot = (float) Math.toRadians(-65);
            leftArm.yRot = (float) Math.toRadians(10);
            rightArm.xRot = (float) Math.toRadians(-65);
            rightArm.yRot = (float) Math.toRadians(-10);
        }
    }
}
