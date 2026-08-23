package com.github.tartaricacid.touhoulittlemaid.client.renderer.item;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.item.state.ChairRenderRenderState;
import com.github.tartaricacid.touhoulittlemaid.client.resource.loader.CustomPackLoader;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityChair;
import com.github.tartaricacid.touhoulittlemaid.item.ItemChair;
import com.github.tartaricacid.touhoulittlemaid.util.EntityCacheUtil;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 椅子物品的 SpecialModelRenderer（替代旧的 BlockEntityWithoutLevelRenderer）
 * <p>
 * 参考 {@link com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.EntityChairRenderer} 的渲染模式实现。
 * extractArgument 仿照旧版 BlockEntityWithoutLevelRenderer 创建椅子实体预览，
 * submit 通过新版 EntityRenderDispatcher 提交实体渲染状态。
 */
public class ChairItemRenderer implements SpecialModelRenderer<ChairRenderRenderState> {
    public static final Identifier CHAIR_ITEM_RENDERER = IdentifierUtil.modLoc("chair_item");
    /**
     * 默认兜底模型 ID，与 {@code EntityChairRenderer.DEFAULT_CHAIR_ID} 保持一致
     */
    private static final String DEFAULT_CHAIR_ID = "touhou_little_maid:cushion";

    /**
     * 同 {@link GarageKitItemRenderer}：{@code extractArgument} 的返回值会被
     * {@code SpecialModelWrapper} 追加进 GUI 图标缓存的 model identity（26.1.2 反编译源实查），
     * 而本状态类没有 {@code equals} —— 每帧新建实例就等于每帧换一个 identity，
     * 图标缓存永远失效。这里只缓存可重复使用的模型标识与缩放；实体渲染状态不能缓存，
     * 因为 Gecko 的 {@code GeckoRenderData} 在一次几何提交后会被 {@code close()} 回收。
     */
    private static final ChairRenderRenderState EMPTY = new ChairRenderRenderState();
    private static final Cache<String, ChairRenderRenderState> STATE_CACHE =
            CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.SECONDS).build();

    public ChairItemRenderer() {
    }

    /**
     * 仿照 {@code EntityChairRenderer.extractRenderState} 提取模型、纹理数据到渲染状态
     */
    @Override
    public ChairRenderRenderState extractArgument(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemChair)) {
            return EMPTY;
        }
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return EMPTY;
        }
        String modelId = ItemChair.getData(stack).modelId();
        try {
            return STATE_CACHE.get(modelId, () -> buildState(modelId));
        } catch (ExecutionException e) {
            TouhouLittleMaid.LOGGER.error("Failed to prepare chair item preview", e);
            return EMPTY;
        }
    }

    private static ChairRenderRenderState buildState(String modelId) {
        ChairRenderRenderState state = new ChairRenderRenderState();
        state.modelId = modelId;

        CustomPackLoader.CHAIR_MODELS.getInfo(modelId).ifPresent(
                info -> state.renderItemScale = info.getRenderItemScale()
        );
        return state;
    }

    /**
     * 仿照 {@code EntityChairRenderer.submitChair} 渲染逻辑
     */
    @Override
    public void submit(
            ChairRenderRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords,
            int overlayCoords,
            boolean hasFoil,
            int outlineColor
    ) {
        if (state == null) {
            return;
        }

        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        EntityChair chair = EntityCacheUtil.getChair(level, EntitySpawnReason.LOAD);
        chair.setModelId(state.modelId);

        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        var entityRenderState = dispatcher.extractEntity(chair, 0);
        if (entityRenderState == null) {
            return;
        }

        // 缩放：优先 renderItemScale，兜底 1.0
        float scale = state.renderItemScale > 0 ? state.renderItemScale : 1.0f;

        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        entityRenderState.lightCoords = lightCoords;
        CameraRenderState camera = new CameraRenderState();
        dispatcher.submit(entityRenderState, camera, 1 / scale - 0.125, 0.25, 0.75, poseStack, collector);
        poseStack.popPose();
    }

    /**
     * GUI/world extents for the cushion preview, in the space {@link #submit} draws in.
     *
     * <p><b>minY MUST stay 0</b>: {@code ItemEntityRenderer} derives a dropped item's height
     * from {@code -getModelBoundingBox().minY + 0.0625} (javap-confirmed on 26.1.2). The raw
     * {@code root().getExtentsForGui} reports y in 1.3125..1.5 (a Bedrock part origin sits 24
     * units up), which yields droppedHeight = -1.25 and sinks the dropped cushion 1.25 blocks
     * into the floor (probe-confirmed 2026-08-22). Item render space puts the ground at y=0,
     * so the box is rebased to start there — the same hardcoded box the behavior baseline
     * {@code port/1.21.11-fabric} ships and the user verified. Not derived at runtime on
     * purpose: {@code getExtentsForGui} lives in Bedrock-part space, not this render space.
     */
    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(-0.4375F, 0, -0.4375F));
        output.accept(new Vector3f(0.4375F, 0.1875F, 0.4375F));
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<ChairRenderRenderState> {
        public static final MapCodec<ChairItemRenderer.Unbaked> MAP_CODEC = MapCodec.unit(ChairItemRenderer.Unbaked::new);

        @Override
        public MapCodec<ChairItemRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public ChairItemRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new ChairItemRenderer();
        }
    }
}
