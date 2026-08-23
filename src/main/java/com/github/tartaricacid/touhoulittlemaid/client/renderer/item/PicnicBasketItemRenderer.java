package com.github.tartaricacid.touhoulittlemaid.client.renderer.item;

import com.github.tartaricacid.touhoulittlemaid.client.model.EntityPlaceholderModel;
import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.SimpleBedrockModel;
import com.github.tartaricacid.touhoulittlemaid.client.resource.bedrock.InternalBedrockModelRegistry;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

/**
 * Preserves origin's flat GUI icon and 3D in-hand picnic basket.
 */
public final class PicnicBasketItemRenderer implements SpecialModelRenderer<Unit> {
    public static final Identifier ID = IdentifierUtil.modLoc("picnic_basket_item");
    private static final Identifier TEXTURE = IdentifierUtil.modLoc("textures/bedrock/block/picnic_basket.png");
    private static final Identifier GUI_TEXTURE = IdentifierUtil.modLoc("textures/item/picnic_basket.png");
    private static final EntityPlaceholderModel GUI_MODEL = new EntityPlaceholderModel();
    private final SimpleBedrockModel<Unit> model =
            InternalBedrockModelRegistry.getModel(InternalBedrockModelRegistry.PICNIC_BASKET);

    @Override
    public Unit extractArgument(ItemStack stack) {
        return Unit.INSTANCE;
    }

    @Override
    public void submit(Unit data, ItemDisplayContext displayContext, PoseStack poseStack,
                       SubmitNodeCollector collector, int light, int overlay, boolean foil, int outlineColor) {
        poseStack.pushPose();
        poseStack.translate(0.5, 1.5, 0.5);
        poseStack.mulPose(Axis.ZN.rotationDegrees(180));
        if (displayContext == ItemDisplayContext.GUI) {
            collector.submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(GUI_TEXTURE), (pose, buffer) -> {
                PoseStack geometryPose = new PoseStack();
                geometryPose.last().set(pose);
                GUI_MODEL.renderToBuffer(geometryPose, buffer, light, overlay, -1);
            });
        } else {
            // The 8-arg submitModel overload's third int is the OUTLINE color, not a tint
            // (the default method maps (i, j, k, crumbling) -> (light, overlay, tint=-1,
            // sprite=null, outline=k, crumbling)). Passing -1 here painted a white outline
            // over the basket in hand and on the ground; the GUI pass skips the outline
            // feature, which is why the inventory icon looked fine. Vanilla special
            // renderers (e.g. ShieldSpecialRenderer) forward their received outlineColor.
            collector.submitModel(model, Unit.INSTANCE, poseStack, RenderTypes.entityCutoutNoCull(TEXTURE),
                    light, overlay, outlineColor, null);
        }
        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(0, 0, 0));
        output.accept(new Vector3f(1, 1, 1));
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(Unbaked::new);

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new PicnicBasketItemRenderer();
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
