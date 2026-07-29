package com.github.tartaricacid.touhoulittlemaid.client.renderer.blockentity;

import cn.sh1rocu.touhoulittlemaid.api.mixin.IEntityRenderStatePartialTick;
import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.client.render.MaidRenderState;
import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.SimpleBedrockModel;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.blockentity.state.GarageKitRenderState;
import com.github.tartaricacid.touhoulittlemaid.client.resource.bedrock.InternalBedrockModelRegistry;
import com.github.tartaricacid.touhoulittlemaid.compat.ysm.YsmCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.github.tartaricacid.touhoulittlemaid.tileentity.TileEntityGarageKit;
import com.github.tartaricacid.touhoulittlemaid.util.EntityCacheUtil;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import com.github.tartaricacid.touhoulittlemaid.util.migrate.EntityTypeUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static com.github.tartaricacid.touhoulittlemaid.client.resource.bedrock.InternalBedrockModelRegistry.STATUE_BASE;
import static com.github.tartaricacid.touhoulittlemaid.util.EntityCacheUtil.clearMaidDataResidue;
import static net.minecraft.util.ProblemReporter.DISCARDING;

/** [Codex] 1.21.11 render-state renderer for a placed garage-kit figure. */
public final class GarageKitRenderer implements BlockEntityRenderer<TileEntityGarageKit, GarageKitRenderState> {
    private static final Identifier TEXTURE = IdentifierUtil.modLoc("textures/bedrock/block/statue_base.png");
    private final EntityRenderDispatcher dispatcher;
    private final SimpleBedrockModel<Unit> baseModel;

    public GarageKitRenderer(BlockEntityRendererProvider.Context context) {
        this.dispatcher = context.entityRenderer();
        this.baseModel = InternalBedrockModelRegistry.getModel(STATUE_BASE);
    }

    @Override
    public GarageKitRenderState createRenderState() {
        return new GarageKitRenderState();
    }

    @Override
    public void extractRenderState(TileEntityGarageKit kit, GarageKitRenderState state, float partialTick,
                                   Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(kit, state, partialTick, cameraPos, breakProgress);
        state.facing = kit.getFacing();
        state.extraData = kit.getExtraData();
        state.entityRenderState = null;

        if (state.extraData == null || state.extraData.isEmpty() || kit.getLevel() == null) {
            return;
        }
        Optional<String> id = state.extraData.getString("id");
        if (id.isEmpty()) {
            return;
        }
        EntityTypeUtil.byString(id.get()).ifPresent(type -> {
            try {
                extractEntityRenderState(kit, state, state.extraData, kit.getLevel(), type, partialTick);
            } catch (ExecutionException e) {
                TouhouLittleMaid.LOGGER.error("Failed to extract garage-kit entity render state", e);
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void extractEntityRenderState(TileEntityGarageKit kit, GarageKitRenderState state, CompoundTag data,
                                          Level level, EntityType type, float partialTick) throws ExecutionException {
        Entity entity = type.equals(InitEntities.MAID)
                ? EntityCacheUtil.getMaidInStatue(kit.getBlockPos().asLong(), level)
                : EntityCacheUtil.getEntity(type, (l, ignored) -> new EntityMaid(l),
                level, EntitySpawnReason.COMMAND);

        RegistryAccess access = entity.registryAccess();
        ValueInput input = TagValueInput.create(DISCARDING, access, data);
        entity.load(input);
        if (entity instanceof EntityMaid maid) {
            clearMaidDataResidue(maid, true);
            maid.renderState = MaidRenderState.GARAGE_KIT;
            // YSM 模型靠 tickCount 推进动画，冻结为 0 会让展示柜里的 YSM 女仆定格（HEAD 同款分支）
            if (YsmCompat.isInstalled() && maid.isYsmModel()) {
                maid.tickCount = (int) level.getGameTime();
            } else {
                maid.tickCount = 0;
            }
        }

        state.entityRenderState = dispatcher.extractEntity(entity, partialTick);
        state.entityRenderState.lightCoords = state.lightCoords;
        ((IEntityRenderStatePartialTick) (Object) state.entityRenderState).tlm$setPartialTick(0);
    }

    @Override
    public void submit(GarageKitRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.translate(1, 1.5, 1);
        poseStack.mulPose(Axis.ZN.rotationDegrees(180));
        collector.submitModel(baseModel, Unit.INSTANCE, poseStack, RenderTypes.entityCutoutNoCull(TEXTURE),
                state.lightCoords, OverlayTexture.NO_OVERLAY, 0, state.breakProgress);
        poseStack.popPose();

        if (state.entityRenderState == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.translate(1, 0.21328125, 1);
        switch (state.facing) {
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            default -> { }
        }
        dispatcher.submit(state.entityRenderState, camera, 0, 0, 0, poseStack, collector);
        poseStack.popPose();
    }
}
