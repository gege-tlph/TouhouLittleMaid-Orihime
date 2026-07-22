package com.github.tartaricacid.touhoulittlemaid.mixin.client;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.extended.LivingEntityRendererAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;


@Mixin(LivingEntityRenderer.class)
/**
 * 暴露 EntityRenderer 的基础提交路径，使替换后的 Gecko 渲染器仍能提交名牌等原版渲染内容。
 */
public abstract class LivingEntityRendererMixin extends EntityRenderer<Entity, EntityRenderState> implements LivingEntityRendererAccessor {
    protected LivingEntityRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    @Unique
    @Override
    public void tlm$renderNameTag(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
}
