package com.github.tartaricacid.touhoulittlemaid.client.model;

import com.github.tartaricacid.simplebedrockmodel.client.bedrock.BedrockModelUtil;
import com.github.tartaricacid.simplebedrockmodel.client.bedrock.model.BedrockCubePerFace;
import com.github.tartaricacid.simplebedrockmodel.client.bedrock.model.BedrockPart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** [Codex] Lightweight item-icon plane; no entity render state is required. */
public class EntityPlaceholderModel {
    private final BedrockPart bone;

    public EntityPlaceholderModel() {
        bone = new BedrockPart();
        bone.setPos(8.0F, 24.0F, -10.0F);
        bone.cubes.add(new BedrockCubePerFace(-16.0F, -16.0F, 9.5F, 16.0F, 16.0F, 0, 0, 16, 16, BedrockModelUtil.singleSouthFace()));
    }

    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        bone.render(poseStack, buffer, packedLight, packedOverlay);
    }
}
