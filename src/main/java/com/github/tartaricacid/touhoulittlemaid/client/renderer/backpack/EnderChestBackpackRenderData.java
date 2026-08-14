package com.github.tartaricacid.touhoulittlemaid.client.renderer.backpack;

import com.github.tartaricacid.touhoulittlemaid.api.backpack.MaidBackpackRenderData;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.state.EntityMaidRenderState;
import com.github.tartaricacid.touhoulittlemaid.client.resource.bedrock.InternalBedrockModelRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import static com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid.MOD_ID;
import static com.github.tartaricacid.touhoulittlemaid.client.resource.bedrock.InternalBedrockModelRegistry.END_CHEST_BACKPACK;

// NODE R8：origin EnderChestBackpack 的 client 渲染三元组（注意 origin 就是 模型=end_chest / 纹理=ender_chest，非笔误）
public class EnderChestBackpackRenderData extends MaidBackpackRenderData {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MOD_ID, "textures/bedrock/entity/backpack/ender_chest_backpack.png");

    @Override
    public @Nullable EntityModel<EntityMaidRenderState> getBackpackModel() {
        return InternalBedrockModelRegistry.getEntityModel(END_CHEST_BACKPACK);
    }

    @Override
    public @Nullable Identifier getBackpackTexture() {
        return TEXTURE;
    }

    @Override
    public void offsetBackpackItem(PoseStack poseStack) {
        poseStack.translate(0, 0.625, 0.25);
    }
}
