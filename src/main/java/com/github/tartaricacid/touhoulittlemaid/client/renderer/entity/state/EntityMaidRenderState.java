package com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.state;

import com.github.tartaricacid.touhoulittlemaid.api.animation.IAnimation;
import com.github.tartaricacid.touhoulittlemaid.api.event.client.RenderMaidEvent;
import com.github.tartaricacid.touhoulittlemaid.api.backpack.IMaidBackpack;
import com.github.tartaricacid.touhoulittlemaid.client.entity.GeckoMaidEntity;
import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.EntityMaidModel;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.gecko.GeckoMaidRenderData;
import com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.MaidModelInfo;
import cn.sh1rocu.touhoulittlemaid.util.neoforge.ClientHooks;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.backpack.BackpackManager;
import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.ChatBubbleDataCollection;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.event.GeckoUpdateTask;
import net.minecraft.client.renderer.blockentity.state.BannerRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import static com.github.tartaricacid.touhoulittlemaid.client.resource.loader.CustomPackLoader.MAID_MODELS;


public class EntityMaidRenderState extends HumanoidRenderState {
    /**
     * 为普通状态管道无法覆盖的可选渲染器桥保留实时源。
     */
    public @Nullable EntityMaid maid;
    public int entityId = -1;
    /**
     * 渲染类型，决定是 Simple Bedrock Model 模型还是 GeckoLib 模型
     */
    public ModelType modelType = ModelType.NONE;
    /**
     * 自定义女仆模型，模型 ID
     */
    public String modelId;
    /**
     * 自定义女仆模型，额外模型相关信息
     */
    public @Nullable MaidModelInfo modelInfo;
    /**
     * Simple Bedrock Model 模型，仅在 SIMPLE_BEDROCK 模式下渲染
     */
    public EntityMaidModel bedrockModel;
    /**
     * 女仆的自定义名称，用于一些命名彩蛋模型的渲染判断
     */
    public @Nullable Component customName;
    /**
     * 自定义女仆模型关联的动画
     */
    public List<IAnimation<EntityMaidRenderState>> animations = Collections.emptyList();
    /**
     * GeckoLib 模型更新数据
     */
    public GeckoUpdateTask<GeckoMaidRenderData> geckoUpdateTask;
    /**
     * 玩家的摄像机数据
     */
    public CameraRenderState camera;
    /**
     * 是否显示聊天气泡
     */
    public boolean showBubble;
    /**
     * 聊天气泡起始位置
     */
    public @Nullable Vec3 bubbleOffset;

    public @Nullable ChatBubbleDataCollection chatBubble;
    /**
     * 当前是否被玩家抱起（即骑乘玩家）
     */
    public boolean playerVehicle;
    /**
     * 女仆是否处于待命状态
     */
    public boolean sitting;
    /**
     * 当前女仆是否处于睡觉状态
     */
    public boolean sleeping;
    /**
     * 女仆挥动手臂的计数器
     */
    public int swingTime;
    /**
     * 女仆是否正在抬起手臂
     */
    public boolean swingingArms;
    /**
     * 女仆是否处于祈求状态
     */
    public boolean begging;
    /**
     * 女仆当前是否正处于受伤状态
     */
    public boolean hurt;
    /**
     * 女仆是否处于钓鱼状态
     */
    public boolean hasFishingHook;
    /**
     * 女仆是否处于游泳状态
     */
    public boolean isSwimming;
    /**
     * 女仆当前工作模式 ID
     */
    public String taskId;
    /**
     * 护甲值，可能用于一些根据护甲值变化的动画或渲染效果
     */
    public int armorValue;

    public String atBiomeTemp = "MEDIUM";
    /**
     * 当前血量
     */
    public float health;
    /**
     * 最大血量
     */
    public float maxHealth;
    /**
     * 实体随机值，用于一些需要随机效果的动画 <p> 默认会取该实体 UUID 的低 64 位，确保同一实体在不同帧的渲染过程中保持一致的随机值
     */
    public long randomNumber;
    /**
     * 女仆配置控制，是否渲染背包
     */
    public boolean showBackpack;
    /**
     * 女仆当前是否穿戴背包
     */
    public boolean hasBackpack;

    public @Nullable IMaidBackpack backpack;
    /**
     * 旗帜渲染
     */
    public @Nullable BannerRenderState backBanner;

