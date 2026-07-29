package com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.layer;

import com.github.tartaricacid.simplebedrockmodel.client.bedrock.model.BedrockPart;
import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.EntityMaidModel;
import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.SimpleBedrockModel;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.EntityMaidRenderer;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.state.EntityMaidRenderState;
import com.github.tartaricacid.touhoulittlemaid.client.resource.bedrock.InternalBedrockModelRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

import static com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid.MOD_ID;
import static com.github.tartaricacid.touhoulittlemaid.client.resource.bedrock.InternalBedrockModelRegistry.MAID_BANNER;
import static net.minecraft.client.renderer.blockentity.BannerRenderer.submitPatterns;
import static net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;


public class LayerMaidBanner extends RenderLayer<EntityMaidRenderState, EntityMaidModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MOD_ID, "textures/bedrock/entity/maid_banner.png");
    private final SimpleBedrockModel<Unit> root;
    private final MaterialSet sprites;
    private final BannerFlagModel flag;

    public LayerMaidBanner(EntityMaidRenderer renderer, EntityRendererProvider.Context context) {
        super(renderer);
        this.root = InternalBedrockModelRegistry.getModel(MAID_BANNER);
        this.flag = new BannerFlagModel(context.bakeLayer(ModelLayers.STANDING_BANNER_FLAG));
        this.sprites = context.getMaterials();
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNode, int light, EntityMaidRenderState state, float yRot, float xRot) {
        if (state.backBanner != null && state.modelInfo.isShowBackpack()) {
            poseStack.pushPose();

            // 依据 root 模型的位移对整体进行物品进行偏移、旋转和缩放
            if (this.getParentModel().root() instanceof BedrockPart part) {
                part.translateAndRotateBedrock(poseStack);
            }

            poseStack.translate(0, -0.25, 0.25);
            poseStack.scale(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(Axis.XN.rotationDegrees(5));

            // 杆子
            RenderType renderType = RenderTypes.entityCutoutNoCull(TEXTURE);
            submitNode.submitModel(
                    root, Unit.INSTANCE, poseStack, renderType, light,
                    NO_OVERLAY, state.outlineColor, null
            );

            BannerPatternLayers patterns = state.backBanner.patterns;
            DyeColor baseColor = state.backBanner.baseColor;
            // Vanilla 1.21.11 passes the opaque banner base material here.
            // submitPatterns itself selects Sheets.BANNER_BASE for the tinted
            // pattern layer; passing that atlas material as the base as well
            // makes the flag inherit its transparent pixels after an Iris
            // pipeline/resource reload until the client is restarted.
            Material baseMaterial = ModelBakery.BANNER_BASE;

            // 旗帜图案：1.21.11 的 submitPatterns 同时渲染底色旗面 + 图案（含 baseMaterial），
            // 故 26.1 的独立 flag submitModel（已删的 SpriteId/SpriteGetter 10-arg 重载）折叠进此调用（同 2c gecko Banner）。
            poseStack.mulPose(Axis.YN.rotationDegrees(90));
            poseStack.translate(0.75, 0.2, 0.1);
            submitPatterns(
                    sprites, poseStack, submitNode, light, NO_OVERLAY, flag,
                    0f, baseMaterial, true, baseColor, patterns, false, null, state.outlineColor
            );

            poseStack.popPose();
        }
    }
}
