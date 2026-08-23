package com.github.tartaricacid.touhoulittlemaid.client.renderer.item;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.client.render.MaidRenderState;
import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.SimpleBedrockModel;
import com.github.tartaricacid.touhoulittlemaid.client.resource.bedrock.InternalBedrockModelRegistry;
import com.github.tartaricacid.touhoulittlemaid.client.resource.loader.CustomPackLoader;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.github.tartaricacid.touhoulittlemaid.item.ItemGarageKit;
import com.github.tartaricacid.touhoulittlemaid.util.EntityCacheUtil;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.github.tartaricacid.touhoulittlemaid.util.EntityCacheUtil.clearMaidDataResidue;

/**
 * 1.21.11 special-model replacement for origin's garage-kit BEWLR.
 */
public final class GarageKitItemRenderer implements SpecialModelRenderer<GarageKitItemRenderer.State> {
    public static final Identifier ID = IdentifierUtil.modLoc("garage_kit_item");
    private static final Identifier TEXTURE = IdentifierUtil.modLoc("textures/bedrock/block/statue_base.png");
    private final SimpleBedrockModel<Unit> baseModel =
            InternalBedrockModelRegistry.getModel(InternalBedrockModelRegistry.STATUE_BASE);

    // GUI 的物品图标缓存按 model identity 判等，而 SpecialModelWrapper 会把 extractArgument 的
    // 返回值追加进 identity——State 必须引用稳定：每帧新建会让图标缓存永远失效，创造栏/JEI
    // 满屏手办时每帧全量重画（整只女仆 NBT load + 渲染状态抽取 + gecko 渲染线程 join）。
    // 按数据组件记忆化后，重活只在数据变化时做一次；弱键随物品堆存亡，10 秒无访问过期。
    private static final State EMPTY = new State();
    private static final Cache<CustomData, State> STATE_CACHE =
            CacheBuilder.newBuilder().weakKeys().expireAfterAccess(10, TimeUnit.SECONDS).build();

    @Override
    public State extractArgument(ItemStack stack) {
        CustomData data = ItemGarageKit.getMaidData(stack);
        Level level = Minecraft.getInstance().level;
        if (data.isEmpty() || level == null) {
            return EMPTY;
        }
        try {
            return STATE_CACHE.get(data, () -> buildState(stack, data.copyTag(), level));
        } catch (ExecutionException e) {
            TouhouLittleMaid.LOGGER.error("Failed to prepare garage-kit item preview", e);
            return EMPTY;
        }
    }

    private State buildState(ItemStack stack, CompoundTag data, Level level) {
        State state = new State();
        data.getString("id").flatMap(EntityType::byString).ifPresent(type -> {
            try {
                Entity entity;
                if (type.equals(InitEntities.MAID)) {
                    entity = EntityCacheUtil.GARAGE_KIT_CACHE.get(stack, () -> new EntityMaid(level));
                } else {
                    entity = EntityCacheUtil.ENTITY_CACHE.get(type, () -> {
                        Entity created = type.create(level, EntitySpawnReason.LOAD);
                        return Objects.requireNonNullElseGet(created, () -> new EntityMaid(level));
                    });
                }
                entity.load(TagValueInput.create(ProblemReporter.DISCARDING, entity.registryAccess(), data));
                if (entity instanceof EntityMaid maid) {
                    clearMaidDataResidue(maid, true);
                    maid.renderState = MaidRenderState.GARAGE_KIT_ITEM;
                    maid.tickCount = 0;
                    data.getString(EntityMaid.MODEL_ID_TAG).ifPresent(modelId ->
                            state.scale = CustomPackLoader.MAID_MODELS.getModelRenderItemScale(modelId));
                }
                state.entity = Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(entity, 0);
            } catch (ExecutionException e) {
                TouhouLittleMaid.LOGGER.error("Failed to prepare garage-kit item preview", e);
            }
        });
        return state;
    }

    @Override
    public void submit(State state, ItemDisplayContext displayContext, PoseStack poseStack,
                       SubmitNodeCollector collector, int light, int overlay, boolean foil, int outlineColor) {
        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.translate(1, 1.5, 1);
        poseStack.mulPose(Axis.ZN.rotationDegrees(180));
        // NoCull matches origin's TileEntityItemStackGarageKitRenderer; the third int of the
        // 8-arg submitModel overload is the OUTLINE color (see PicnicBasketItemRenderer) —
        // passing -1 painted a white outline over the base on the dropped-item form.
        collector.submitModel(baseModel, Unit.INSTANCE, poseStack, RenderTypes.entityCutoutNoCull(TEXTURE),
                light, overlay, outlineColor, null);
        poseStack.popPose();

        if (state == null || state.entity == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        float scale = state.scale > 0 ? state.scale : 1.0F;
        poseStack.scale(scale, scale, scale);
        poseStack.translate(1, 0.21328125, 1);
        poseStack.mulPose(Axis.YP.rotationDegrees(180));
        state.entity.lightCoords = light;
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        dispatcher.submit(state.entity, new CameraRenderState(), 0, 0, 0, poseStack, collector);
        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(0, 0, 0));
        output.accept(new Vector3f(1, 2, 1));
    }

    public static final class State {
        private EntityRenderState entity;
        private float scale = 1.0F;
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(Unbaked::new);

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new GarageKitItemRenderer();
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