    public @Nullable BlockState headBlockState;
    /**
     * 装饰栏，物品
     */
    public final ItemStackRenderState backItem = new ItemStackRenderState();
    /**
     * 游戏时间，用于一些仅根据时间变化的动画或渲染效果
     */
    public long gameTime;
    /**
     * 维度信息，用于一些根据维度变化的动画或渲染效果
     */
    public @Nullable ResourceKey<Level> dimension;
    /**
     * 当前所处环境是否下雨，用于一些根据天气变化的动画或渲染效果
     */
    public boolean raining;
    /**
     * 当前所处环境是否处于雷暴天气，用于一些根据天气变化的动画或渲染效果
     */
    public boolean thundering;


    private static final String DEFAULT_MODEL_ID = "touhou_little_maid:hakurei_reimu";

    public void clear() {
        maid = null;
        entityId = -1;
        modelType = ModelType.NONE;
        modelId = null;
        modelInfo = null;
        bedrockModel = null;
        customName = null;
        animations = Collections.emptyList();
        geckoUpdateTask = null;
        camera = null;
        showBubble = false;
        bubbleOffset = null;
        chatBubble = null;
        playerVehicle = false;
        sitting = false;
        sleeping = false;
        swingTime = 0;
        swingingArms = false;
        begging = false;
        hurt = false;
        hasFishingHook = false;
        isSwimming = false;
        taskId = null;
        armorValue = 0;
        atBiomeTemp = "MEDIUM";
        health = 0;
        maxHealth = 0;
        randomNumber = 0;
        showBackpack = false;
        hasBackpack = false;
        backpack = null;
        backBanner = null;
        headBlockState = null;
        backItem.clear();
        gameTime = 0;
        dimension = null;
        raining = false;
        thundering = false;
    }

    public static void extractRenderState(
            EntityMaid maid,
            EntityMaidRenderState state,
            float partialTicks,
            ItemModelResolver itemModelResolver,
            @Nullable GeckoMaidEntity<? extends EntityMaid> geckoEntity
    ) {
        state.maid = maid;
        state.entityId = maid.getId();
        extractEnvironmentState(maid, state);
        extractAttributeState(maid, state);
        extractBehaviorState(maid, state);
        extractModelState(maid, state);
        extractBackDecorationState(maid, state);
        extractChatBubbleState(maid, state, partialTicks);
        extractBackpackState(maid, state, itemModelResolver);
        extractGeckoState(state, geckoEntity);  // Gecko 更新 + modelType 定型，放最后
    }

    private static void extractEnvironmentState(EntityMaid maid, EntityMaidRenderState state) {
        state.gameTime = maid.level().getGameTime();
        state.dimension = maid.level().dimension();
        state.raining = maid.level().isRaining();
        state.thundering = maid.level().isThundering();
    }

    private static void extractAttributeState(EntityMaid maid, EntityMaidRenderState state) {
        state.customName = maid.getCustomName();
        state.armorValue = maid.getArmorValue();
        state.atBiomeTemp = maid.getAtBiomeTemp();
        state.health = maid.getHealth();
        state.maxHealth = maid.getMaxHealth();
        state.randomNumber = maid.getUUID().getLeastSignificantBits();
    }

    private static void extractBehaviorState(EntityMaid maid, EntityMaidRenderState state) {
        state.playerVehicle = maid.getVehicle() instanceof Player;
        state.sitting = maid.isMaidInSittingPose();
        state.swingTime = maid.swingTime;
        state.sleeping = maid.isSleeping();
        state.begging = maid.isBegging();
        state.swingingArms = maid.isSwingingArms();
        state.hasBackpack = maid.hasBackpack();
        state.hurt = maid.hurtTime > 0;
        state.hasFishingHook = maid.hasFishingHook();
        state.isSwimming = maid.isInWater() && maid.getFluidHeight(FluidTags.WATER) > maid.getFluidJumpThreshold();
        state.taskId = maid.getTask().getUid().getPath();
    }

