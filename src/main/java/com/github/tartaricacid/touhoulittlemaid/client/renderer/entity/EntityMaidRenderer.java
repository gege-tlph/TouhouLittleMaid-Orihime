package com.github.tartaricacid.touhoulittlemaid.client.renderer.entity;

import cn.sh1rocu.touhoulittlemaid.api.mixin.IEntityRenderStatePartialTick;
import com.github.tartaricacid.touhoulittlemaid.api.event.client.AddMaidLayerEvent;
import com.github.tartaricacid.touhoulittlemaid.client.animation.HardcodedAnimationManger;
import com.github.tartaricacid.touhoulittlemaid.client.entity.GeckoMaidEntity;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.chatbubble.ChatBubbleRenderer;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.chatbubble.EntityGraphics;
import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.EntityMaidModel;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.gecko.GeckoEntityMaidRenderer;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.layer.*;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.state.EntityMaidRenderState;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.state.ModelType;
import com.github.tartaricacid.touhoulittlemaid.client.resource.loader.CustomPackLoader;
import com.github.tartaricacid.touhoulittlemaid.compat.patpat.PatPatCompat;
import com.github.tartaricacid.touhoulittlemaid.compat.ysm.YsmCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.GeoLayerRenderer;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.IGeoEntity;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.IGeoEntityRenderer;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * 女仆渲染 HUB（节点 5，lean）。26.1 的 3-段 MobRenderer 架构；HEAD 的 immediate-mode/YSM 已被新 RenderState 管线取代。
 * <p>lean 范围（编译器驱动，延后子系统各带锚点，随后续节点还原）：
 * <ul>
 *   <li>✅ 聊天气泡（Node 3 已恢复）：ChatBubbleRenderer/EntityGraphics 从 26.1 逐字还原，字段/ctor/submit 气泡块已接线。</li>
 *   <li>✅ 旧 bedrock layer（Node 4 已恢复）：5 个 LayerMaid* 已 retype 到 RenderLayer&lt;EntityMaidRenderState, EntityMaidModel&gt; 并 addLayer。</li>
 *   <li>✅ YSM 接管：静态钩子 + initYsmModelRenderer + submit 接管分支，
 *       接管顺序与 HEAD 一致（气泡后、GECKO 前）。契约唯一消费者 = 我们维护的 OpenYSM fork 内 TLM 适配器。</li>
 * </ul>
 */
public class EntityMaidRenderer extends MobRenderer<EntityMaid, EntityMaidRenderState, EntityMaidModel> {
    /**
     * YSM 到时候会把渲染器加入其中。
     * <p>
     * HEAD 形态是 {@code Function<Context, IGeoEntityRenderer<Mob>>}（immediate-mode）；本树契约已随管线
     * 重塑为 render-state 形态，唯一消费者是我们维护的 OpenYSM fork 里的 TLM 适配器（rip.ysm.compat.touhoulittlemaid）。
     */
    @Nullable
    public static Function<EntityRendererProvider.Context, IGeoEntityRenderer<EntityMaidRenderState>> YSM_ENTITY_MAID_RENDERER;

    private static final Identifier DEFAULT_TEXTURE = IdentifierUtil.modLoc("textures/entity/empty.png");

    private final GeckoEntityMaidRenderer geckoRenderer;
    private final ChatBubbleRenderer chatBubbleRenderer;

    /**
     * YSM 借用的渲染器实例；null = 未装 YSM 或 YSM 侧未注入
     */
    @Nullable
    private IGeoEntityRenderer<EntityMaidRenderState> ysmMaidRenderer;

    public EntityMaidRenderer(EntityRendererProvider.Context context) {
        super(context, new EntityMaidModel(), 0.5f);

        this.geckoRenderer = new GeckoEntityMaidRenderer(context);
        this.initYsmModelRenderer(context);
        this.chatBubbleRenderer = new ChatBubbleRenderer(this);

        this.addLayer(new LayerMaidHeldItem(this));
        this.addLayer(new LayerMaidBipedHead(this, context));
        this.addLayer(new LayerMaidBackpack(this));
        this.addLayer(new LayerMaidBackItem(this));
        this.addLayer(new LayerMaidBanner(this, context));

        AddMaidLayerEvent.LEGACY.invoker().post(new AddMaidLayerEvent.Legacy(context, this));
    }

    /**
     * 不能使用事件来初始化 YSM 渲染器
     * <p>
     * 使用事件的话，会受到先后顺序的影响
     */
    private void initYsmModelRenderer(EntityRendererProvider.Context manager) {
        if (!YsmCompat.isInstalled() || YSM_ENTITY_MAID_RENDERER == null) {
            return;
        }
        IGeoEntityRenderer<EntityMaidRenderState> geoEntityRenderer = YSM_ENTITY_MAID_RENDERER.apply(manager);
        if (geoEntityRenderer != null) {
            this.ysmMaidRenderer = geoEntityRenderer;
            // 将女仆模组自带的 GeckoLib 模型的 Layer 渲染交给 YSM 侧。
            // HEAD 需 layerRenderer.copy(owner) 重绑归属；本树 GeoLayerRenderer 已是无状态接口（一切入参 per-call），共享实例即等价。
            for (GeoLayerRenderer<?, ?> layerRenderer : this.geckoRenderer.getLayerRenderers()) {
                this.ysmMaidRenderer.addGeoLayerRenderer(layerRenderer);
            }
        }
    }

