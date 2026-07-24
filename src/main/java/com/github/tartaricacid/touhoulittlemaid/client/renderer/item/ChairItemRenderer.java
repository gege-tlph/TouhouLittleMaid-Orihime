package com.github.tartaricacid.touhoulittlemaid.client.renderer.item;

import com.github.tartaricacid.touhoulittlemaid.client.resource.loader.CustomPackLoader;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityChair;
import com.github.tartaricacid.touhoulittlemaid.item.ItemChair;
import com.github.tartaricacid.touhoulittlemaid.util.EntityCacheUtil;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

/**
 * 椅子物品的特殊模型渲染器，在物品栏、手持和世界展示中保留完整的三维模型。
 */
public final class ChairItemRenderer implements SpecialModelRenderer<ChairItemRenderer.State> {
    public static final Identifier ID = IdentifierUtil.modLoc("chair_item");
    /**
     * 包围范围取自默认坐垫模型；下边界必须保持为 0，才能与地面变换正确对齐。
     */
    @Override
    public State extractArgument(ItemStack stack) {
        State state = new State();
        ItemChair.Data data = ItemChair.getData(stack);
        state.scale = CustomPackLoader.CHAIR_MODELS.getModelRenderItemScale(data.modelId());
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return state;
        }

        EntityChair chair = EntityCacheUtil.getChair(level, EntitySpawnReason.LOAD);
        chair.setModelId(data.modelId());
        state.entity = Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(chair, 0);
        return state;
    }

    @Override
    public void submit(State state, ItemDisplayContext displayContext, PoseStack poseStack,
                       SubmitNodeCollector collector, int light, int overlay, boolean foil, int outlineColor) {
        if (state == null || state.entity == null) {
            return;
        }
        float scale = state.scale > 0 ? state.scale : 1.0F;
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        state.entity.lightCoords = light;
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        dispatcher.submit(state.entity, new CameraRenderState(),
                1 / scale - 0.125, 0.25, 0.75, poseStack, collector);
        poseStack.popPose();
    }


    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(-0.4375F, 0, -0.4375F));
        output.accept(new Vector3f(0.4375F, 0.1875F, 0.4375F));
    }

    public static final class State {
        private EntityRenderState entity;
        private float scale = 1.0F;
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(Unbaked::new);

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new ChairItemRenderer();
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
