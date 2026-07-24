package com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.gecko;

import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.state.EntityChairRenderState;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityChair;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.GeckoRenderData;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.GeoReplacedEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jspecify.annotations.Nullable;


/**
 * Gecko 椅子模型渲染器，提交渲染状态中已选择并更新的动态模型。
 */
public class GeckoEntityChairRenderer
        extends GeoReplacedEntityRenderer<EntityChair, EntityChairRenderState, GeckoRenderData> {
    public GeckoEntityChairRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityChairRenderState createRenderState() {
        return new EntityChairRenderState();
    }

    @Override
    public @Nullable GeckoRenderData getGeckoRenderData(EntityChairRenderState state) {
        return state.geckoUpdateTask == null ? null : state.geckoUpdateTask.getResult();
    }

    @Override
    protected void scale(EntityChairRenderState state, PoseStack poseStack) {
        if (state.chairInfo != null) {
            float scale = state.chairInfo.getRenderEntityScale();
            poseStack.scale(scale, scale, scale);
        }
    }
}