    @Override
    public EntityMaidRenderState createRenderState() {
        return new EntityMaidRenderState();
    }

    @Override
    public void extractRenderState(EntityMaid maid, EntityMaidRenderState state, float partialTicks) {
        state.clear();
        super.extractRenderState(maid, state, partialTicks);
        HumanoidMobRenderer.extractHumanoidRenderState(maid, state, partialTicks, itemModelResolver);
        EntityMaidRenderState.extractRenderState(maid, state, partialTicks, itemModelResolver, getGeckoEntity(maid));
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public GeckoMaidEntity<? extends EntityMaid> getGeckoEntity(Mob entity) {
        return entity.getAttached(GeckoMaidEntity.TYPE);
    }

    @Override
    public void submit(EntityMaidRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        // 一般不太可能触发
        if (state.modelType == ModelType.NONE) {
            return;
        }

        state.camera = camera;

        // 聊天气泡渲染
        if (state.showBubble && state.bubbleOffset != null) {
            poseStack.pushPose();

            double offsetY = state.bubbleOffset.y() + 0.5f;
            if (state.sitting) {
                offsetY -= 0.25f;
            }
            poseStack.translate(state.bubbleOffset.x, offsetY, state.bubbleOffset.z);
            poseStack.mulPose(camera.orientation);
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
            poseStack.scale(-0.025F, -0.025F, 0.025F);

            // 26.1 直呼 state.tlm$partialTick()；本树 partial-tick 走 duck-interface + EntityRenderStateMixin（显式 cast）
            float partialTick = ((IEntityRenderStatePartialTick) (Object) state).tlm$partialTick();
            EntityGraphics graphics = new EntityGraphics(submitNodeCollector, poseStack, state, state.lightCoords, partialTick);
            this.chatBubbleRenderer.submit(graphics);

            poseStack.popPose();
        }

        // YSM 接管渲染（HEAD 语义与顺序：标志 + 已注入渲染器则全权移交；state.maid 与下方 PatPat 用法同源）
        EntityMaid maidEntity = state.maid;
        if (maidEntity != null && maidEntity.isYsmModel() && this.ysmMaidRenderer != null) {
            IGeoEntity geoEntity = this.ysmMaidRenderer.getGeoEntity(state);
            geoEntity.setYsmModel(maidEntity.getYsmModelId(), maidEntity.getYsmModelTexture());
            geoEntity.updateRoamingVars(maidEntity.roamingVars);
            float ysmPartialTick = ((IEntityRenderStatePartialTick) (Object) state).tlm$partialTick();
            PatPatCompat.renderPat(maidEntity, poseStack, ysmPartialTick);
            this.ysmMaidRenderer.geoRender(state, state.bodyRot, ysmPartialTick, poseStack, submitNodeCollector, state.lightCoords);
            // 名字牌：本分支不走 super.submit()，而 vanilla 的名字牌正是在 EntityRenderer.submit 里提交的，
            // 故须显式补一次（HEAD 由 YSM 渲染器经 accessor 自行提交，效果相同）。
            // 拴绳有意不补：HEAD 的 YSM 路径同样没有提交拴绳，补了就是与基准的偏离。
            this.submitNameTag(state, poseStack, submitNodeCollector, camera);
            return;
        }

        // GeckoLib 接管渲染
        if (state.modelType == ModelType.GECKO) {
            float partialTick = ((IEntityRenderStatePartialTick) (Object) state).tlm$partialTick();
            PatPatCompat.renderPat(state.maid, poseStack, partialTick);
            this.geckoRenderer.submit(state, poseStack, submitNodeCollector, camera);
            return;
        }

        // 普通模型渲染
        if (state.modelType == ModelType.SIMPLE_BEDROCK) {
            if (state.bedrockModel == null) {
                return;
            }
            this.model = state.bedrockModel;
            this.model.setAnimations(state.animations);
            super.submit(state, poseStack, submitNodeCollector, camera);
        }
    }

    @Override
    protected void setupRotations(EntityMaidRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);

        // 抱起女仆时的旋转
        if (state.playerVehicle && state.modelType != ModelType.GECKO) {
            poseStack.translate(-0.375, 0.8325, 0.375);
            poseStack.mulPose(Axis.ZN.rotationDegrees(65));
            poseStack.mulPose(Axis.YN.rotationDegrees(-80));
        }
        if (state.modelType != ModelType.GECKO) {
            HardcodedAnimationManger.setupRotations(state, poseStack, bodyRot, false);
        }
    }

    @Override
    protected void scale(EntityMaidRenderState state, PoseStack poseStack) {
        if (state.modelInfo == null) {
            return;
        }
        var scale = state.modelInfo.getRenderEntityScale();
        poseStack.scale(scale, scale, scale);
    }

    @Override
    public Identifier getTextureLocation(EntityMaidRenderState state) {
        if (state.modelInfo == null) {
            return DEFAULT_TEXTURE;
        }
        return state.modelInfo.getTexture();
    }

    @Override
    protected AABB getBoundingBoxForCulling(EntityMaid maid) {
        AABB fallback = super.getBoundingBoxForCulling(maid);
        return CustomPackLoader.MAID_MODELS.getModel(maid.getModelId())
                .map(model -> model.getRenderBoundingBox().move(maid.position()))
                .orElse(fallback);
    }

    @Override
    public float getWhiteOverlayProgress(EntityMaidRenderState state) {
        return super.getWhiteOverlayProgress(state);
    }
}
