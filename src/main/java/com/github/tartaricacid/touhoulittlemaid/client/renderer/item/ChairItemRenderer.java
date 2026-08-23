package com.github.tartaricacid.touhoulittlemaid.client.renderer.item;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 1.21.11 special-model replacement for origin's chair BEWLR.
 */
public final class ChairItemRenderer implements SpecialModelRenderer<ChairItemRenderer.State> {
    public static final Identifier ID = IdentifierUtil.modLoc("chair_item");

    // 同 GarageKitItemRenderer：extractArgument 的返回值参与 GUI 图标缓存的 model identity，
    // 必须引用稳定，否则创造栏满屏坐垫每帧全量重抽取重绘。按 modelId 记忆化。
    private static final State EMPTY = new State();
    private static final Cache<String, State> STATE_CACHE =
            CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.SECONDS).build();

    @Override
    public State extractArgument(ItemStack stack) {
        ItemChair.Data data = ItemChair.getData(stack);
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return EMPTY;
        }
        try {
            return STATE_CACHE.get(data.modelId(), () -> buildState(data, level));
        } catch (ExecutionException e) {
            TouhouLittleMaid.LOGGER.error("Failed to prepare chair item preview", e);
            return EMPTY;
        }
    }

    private static State buildState(ItemChair.Data data, Level level) {
        State state = new State();
        state.scale = CustomPackLoader.CHAIR_MODELS.getModelRenderItemScale(data.modelId());
        // EntityCacheUtil.getChair assigns the negative preview id; the previous inline
        // ENTITY_CACHE.get bypassed it, so GeckoChairEntity.isPreviewEntity() stayed false,
        // the never-ticked preview chair froze Gecko's frame clock and the immutable
        // (dropped-item) path never extracted any render bones. Matches origin/26.1's shape.
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

    /**
     * GUI/world extents for the cushion preview, in the space {@link #submit} draws in.
     *
     * <p>Measured from the default cushion Bedrock model with a temporary probe
     * ({@code root().getExtentsForGui}): 24 points, x/z = +-0.4375 (14px wide) and a
     * 0.1875 (3px) thickness. That derivation reports y in 1.3125..1.5 because a Bedrock
     * part places its origin 24 units up, while item render space puts the ground at 0,
     * so the box is rebased to start at y=0.
     *
     * <p>minY MUST stay 0: {@link net.minecraft.client.renderer.entity.ItemEntityRenderer}
     * derives a dropped item's height from {@code -minY + 0.0625}. Feeding it the raw
     * 1.3125 lifts the dropped cushion more than a block off the ground. The previous
     * hardcoded box also had minY=0, which is why dropped rendering was correct; it was
     * only too large in x/y/z (+-1 wide, 2 tall), so the GUI AABB exceeded 16px, the item
     * was classified oversized-in-GUI and its creative tab icon was drawn in the oversized
     * pass underneath the tab graphic.
     *
     * <p>Not derived at runtime on purpose: extents are memoized once per baked model, and
     * this renderer submits an entity render rather than the model itself, so the Bedrock
     * part transform is not the space these coordinates live in.
     */
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
