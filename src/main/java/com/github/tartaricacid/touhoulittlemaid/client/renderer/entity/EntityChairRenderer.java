package com.github.tartaricacid.touhoulittlemaid.client.renderer.entity;

import com.github.tartaricacid.touhoulittlemaid.client.entity.GeckoChairEntity;
import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.EntityChairModel;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.gecko.GeckoEntityChairRenderer;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.state.EntityChairRenderState;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.state.ModelType;
import com.github.tartaricacid.touhoulittlemaid.client.resource.loader.CustomPackLoader;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityChair;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.event.GeckoUpdateTask;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.GeckoRenderData;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Collections;


/**
 * 椅子实体渲染器。按模型信息选择 Bedrock 或 Gecko 路径，并保留动画与碰撞箱显示行为。
 */
public class EntityChairRenderer extends LivingEntityRenderer<EntityChair, EntityChairRenderState, EntityChairModel> {
    public static final Identifier DEFAULT_TEXTURE = IdentifierUtil.modLoc("textures/entity/empty.png");
    private static final String DEFAULT_CHAIR_ID = "touhou_little_maid:cushion";
    public static boolean renderHitBox = true;
    private final GeckoEntityChairRenderer geckoEntityChairRenderer;

    public EntityChairRenderer(EntityRendererProvider.Context context) {
        super(context, new EntityChairModel(), 0);
        this.geckoEntityChairRenderer = new GeckoEntityChairRenderer(context);
    }

    @Override
    public EntityChairRenderState createRenderState() {
        return new EntityChairRenderState();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void extractRenderState(EntityChair chair, EntityChairRenderState state, float partialTicks) {
        state.clear();
        super.extractRenderState(chair, state, partialTicks);

        state.hasPassenger = chair.hasPassenger();
        state.passengerYRot = chair.getPassengerYaw();
        state.passengerXRot = chair.getPassengerPitch();

        CustomPackLoader.CHAIR_MODELS.getModel(DEFAULT_CHAIR_ID).ifPresent(model -> state.bedrockModel = model);
        CustomPackLoader.CHAIR_MODELS.getInfo(DEFAULT_CHAIR_ID).ifPresent(info -> state.chairInfo = info);
        CustomPackLoader.CHAIR_MODELS.getModel(chair.getModelId()).ifPresent(model -> state.bedrockModel = model);
        CustomPackLoader.CHAIR_MODELS.getInfo(chair.getModelId()).ifPresent(info -> state.chairInfo = info);

        if (state.chairInfo != null) {
            state.chairAnimations = CustomPackLoader.CHAIR_MODELS
                    .getAnimation(state.chairInfo.getModelId().toString())
                    .orElse(Collections.emptyList());
        }

        Player player = Minecraft.getInstance().player;
        if (canShowHitBox(player) && renderHitBox) {
            state.hitbox = chair.getBoundingBox().move(-state.x, -state.y, -state.z);
        }

        if (state.chairInfo != null && state.chairInfo.isGeckoModel()) {
            state.modelType = ModelType.GECKO;
            GeckoChairEntity geckoEntity = chair.getAnimatableEntity();
            if (geckoEntity != null) {
                geckoEntity.setChair(state.chairInfo);
                state.geckoUpdateTask = (GeckoUpdateTask<GeckoRenderData>) geckoEntity.createUpdateTask(state);
            }
        } else {
            state.modelType = ModelType.SIMPLE_BEDROCK;
        }
    }

    @Override
    public void submit(EntityChairRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        if (state.hitbox != null) {
            AABB aabb = state.hitbox.move(state.x, state.y, state.z);
            Gizmos.cuboid(aabb, GizmoStyle.stroke(ARGB.colorFromFloat(1.0F, 1.0F, 0, 0)));
            return;
        }

        if (state.modelType == ModelType.GECKO) {
            this.geckoEntityChairRenderer.submit(state, poseStack, collector, camera);
            return;
        }
        if (state.modelType == ModelType.SIMPLE_BEDROCK && state.bedrockModel != null) {
            this.model = state.bedrockModel;
            this.model.setAnimations(state.chairAnimations);
            this.model.setupAnim(state);
            super.submit(state, poseStack, collector, camera);
        }
    }

    private boolean canShowHitBox(@Nullable Player player) {
        return player != null && player.isShiftKeyDown()
                && player.getMainHandItem().is(InitItems.CHAIR_SHOW);
    }

    @Override
    protected void scale(EntityChairRenderState state, PoseStack poseStack) {
        if (state.chairInfo != null) {
            float scale = state.chairInfo.getRenderEntityScale();
            poseStack.scale(scale, scale, scale);
        }
    }

    @Override
    public Identifier getTextureLocation(EntityChairRenderState state) {
        return state.chairInfo == null ? DEFAULT_TEXTURE : state.chairInfo.getTexture();
    }

    @Override
    protected void setupRotations(EntityChairRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        poseStack.mulPose(Axis.YP.rotationDegrees(180 - bodyRot));
    }

    @Override
    protected boolean shouldShowName(EntityChair entity, double distanceToCameraSq) {
        return entity.shouldShowName();
    }

    @Override
    protected AABB getBoundingBoxForCulling(EntityChair chair) {
        AABB fallback = super.getBoundingBoxForCulling(chair);
        return CustomPackLoader.CHAIR_MODELS.getModel(chair.getModelId()).map(model -> {
            Vec3 position = chair.position();
            return model.getRenderBoundingBox().move(position);
        }).orElse(fallback);
    }
}
