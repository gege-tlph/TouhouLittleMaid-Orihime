package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.detail;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Vector3f;


@Environment(EnvType.CLIENT)
public class DebugFloorPiPRenderer extends PictureInPictureRenderer<DebugFloorRenderState> {
    public DebugFloorPiPRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public Class<DebugFloorRenderState> getRenderStateClass() {
        return DebugFloorRenderState.class;
    }

    @Override
    protected void renderToTexture(DebugFloorRenderState state, PoseStack poseStack) {
        Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
        Vector3f translation = state.translation();
        poseStack.translate(translation.x, translation.y, translation.z);
        poseStack.mulPose(state.rotation());
        poseStack.translate(0.0F, 0.5F, 0.0F);
        state.floorModel().renderToBuffer(poseStack,
                this.bufferSource.getBuffer(state.floorModel().renderType(AbstractModelDetailsGui.FLOOR_TEXTURE)),
                0xf000f0, OverlayTexture.NO_OVERLAY);
    }

    @Override
    protected float getTranslateY(int i, int j) {
        return i / 2.0F;
    }

    @Override
    protected String getTextureLabel() {
        return "tlm_debug_floor";
    }
}
