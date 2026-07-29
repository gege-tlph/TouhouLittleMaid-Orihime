package com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.gecko.layer;

import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.SimpleBedrockModel;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.gecko.GeckoMaidRenderData;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.state.EntityMaidRenderState;
import com.github.tartaricacid.touhoulittlemaid.client.resource.bedrock.InternalBedrockModelRegistry;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.GeoLayerRenderer;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.render.built.GeoLocatorType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
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

public class GeckoLayerMaidBanner implements GeoLayerRenderer<EntityMaidRenderState, GeckoMaidRenderData> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MOD_ID, "textures/bedrock/entity/maid_banner.png");
    private final SimpleBedrockModel<Unit> banner;
    private final MaterialSet sprites;
    private final BannerFlagModel flag;

    public GeckoLayerMaidBanner(EntityRendererProvider.Context context) {
        this.banner = InternalBedrockModelRegistry.getModel(MAID_BANNER);
        this.flag = new BannerFlagModel(context.bakeLayer(ModelLayers.STANDING_BANNER_FLAG));
        this.sprites = context.getMaterials();
    }

    @Override
    public void submit(SubmitNodeCollector submitNode, PoseStack poseStack, EntityMaidRenderState state,
                       GeckoMaidRenderData data, CameraRenderState camera) {
        if (state.backBanner != null) {
            data.locators().visitLocatorGroup(GeoLocatorType.BACKPACK, poseStack, locator -> {
                locator.translate(0, 0.75, 0.3);
                locator.scale(0.65F, -0.65F, -0.65F);
                locator.mulPose(Axis.YN.rotationDegrees(180));
                locator.mulPose(Axis.XN.rotationDegrees(5));

                // 杆子
                int light = state.lightCoords;
                RenderType renderType = RenderTypes.entityCutout(TEXTURE);
                submitNode.submitModel(
                        banner, Unit.INSTANCE, poseStack, renderType, light,
                        OverlayTexture.NO_OVERLAY, state.outlineColor, null
                );

                BannerPatternLayers patterns = state.backBanner.patterns;
                DyeColor baseColor = state.backBanner.baseColor;
                // Match vanilla BannerRenderer: the first submit is the
                // opaque ModelBakery base; submitPatterns chooses
                // Sheets.BANNER_BASE only for the tinted pattern layer.
                Material baseMaterial = ModelBakery.BANNER_BASE;

                // 旗帜图案：1.21.11 的 submitPatterns 同时渲染底色旗面 + 图案（含 baseMaterial），
                // 故 26.1 的独立 flag submitModel（已删的 SpriteId/SpriteGetter 10-arg 重载）折叠进此调用。
                poseStack.mulPose(Axis.YN.rotationDegrees(90));
                poseStack.translate(0.75, 0.2, 0.1);
                submitPatterns(
                        sprites, poseStack, submitNode, light, NO_OVERLAY, flag,
                        0f, baseMaterial, true, baseColor, patterns, false, null, state.outlineColor
                );
            });
        }
    }
}
