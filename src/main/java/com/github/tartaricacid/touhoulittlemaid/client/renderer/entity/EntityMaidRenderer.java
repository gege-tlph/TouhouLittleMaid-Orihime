package com.github.tartaricacid.touhoulittlemaid.client.renderer.entity;

import com.github.tartaricacid.touhoulittlemaid.api.event.client.AddMaidLayerEvent;
import com.github.tartaricacid.touhoulittlemaid.client.entity.GeckoMaidEntity;
import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.EntityMaidModel;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.chatbubble.ChatBubbleRenderer;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.chatbubble.EntityGraphics;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.gecko.GeckoEntityMaidRenderer;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.layer.*;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.state.EntityMaidRenderState;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.state.ModelType;
import com.github.tartaricacid.touhoulittlemaid.compat.ysm.YsmCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.IGeoEntityRenderer;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class EntityMaidRenderer extends MobRenderer<EntityMaid, EntityMaidRenderState, EntityMaidModel> {
    private static final Identifier DEFAULT_TEXTURE = IdentifierUtil.modLoc("textures/entity/empty.png");

    /**
     * 第三方模型系统（目前是 YSM）接管女仆本体渲染的钩子。为 null 表示未安装该模组——
     * 保持 null 是安全默认值，{@code ModelType.YSM} 分支在这种情况下什么都不画（不崩，不回落）。
     * <p>
     * 由该模组自己的 Fabric client 入口在初始化时赋值，本类不知道、也不需要知道赋值方是谁。
     */
    @Nullable
    public static Function<EntityRendererProvider.Context, IGeoEntityRenderer<EntityMaidRenderState>> YSM_ENTITY_MAID_RENDERER;

    private final BlockModelResolver blockModelResolver;
    private final GeckoEntityMaidRenderer geckoRenderer;
    private final ChatBubbleRenderer chatBubbleRenderer;
    @Nullable
    private final IGeoEntityRenderer<EntityMaidRenderState> ysmModelRenderer;

    public EntityMaidRenderer(EntityRendererProvider.Context context) {
        super(context, new EntityMaidModel(), 0.5f);

        this.blockModelResolver = context.getBlockModelResolver();
        this.geckoRenderer = new GeckoEntityMaidRenderer(context);
        this.chatBubbleRenderer = new ChatBubbleRenderer(this);
        this.ysmModelRenderer = initYsmModelRenderer(context);

        this.addLayer(new LayerMaidHeldItem(this));
        this.addLayer(new LayerMaidBipedHead(this, context));
        this.addLayer(new LayerMaidBackpack(this));
        this.addLayer(new LayerMaidBackItem(this));
        this.addLayer(new LayerMaidBanner(this, context));

        AddMaidLayerEvent.LEGACY.invoker().post(new AddMaidLayerEvent.Legacy(context, this));
    }

    /**
     * YSM 接管渲染器要能画 TLM 自己的 5 个挂件 layer（拿持物/头饰/背包/背部物品/旗帜），
     * 所以把 gecko 渲染器已经收集到的那一份原样转交给它——两条路径共用同一批 layer 实例，
     * TLM 增删 layer 时两边自动保持一致，不需要在这里重复枚举。
     * <p>
     * 额外挡一层 {@link YsmCompat#isInstalled()}：{@link #YSM_ENTITY_MAID_RENDERER} 理论上只有
     * OpenYSM 自己的客户端入口跑过才会非 null，这层检查更多是把"是否接管"的判据集中到
     * YsmCompat 一处，不散落成"钩子非空即启用"的隐式约定。
     */
    @Nullable
    private IGeoEntityRenderer<EntityMaidRenderState> initYsmModelRenderer(EntityRendererProvider.Context context) {
        if (!YsmCompat.isInstalled() || YSM_ENTITY_MAID_RENDERER == null) {
            return null;
        }
        var renderer = YSM_ENTITY_MAID_RENDERER.apply(context);
        for (var layer : this.geckoRenderer.getLayerRenderers()) {
            renderer.addGeoLayerRenderer(layer);
        }
        return renderer;
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
        EntityMaidRenderState.extractRenderState(maid, state, partialTicks, blockModelResolver, itemModelResolver, getGeckoEntity(maid));
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

            EntityGraphics graphics = new EntityGraphics(submitNodeCollector, poseStack, state, state.lightCoords, state.tlm$partialTick());
            this.chatBubbleRenderer.submit(graphics);

            poseStack.popPose();
        }

        // 第三方模型系统（YSM）身体接管渲染
        if (state.modelType == ModelType.YSM) {
            if (this.ysmModelRenderer != null) {
                this.ysmModelRenderer.submit(state, poseStack, submitNodeCollector, camera);
            }
            return;
        }

        // GeckoLib 接管渲染
        if (state.modelType == ModelType.GECKO) {
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
    public float getWhiteOverlayProgress(EntityMaidRenderState state) {
        return super.getWhiteOverlayProgress(state);
    }
}