    private static void extractModelState(EntityMaid maid, EntityMaidRenderState state) {
        state.modelId = maid.getModelId();

        EntityMaidModel defaultModel = MAID_MODELS.getModel(DEFAULT_MODEL_ID).orElse(null);
        MaidModelInfo defaultInfo = MAID_MODELS.getInfo(DEFAULT_MODEL_ID).orElse(null);
        List<IAnimation<EntityMaidRenderState>> defaultAnimations = MAID_MODELS.getAnimation(DEFAULT_MODEL_ID).orElse(null);
        RenderMaidEvent.ModelData eventModelData = new RenderMaidEvent.ModelData(defaultModel, defaultInfo, defaultAnimations);
        RenderMaidEvent event = new RenderMaidEvent(maid, eventModelData);
        RenderMaidEvent.CALLBACK.invoker().post(event);
        if (event.isCanceled()) {
            state.bedrockModel = eventModelData.getModel() != null ? eventModelData.getModel() : defaultModel;
            state.modelInfo = eventModelData.getInfo();
            state.animations = eventModelData.getAnimations() == null ? Collections.emptyList() : eventModelData.getAnimations();
        } else {
            MAID_MODELS.getModel(state.modelId).ifPresent(model -> state.bedrockModel = model);
            MAID_MODELS.getInfo(state.modelId).ifPresent(info -> state.modelInfo = info);
            MAID_MODELS.getAnimation(state.modelId).ifPresent(animations -> state.animations = animations);

            if (state.bedrockModel == null) {
                state.bedrockModel = defaultModel;
            }
            if (state.modelInfo == null) {
                state.modelInfo = defaultInfo;
            }
            if (state.animations.isEmpty() && defaultAnimations != null) {
                state.animations = defaultAnimations;
            }
        }
    }

    private static void extractBackDecorationState(EntityMaid maid, EntityMaidRenderState state) {
        ItemStack showItem = maid.getBackpackShowItem();

        // 旗帜特殊效果，最优先
        if (showItem.getItem() instanceof BannerItem bannerItem) {
            BannerPatternLayers layers = showItem.get(DataComponents.BANNER_PATTERNS);
            if (layers == null) {
                return;
            }
            BannerRenderState backBanner = new BannerRenderState();
            backBanner.baseColor = bannerItem.getColor();
            backBanner.patterns = layers;
            state.backBanner = backBanner;
            return;
        }


        if (showItem.getItem() instanceof BlockItem blockItem) {
            state.headBlockState = blockItem.getBlock().defaultBlockState();
            return;
        }
    }

    private static void extractChatBubbleState(EntityMaid maid, EntityMaidRenderState state, float partialTicks) {
        // 暂定只能女仆显示
        if (state.modelInfo == null || !MaidConfig.GLOBAL_MAID_SHOW_CHAT_BUBBLE.get() || !maid.getConfigManager().isChatBubbleShow()) {
            return;
        }

        Vec3 bubbleOffset = maid.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, maid.getViewYRot(partialTicks));
        if (bubbleOffset == null || !ClientHooks.isNameplateInRenderDistance(maid, state.distanceToCameraSq)) {
            return;
        }

        // 依据模型的缩放大小进行 Y 位移
        float scale = state.modelInfo.getRenderEntityScale();
        if (state.modelInfo.isGeckoModel()) {
            // GeckoLib 模型一般偏高
            bubbleOffset = bubbleOffset.multiply(1, scale + 0.3, 1);
        } else {
            bubbleOffset = bubbleOffset.multiply(1, scale, 1);
        }

        var chatBubble = maid.getChatBubbleManager().getChatBubbleDataCollection();
        if (chatBubble == null || chatBubble.isEmpty()) {
            return;
        }

        state.showBubble = true;
        state.bubbleOffset = bubbleOffset;
        state.chatBubble = chatBubble;
    }

    private static void extractBackpackState(EntityMaid maid, EntityMaidRenderState state, ItemModelResolver itemModelResolver) {
        if (maid.isSleeping() || maid.isInvisible()) {
            return;
        }
        state.showBackpack = maid.getConfigManager().isShowBackpack();
        state.backpack = state.showBackpack ? maid.getMaidBackpackType() : BackpackManager.getEmptyBackpack();

        ItemStack showItem = maid.getBackpackShowItem();
        // 只有工具类物品才会显示在背部
        if (showItem.has(DataComponents.TOOL)) {
            itemModelResolver.updateForLiving(state.backItem, showItem, ItemDisplayContext.FIXED, maid);
        }
    }

    @SuppressWarnings("unchecked")
    private static void extractGeckoState(EntityMaidRenderState state, @Nullable GeckoMaidEntity<? extends EntityMaid> geckoEntity) {
        if (geckoEntity != null) {
            if (state.modelInfo != null && state.modelInfo.isGeckoModel()) {
                state.modelType = ModelType.GECKO;
                geckoEntity.setMaidInfo(state.modelInfo);
                state.geckoUpdateTask = (GeckoUpdateTask<GeckoMaidRenderData>) geckoEntity.createUpdateTask(state);
            } else {
                geckoEntity.reset();
            }
        }
        if (state.modelType == ModelType.NONE) {
            state.modelType = ModelType.SIMPLE_BEDROCK;
        }
    }
}
