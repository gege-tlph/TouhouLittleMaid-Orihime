package com.github.tartaricacid.touhoulittlemaid.client.renderer.entity;

import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.SimpleBedrockEntityModel;
import com.github.tartaricacid.touhoulittlemaid.client.resource.bedrock.InternalBedrockModelRegistry;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.VanillaConfig;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
// 26.1.2：CameraRenderState 挪进 state.level 子包
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.Slime;

public class EntityYukkuriSlimeRender extends MobRenderer<Slime, SlimeRenderState, SimpleBedrockEntityModel<SlimeRenderState>> {
    private static final Identifier TEXTURE = IdentifierUtil.modLoc("textures/bedrock/entity/reimu_yukkuri.png");
    private final SlimeRenderer vanillaRender;

    public EntityYukkuriSlimeRender(EntityRendererProvider.Context context) {
        super(context, InternalBedrockModelRegistry.getEntityModel(InternalBedrockModelRegistry.REIMU_YUKKURI), 0.25F);
        this.vanillaRender = new SlimeRenderer(context);
    }

    @Override
    public SlimeRenderState createRenderState() {
        return new SlimeRenderState();
    }

    @Override
    public void extractRenderState(Slime slime, SlimeRenderState state, float partialTicks) {
        // 完整委托原版提取，squish/size 与全部 LivingEntity 状态字段和原版一字不差
        this.vanillaRender.extractRenderState(slime, state, partialTicks);
    }

    @Override
    public void submit(SlimeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (VanillaConfig.REPLACE_SLIME_MODEL.get()) {
            super.submit(state, poseStack, collector, camera);
        } else {
            this.vanillaRender.submit(state, poseStack, collector, camera);
        }
    }

    @Override
    protected float getShadowRadius(SlimeRenderState state) {
        return 0.25F * state.size;
    }

    @Override
    protected void scale(SlimeRenderState state, PoseStack poseStack) {
        poseStack.scale(0.999F, 0.999F, 0.999F);
        poseStack.translate(0.0F, 0.001F, 0.0F);
        float slimeSize = state.size;
        float tmp = state.squish / (slimeSize * 0.5F + 1.0F);
        float scale = 1.0F / (tmp + 1.0F);
        poseStack.scale(scale * slimeSize, 1.0F / scale * slimeSize, scale * slimeSize);
    }

    @Override
    public Identifier getTextureLocation(SlimeRenderState state) {
        return TEXTURE;
    }
}
