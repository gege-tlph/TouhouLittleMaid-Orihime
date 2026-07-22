package com.github.tartaricacid.touhoulittlemaid.entity.passive;

import java.util.Objects;
import net.minecraft.world.item.component.CustomData;
import cn.sh1rocu.touhoulittlemaid.api.extension.IEntity;
import cn.sh1rocu.touhoulittlemaid.mixin.accessor.ExperienceOrbAccessor;
import cn.sh1rocu.touhoulittlemaid.util.block.BlockUtil;
import cn.sh1rocu.touhoulittlemaid.util.enchant.EnchantmentUtil;
import cn.sh1rocu.touhoulittlemaid.util.forge.CommonHooks;
import cn.sh1rocu.touhoulittlemaid.util.itemhandler.*;
import cn.sh1rocu.touhoulittlemaid.util.itemhandler.entity.EntityArmorInvWrapper;
import cn.sh1rocu.touhoulittlemaid.util.itemhandler.entity.EntityHandsInvWrapper;

import com.github.tartaricacid.simplebedrockmodel.client.bedrock.model.BedrockPart;
import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.advancements.maid.TriggerType;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.MaidAIChatManager;
import com.github.tartaricacid.touhoulittlemaid.api.backpack.IBackpackData;
import com.github.tartaricacid.touhoulittlemaid.api.backpack.IMaidBackpack;
import com.github.tartaricacid.touhoulittlemaid.api.client.render.MaidRenderState;
import com.github.tartaricacid.touhoulittlemaid.api.entity.IMaid;
import cn.sh1rocu.touhoulittlemaid.api.entity.data.TaskDataKey;
import com.github.tartaricacid.touhoulittlemaid.api.event.*;
import com.github.tartaricacid.touhoulittlemaid.api.task.IAttackTask;
import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import com.github.tartaricacid.touhoulittlemaid.api.task.IRangedAttackTask;

import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.EntityMaidModel;

import com.github.tartaricacid.touhoulittlemaid.client.resource.loader.CustomPackLoader;

import com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.MaidModelInfo;

import com.github.tartaricacid.touhoulittlemaid.compat.ysm.YsmCompat;

import com.github.tartaricacid.touhoulittlemaid.compat.ysm.event.YsmMaidClientTickEvent;
import com.github.tartaricacid.touhoulittlemaid.config.ServerConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MiscConfig;
import com.github.tartaricacid.touhoulittlemaid.data.MaidNumAttachment;

import com.github.tartaricacid.touhoulittlemaid.datagen.tag.TagEntity;
import com.github.tartaricacid.touhoulittlemaid.datagen.tag.TagItem;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.MaidBrain;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.MaidSchedule;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.control.MaidMoveControl;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.navigation.MaidPathNavigation;
import com.github.tartaricacid.touhoulittlemaid.entity.backpack.*;
import com.github.tartaricacid.touhoulittlemaid.entity.backpack.data.TankBackpackData;
import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.ChatBubbleDataCollection;
import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.ChatBubbleManager;
import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.ChatBubbleRegister;
import com.github.tartaricacid.touhoulittlemaid.entity.data.MaidTaskDataMaps;
import com.github.tartaricacid.touhoulittlemaid.entity.favorability.FavorabilityManager;
import com.github.tartaricacid.touhoulittlemaid.entity.favorability.Type;

import com.github.tartaricacid.touhoulittlemaid.entity.info.ServerCustomPackLoader;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityPowerPoint;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityTombstone;
import com.github.tartaricacid.touhoulittlemaid.entity.projectile.MaidFishingHook;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskIdle;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import com.github.tartaricacid.touhoulittlemaid.init.*;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.backpack.BaubleContainer;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.config.MaidConfigContainer;
import com.github.tartaricacid.touhoulittlemaid.inventory.handler.BaubleItemHandler;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.ComponentSerialization;
import com.github.tartaricacid.touhoulittlemaid.entity.backpack.SmallBackpack;
import com.github.tartaricacid.touhoulittlemaid.entity.backpack.MiddleBackpack;
import com.github.tartaricacid.touhoulittlemaid.entity.backpack.BigBackpack;
import com.github.tartaricacid.touhoulittlemaid.inventory.handler.MaidBackpackHandler;
import com.github.tartaricacid.touhoulittlemaid.inventory.handler.MaidHandsInvWrapper;
import com.github.tartaricacid.touhoulittlemaid.inventory.handler.MaidInvWrapper;

import com.github.tartaricacid.touhoulittlemaid.item.ItemFilm;
import com.github.tartaricacid.touhoulittlemaid.mixin.accessor.ArrowAccessor;
import com.github.tartaricacid.touhoulittlemaid.network.NetworkHandler;
import com.github.tartaricacid.touhoulittlemaid.network.message.ItemBreakPackage;
import com.github.tartaricacid.touhoulittlemaid.network.message.SyncYsmMaidDataPackage;
import com.github.tartaricacid.touhoulittlemaid.network.message.*;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import com.github.tartaricacid.touhoulittlemaid.util.ParseI18n;
import com.github.tartaricacid.touhoulittlemaid.util.ResourceLocationUtil;
import com.github.tartaricacid.touhoulittlemaid.util.TeleportHelper;
import com.github.tartaricacid.touhoulittlemaid.world.backups.MaidBackupsManager;
import com.github.tartaricacid.touhoulittlemaid.world.data.MaidWorldData;
import com.google.common.collect.Lists;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.chat.*;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.*;

import static com.github.tartaricacid.touhoulittlemaid.config.ServerConfig.MAID_AI_TIME_DEBUG;

import static com.github.tartaricacid.touhoulittlemaid.datagen.EnchantmentKeys.getEnchantmentLevel;
import static com.github.tartaricacid.touhoulittlemaid.init.InitDataAttachment.MAID_NUM;
import static com.github.tartaricacid.touhoulittlemaid.init.InitDataComponent.MODEL_ID_TAG_NAME;

public class EntityMaid extends TamableAnimal implements CrossbowAttackMob, IMaid, IEntity {
    private boolean isAddedToLevel;

    public static final EntityType<EntityMaid> TYPE = EntityType.Builder.<EntityMaid>of(EntityMaid::new, MobCategory.CREATURE)
            .sized(0.6f, 1.5f).clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocationUtil.getResourceLocation("maid")));

    // YSM 女仆兼容内容
    public static final String IS_YSM_MODEL_TAG = "IsYsmModel";
    public static final String YSM_MODEL_ID_TAG = "YsmModelId";
    public static final String YSM_MODEL_TEXTURE_TAG = "YsmModelTexture";
    public static final String YSM_MODEL_NAME_TAG = "YsmModelName";
    public static final String YSM_ROULETTE_ANIM_TAG = "YsmRouletteAnim";
    public static final String YSM_ROAMING_VARS_TAG = "YsmRoamingVars";
    public static final String YSM_ROAMING_VARS_COUNT_TAG = "YsmRoamingVarsCount";
    public static final String YSM_ROAMING_UPDATE_FLAG_TAG = "YsmRoamingUpdateFlag";

    // 女仆默认属性
    public static final String MODEL_ID_TAG = MODEL_ID_TAG_NAME;
    public static final String SOUND_PACK_ID_TAG = "SoundPackId";
    public static final String MAID_BACKPACK_TYPE = "MaidBackpackType";
    public static final String MAID_INVENTORY_TAG = "MaidInventory";
    public static final String MAID_BAUBLE_INVENTORY_TAG = "MaidBaubleInventory";
    public static final String MAID_HIDE_INVENTORY_TAG = "MaidHideInventory";
    public static final String MAID_TASK_INVENTORY_TAG = "MaidTaskInventory";
    public static final String EXPERIENCE_TAG = "MaidExperience";

    // AI 超时检测
    private static final long WARNING_TIME_NANOS = Duration.ofMillis(50L).toNanos();
    // 女仆传送到主人处的最大尝试次数
    private static final int MAX_TELEPORT_ATTEMPTS_TIMES = 10;
    // 饰品栏容量
    public static final int BAUBLE_INV_SIZE = 30;

    // YSM 女仆兼容同步数据
    private static final EntityDataAccessor<Boolean> DATA_IS_YSM_MODEL = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_YSM_MODEL_ID = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_YSM_MODEL_TEXTURE = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Component> DATA_YSM_MODEL_NAME = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.COMPONENT);

    // 女仆默认同步数据
    private static final EntityDataAccessor<String> DATA_MODEL_ID = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_SOUND_PACK_ID = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_TASK = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_BEGGING = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_INVULNERABLE = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_HUNGER = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FAVORABILITY = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_EXPERIENCE = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_STRUCK_BY_LIGHTNING = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_CHARGING_CROSSBOW = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ARM_RISE = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<MaidSchedule> SCHEDULE_MODE = SynchedEntityData.defineId(EntityMaid.class, MaidSchedule.DATA);
    private static final EntityDataAccessor<BlockPos> RESTRICT_CENTER = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Float> RESTRICT_RADIUS = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<ChatBubbleDataCollection> CHAT_BUBBLE = SynchedEntityData.defineId(EntityMaid.class, ChatBubbleRegister.INSTANCE);
    private static final EntityDataAccessor<String> BACKPACK_TYPE = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<ItemStack> BACKPACK_ITEM_SHOW = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<String> BACKPACK_FLUID = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.STRING);

    // 给卓越前线之类的枪械模组使用的，标记女仆是否处于 aim 状态
    private static final EntityDataAccessor<Boolean> DATA_IS_AIMING = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.BOOLEAN);


    static final EntityDataAccessor<Byte> GAME_STATUE = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.BYTE);

    // 给 MaidConfigManager 用的，必须在这里声明，避免 ID 不同步
    static final EntityDataAccessor<Boolean> DATA_PICKUP = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Boolean> DATA_HOME_MODE = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Boolean> DATA_RIDEABLE = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Boolean> BACKPACK_SHOW = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Boolean> BACK_ITEM_SHOW = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Boolean> CHATBUBBLE_SHOW = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Float> SOUND_FREQ = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Integer> PICKUP_TYPE = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<Boolean> OPEN_DOOR = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Boolean> OPEN_FENCE_GATE = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Boolean> ACTIVE_CLIMBING = SynchedEntityData.defineId(EntityMaid.class, EntityDataSerializers.BOOLEAN);

    /**
     * 开辟空间给任务存储使用,也便于附属模组存储数据
     */

    private static final String TASK_TAG = "MaidTask";
    private static final String STRUCK_BY_LIGHTNING_TAG = "StruckByLightning";
    private static final String INVULNERABLE_TAG = "Invulnerable";
    private static final String HUNGER_TAG = "MaidHunger";
    private static final String FAVORABILITY_TAG = "MaidFavorability";
    private static final String SCHEDULE_MODE_TAG = "MaidScheduleMode";
    private static final String BACKPACK_DATA_TAG = "MaidBackpackData";
    private static final String STRUCTURE_SPAWN_TAG = "StructureSpawn";
    private static final String DEFAULT_MODEL_ID = "touhou_little_maid:hakurei_reimu";

    // 弃用数据，仅用于旧版存档的迁移
    private static final @Deprecated String BACKPACK_LEVEL_TAG = "MaidBackpackLevel";
    private static final @Deprecated String RESTRICT_CENTER_TAG = "MaidRestrictCenter";

    public final ItemStack[] handItemsForAnimation = new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY};

    // 物品存储相关
    private final EntityArmorInvWrapper armorInvWrapper = new EntityArmorInvWrapper(this);
    private final EntityHandsInvWrapper handsInvWrapper = new MaidHandsInvWrapper(this);
    private final ItemStackHandler maidInv = new MaidBackpackHandler(36, this);
    private final BaubleItemHandler maidBauble = new BaubleItemHandler(BAUBLE_INV_SIZE);
    // 用于暂存副手物品的物品栏
    private final ItemStackHandler hideInv = new ItemStackHandler(1);
    // 用于工作任务可能需要的物品栏
    private final ItemStackHandler taskInv = new ItemStackHandler(9);

    private final MaidKillRecordManager killRecordManager = new MaidKillRecordManager();
    private final ChatBubbleManager chatBubbleManager = new ChatBubbleManager(this);
    private final MaidTaskDataMaps taskDataMaps = new MaidTaskDataMaps();
    private final FavorabilityManager favorabilityManager;
    private final MaidSwimManager swimManager;
    // 控制不同的 navigation 切换的条件以及切换后变更女仆相关的 AI 控制参数
    private final MaidNavigationManager navigationManager;
    private final MaidAIChatManager aiChatManager;
    private final SchedulePos schedulePos;
    private final ItemCooldowns cooldowns;

    public boolean guiOpening = false;
    public MaidFishingHook fishing = null;

    public MaidRenderState renderState = MaidRenderState.ENTITY;
    public boolean rouletteAnimPlaying = false;
    public String rouletteAnim = "empty";
    public boolean rouletteAnimDirty = false;
    public int roamingVarsUpdateFlag = 0;
    public Object2FloatOpenHashMap<String> roamingVars = new Object2FloatOpenHashMap<>();

    /**
     * 用于方便特殊动画播放的变量，目前仅支持捡雪球
     */
    public int animationId = 0;
    public long animationRecordTime = -1L;
    public boolean shouldReset = false;


    private List<SendEffectPackage.EffectData> effects = Lists.newArrayList();
    private IMaidTask task = TaskManager.getIdleTask();
    private IMaidBackpack backpack = BackpackManager.getEmptyBackpack();
    private int playerHurtSoundCount = 120;
    private int pickupSoundCount = 5;
    private int backpackDelay = 0;
    private int passiveUseShieldTick = 0;
    private IBackpackData backpackData = null;

    private MaidConfigManager configManager = new MaidConfigManager(this.entityData);
    private MaidGameRecordManager gameRecordManager = new MaidGameRecordManager(this);

    /**
     * 女仆现在可以在前哨站生成，那么会打上这个标签
     */
    private boolean structureSpawn = false;
    /**
     * 女仆主动爬行标志位，用于管控女仆当前时刻需不需要攀爬
     */
    private boolean canClimb = false;
    /**
     * 一个记录女仆已经生成墓碑的变量，避免死亡重复生成墓碑
     */
    private boolean alreadyDropped = false;

    /**
     * 爬梯的计时器，用于在爬梯后的一段时间内禁用摔落伤害
     */
    private int climbFallDelayTicks = 0;

    protected EntityMaid(EntityType<EntityMaid> type, Level world) {
        super(type, world);
        this.favorabilityManager = new FavorabilityManager(this);
        this.aiChatManager = new MaidAIChatManager(this);

        // 尝试修复 https://github.com/TartaricAcid/TouhouLittleMaid/issues/631
        ResourceKey<Level> dimension = Objects.requireNonNullElse(world.dimension(), Level.OVERWORLD);
        this.schedulePos = new SchedulePos(BlockPos.ZERO, dimension.identifier());

        this.moveControl = new MaidMoveControl(this);
        this.swimManager = new MaidSwimManager(this);
        this.navigationManager = new MaidNavigationManager(this);

        this.cooldowns = new ItemCooldowns();

        // 启用实体持久化，也许能解决难以复现的女仆实体丢失问题
        this.setPersistenceRequired();
    }

    public EntityMaid(Level worldIn) {
        this(TYPE, worldIn);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                // 目前仅用于寻路，女仆最大可寻路 64 格
                .add(Attributes.FOLLOW_RANGE, 64)
                .add(Attributes.ATTACK_KNOCKBACK)
                .add(Attributes.ATTACK_DAMAGE)
                .add(Attributes.SWEEPING_DAMAGE_RATIO)
                // 目前仅用于寻路，女仆最大可寻路 64 格
                .add(Attributes.LUCK)
                // 女仆攻击速度，这个数字表示每秒可施展的攻击次数，会受武器本身的攻击速度影响
                .add(Attributes.ATTACK_SPEED)
                // 用于女仆近战的范围判断
                .add(Attributes.ENTITY_INTERACTION_RANGE, 2)
                .add(InitAttribute.MAID_USE_ITEM_SPEED)
                .add(InitAttribute.MAID_CROSSBOW_ATTACK_SPEED)
                .add(InitAttribute.MAID_SHOOT_COOLDOWN)
                .add(InitAttribute.MAID_TRIDENT_COOLDOWN)
                .add(InitAttribute.MAID_PICKUP_RANGE)
                .add(InitAttribute.MAID_PASSIVE_USE_SHIELD_TICK)
                .add(InitAttribute.MAID_HUNGER);
    }

    public static boolean canInsertItem(ItemStack stack) {
        Identifier key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key != null && MaidConfig.MAID_BACKPACK_BLACKLIST.get().contains(key.toString())) {
            return false;
        }
        return stack.getItem().canFitInsideContainerItems();
    }

    public static EntityDataAccessor<ChatBubbleDataCollection> getChatBubbleKey() {
        return CHAT_BUBBLE;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);

        builder.define(DATA_IS_YSM_MODEL, false);
        builder.define(DATA_YSM_MODEL_ID, StringUtils.EMPTY);
        builder.define(DATA_YSM_MODEL_TEXTURE, StringUtils.EMPTY);
        builder.define(DATA_YSM_MODEL_NAME, Component.empty());

        builder.define(DATA_MODEL_ID, DEFAULT_MODEL_ID);
        builder.define(DATA_SOUND_PACK_ID, DefaultMaidSoundPack.getInitSoundPackId());
        builder.define(DATA_TASK, TaskIdle.UID.toString());
        builder.define(DATA_BEGGING, false);
        builder.define(DATA_INVULNERABLE, false);
        builder.define(DATA_HUNGER, 0);
        builder.define(DATA_FAVORABILITY, 0);
        builder.define(DATA_EXPERIENCE, 0);
        builder.define(DATA_STRUCK_BY_LIGHTNING, false);
        builder.define(DATA_IS_CHARGING_CROSSBOW, false);
        builder.define(DATA_ARM_RISE, false);
        builder.define(SCHEDULE_MODE, MaidSchedule.DAY);
        builder.define(RESTRICT_CENTER, BlockPos.ZERO);
        builder.define(RESTRICT_RADIUS, MaidConfig.MAID_NON_HOME_RANGE.get().floatValue());
        builder.define(CHAT_BUBBLE, ChatBubbleDataCollection.getEmptyCollection());
        builder.define(BACKPACK_TYPE, EmptyBackpack.ID.toString());
        builder.define(BACKPACK_ITEM_SHOW, ItemStack.EMPTY);
        builder.define(BACKPACK_FLUID, StringUtils.EMPTY);


        builder.define(DATA_IS_AIMING, false);

        // 父类构造方法调用此类，就会出现这种初始化混乱的问题
        if (this.configManager == null) {
            this.configManager = new MaidConfigManager(this.entityData);
        }
        this.configManager.defineSynchedData(builder);
        if (this.gameRecordManager == null) {
            this.gameRecordManager = new MaidGameRecordManager(this);
        }
        this.gameRecordManager.defineSynchedData(builder);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);

    }

    /**
     * 获取注册的数据
     */
    @Nullable
    public <T> T getData(TaskDataKey<T> dataKey) {
        return this.taskDataMaps.getData(dataKey);
    }

    /**
     * 创建或获取注册的数据
     */
    public <T> T getOrCreateData(TaskDataKey<T> dataKey, T defaultValue) {
        return this.taskDataMaps.getOrCreateData(dataKey, defaultValue);
    }

    /**
     * 设置数据
     */
    public <T> void setData(TaskDataKey<T> dataKey, T value) {
        this.taskDataMaps.setData(dataKey, value);
    }

    /**
     * 设置数据，并将其同步到客户端
     */
    public <T> void setAndSyncData(TaskDataKey<T> dataKey, T value) {
        this.setData(dataKey, value);
        if (!this.level.isClientSide()) {
            NetworkHandler.sendToPlayersTrackingEntity(this,
                    new SyncMaidTaskDataPackage(this.getId(), this.taskDataMaps.getUpdateTag()));
        }
    }

    @Environment(EnvType.CLIENT)
    public void readTaskDataFromServer(CompoundTag taskData) {
        this.taskDataMaps.readFromServer(taskData);
    }

    public CompoundTag getTaskDataUpdateTag() {
        return this.taskDataMaps.getUpdateTag();
    }

    @Override
    protected PathNavigation createNavigation(Level levelIn) {
        return new MaidPathNavigation(this, levelIn);
    }

    @Override
    @SuppressWarnings("all")
    public Brain<EntityMaid> getBrain() {
        return (Brain<EntityMaid>) super.getBrain();
    }

    @Override
    protected Brain.Provider<EntityMaid> brainProvider() {
        return Brain.provider(MaidBrain.getMemoryTypes(), MaidBrain.getSensorTypes());
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamicIn) {
        Brain<EntityMaid> brain = this.brainProvider().makeBrain(dynamicIn);
        MaidBrain.registerBrainGoals(brain, this);
        return brain;
    }

    public void refreshBrain(ServerLevel serverWorldIn) {
        Brain<EntityMaid> brain = this.getBrain();
        brain.stopAll(serverWorldIn, this);
        this.brain = brain.copyWithoutBehaviors();
        MaidBrain.registerBrainGoals(this.getBrain(), this);
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        long timeRecord = Util.getNanos();
        if (!guiOpening) {
            this.getBrain().tick((ServerLevel) this.level, this);
        }
        timeRecord = Util.getNanos() - timeRecord;
        if (MAID_AI_TIME_DEBUG.get() && timeRecord > WARNING_TIME_NANOS) {
            double timeMs = timeRecord / 1000000.0;
            BlockPos blockPos = this.blockPosition();
            String taskId = this.getTask().getUid().toString();
            int searchRange = this.getHomeRadius();

            TouhouLittleMaid.LOGGER.error("Maid's AI taking too long! Time: {} ms, Pos: ({},{},{}), Task ID: {}, Search Range: {}",
                    timeMs, blockPos.getX(), blockPos.getY(), blockPos.getZ(), taskId, searchRange);
        }
        super.customServerAiStep(serverLevel);
    }

    @Override
    public void tick() {
        MaidTickEvent maidTickEvent = new MaidTickEvent(this);
        MaidTickEvent.CALLBACK.invoker().post(maidTickEvent);
        if (!maidTickEvent.isCanceled()) {
            super.tick();
            maidBauble.fireEvent((b, s) -> {
                b.onTick(this, s);
                return false;
            });
        }
        if (YsmCompat.isInstalled() && this.isYsmModel()) {
            if (level.isClientSide()) {
                // 触发 ysm 模型的客户端事件
                YsmMaidClientTickEvent.CALLBACK.invoker().post(new YsmMaidClientTickEvent(this));
            }
            // 同步 ysm 轮盘数据
            if (!level.isClientSide() && this.rouletteAnimDirty) {
                this.rouletteAnimDirty = false;
                SyncYsmMaidDataPackage message = new SyncYsmMaidDataPackage(this.getId(), this.rouletteAnim, this.rouletteAnimPlaying, this.roamingVars);
                NetworkHandler.sendToPlayersTrackingEntity(this, message);
            }
        }

        // 自 1.4.2 版本起强制开启女仆备份机制
        int saveIntervalTick = ServerConfig.MAID_BACKUP_INTERVAL_SECONDS.get() * 20;
        // 通过哈希计算出一个随机值，这样做可以避免所有实体都在同一 tick 进行保存
        int checkTick = Math.abs(this.getUUID().hashCode()) % saveIntervalTick;
        if (this.level.getGameTime() % saveIntervalTick == checkTick && this.level instanceof ServerLevel serverLevel) {
            MaidBackupsManager.save(serverLevel.getServer(), this);
        }
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (backpackDelay > 0) {
            backpackDelay--;
        }
        if (playerHurtSoundCount > 0) {
            playerHurtSoundCount--;
        }
        if (climbFallDelayTicks > 0) {
            climbFallDelayTicks--;
            this.fallDistance = 0;
        }
        this.spawnPortalParticle();
        this.randomRestoreHealth();
        this.onMaidSleep();

        this.gameRecordManager.tick();
    }

    @Override
    public void rideTick() {
        super.rideTick();

        Entity vehicle = this.getVehicle();
        if (vehicle != null && !vehicle.getType().is(TagEntity.MAID_VEHICLE_ROTATE_BLOCKLIST)) {
            this.setYHeadRot(vehicle.getYRot());
            this.setYBodyRot(vehicle.getYRot());
        }
    }


    private void onMaidSleep() {
        if (isSleeping()) {
            getSleepingPos().ifPresent(pos -> setPos(pos.getX() + 0.5, pos.getY() + 0.5625, pos.getZ() + 0.5));
            setDeltaMovement(Vec3.ZERO);
            if (!isSilent()) {
                this.setSilent(true);
            }
        } else {
            if (isSilent()) {
                this.setSilent(false);
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.updateSwingTime();
        this.navigationManager.tick();
        if (!level.isClientSide()) {
            this.chatBubbleManager.tick();
            if (this.backpackData != null) {
                this.backpackData.serverTick(this);
            }

            this.favorabilityManager.tick();

            this.schedulePos.tick(this);

            this.cooldowns.tick();
            if (this.passiveUseShieldTick > 0) {
                // 如果没有拿着盾牌，直接取消计时，避免疯狂挥手
                ItemStack offHandItem = this.getItemInHand(InteractionHand.OFF_HAND);
                if (offHandItem.has(DataComponents.BLOCKS_ATTACKS)) {
                    this.passiveUseShieldTick--;
                } else {
                    this.passiveUseShieldTick = 1;
                }
                // 最后 1 tick 取消盾牌
                if (this.passiveUseShieldTick == 1 && this.isUsingItem() && this.getUsedItemHand() == InteractionHand.OFF_HAND) {
                    this.stopUsingItem();
                }
            }
        }
    }

    @Override
    public InteractionResult mobInteract(Player playerIn, InteractionHand hand) {
        // 禁止 fake player 交互女仆
        if (playerIn instanceof FakePlayer) {
            return InteractionResult.PASS;
        }
        if (hand == InteractionHand.MAIN_HAND && isOwnedBy(playerIn)) {
            ItemStack stack = playerIn.getMainHandItem();
            InteractMaidEvent event = new InteractMaidEvent(playerIn, this, stack);
            InteractMaidEvent.CALLBACK.invoker().post(event);
            // 利用短路原理，逐个触发对应的交互事件
            if (event.isCanceled()
                    || stack.interactLivingEntity(playerIn, this, hand).consumesAction()
                    || openMaidGui(playerIn)) {
                return InteractionResult.SUCCESS;
            }
        } else {
            return tameMaid(playerIn.getItemInHand(hand), playerIn);
        }
        return InteractionResult.PASS;
    }

    private InteractionResult tameMaid(ItemStack stack, Player player) {
        MaidNumAttachment cap = player.getAttached(MAID_NUM);
        if (cap.canAdd() || player.isCreative()) {
            boolean isNormal = !isTame() && getTamedItem().test(stack);
            boolean isNtr = getNtrItem().test(stack);
            if (isNormal || isNtr) {
                if (!player.isCreative()) {
                    stack.shrink(1);
                    cap.add();
                }
                this.tame(player);
                // 清掉寻路，清掉敌对记忆
                this.navigation.stop();
                this.setTarget(null);
                this.brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
                this.level.broadcastEntityEvent(this, EntityEvent.TAMING_SUCCEEDED);
                this.playSound(InitSounds.MAID_TAMED, 1, 1);
                if (player instanceof ServerPlayer serverPlayer) {
                    InitTrigger.MAID_EVENT.trigger(serverPlayer, TriggerType.TAMED_MAID);
                    if (this.isStructureSpawn()) {
                        InitTrigger.MAID_EVENT.trigger(serverPlayer, TriggerType.TAMED_MAID_FROM_STRUCTURE);
                    }
                }
                // 触发事件
                MaidTamedEvent.CALLBACK.invoker().onMaidTamed(new MaidTamedEvent(this, player, isNtr));
                return InteractionResult.SUCCESS;
            }
        } else {
            if (player instanceof ServerPlayer) {
                MutableComponent msg = Component.translatable("message.touhou_little_maid.owner_maid_num.can_not_add",
                        cap.get(), cap.getMaxNum());
                player.displayClientMessage(msg, false);
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void pushEntities() {
        super.pushEntities();
        // 只有拾物模式开启，驯服状态下才可以捡起物品
        if (this.isPickup() && this.isTame()) {
            AABB pickupBox;
            AttributeInstance attribute = this.getAttribute(InitAttribute.MAID_PICKUP_RANGE);
            if (attribute != null) {
                pickupBox = this.getBoundingBox().inflate(attribute.getValue());
            } else {
                pickupBox = this.getBoundingBox().inflate(0.5);
            }

            List<Entity> entityList = this.level.getEntities(this, pickupBox, this::canPickup);
            if (!entityList.isEmpty() && this.isAlive()) {
                for (Entity entityPickup : entityList) {
                    // 如果是物品
                    if (entityPickup instanceof ItemEntity) {
                        pickupItem((ItemEntity) entityPickup, false);
                    }
                    // 如果是经验
                    if (entityPickup instanceof ExperienceOrb) {
                        pickupXPOrb((ExperienceOrb) entityPickup);
                    }
                    // 如果是 P 点
                    if (entityPickup instanceof EntityPowerPoint) {
                        pickupPowerPoint((EntityPowerPoint) entityPickup);
                    }
                    // 如果是箭
                    if (entityPickup instanceof AbstractArrow) {
                        pickupArrow((AbstractArrow) entityPickup, false);
                    }
                }
            }
        }
    }

    public boolean pickupItem(ItemEntity entityItem, boolean simulate) {
        MaidPickupEvent.ItemResultPre resultPre = new MaidPickupEvent.ItemResultPre(this, entityItem, simulate);
        MaidPickupEvent.ITEM_RESULT_PRE.invoker().onItemResultPre(resultPre);
        if (resultPre.isCanceled()) {
            return resultPre.isCanPickup();
        }
        if (!level.isClientSide() && entityItem.isAlive() && !entityItem.hasPickUpDelay()) {
            // 获取实体的物品堆
            ItemStack itemstack = entityItem.getItem();
            // 检查物品是否合法
            if (!canInsertItem(itemstack)) {
                return false;
            }
            // 获取数量，为后面方面用
            int count = itemstack.getCount();
            itemstack = ItemHandlerHelper.insertItemStacked(getAvailableInv(false), itemstack, simulate);
            if (count == itemstack.getCount()) {
                return false;
            }
            if (!simulate) {
                // 这是向客户端同步数据用的，如果加了这个方法，会有短暂的拾取动画和音效
                this.take(entityItem, count - itemstack.getCount());
                this.tryPlayMaidPickupSound();
                ItemStack copy = new ItemStack(itemstack.getItem(), count - itemstack.getCount());
                // 如果遍历塞完后发现为空了
                if (itemstack.isEmpty()) {
                    // 清除这个实体
                    entityItem.discard();
                } else {
                    // 将物品数量同步到客户端
                    entityItem.setItem(itemstack);
                }
                MaidPickupEvent.ITEM_RESULT_POST.invoker().onItemResultPost(new MaidPickupEvent.ItemResultPost(this, copy));
            }
            return true;
        }
        return false;
    }

    public void pickupXPOrb(ExperienceOrb entityXPOrb) {
        MaidPickupEvent.ExperienceResult expResult = new MaidPickupEvent.ExperienceResult(this, entityXPOrb, false);
        MaidPickupEvent.EXPERIENCE_RESULT.invoker().onExperienceResult(expResult);
        if (expResult.isCanceled()) {
            return;
        }
        if (!this.level.isClientSide() && entityXPOrb.isAlive() && entityXPOrb.tickCount > 2) {
            // 这是向客户端同步数据用的，如果加了这个方法，会有短暂的拾取动画和音效
            this.take(entityXPOrb, 1);
            this.tryPlayMaidPickupSound();

            // 对经验修补的应用，因为全部来自于原版，所以效果也是相同的
            IItemHandler allItems = new CombinedInvWrapper(armorInvWrapper, handsInvWrapper, maidBauble);
            ItemStack itemstack = this.getRandomItemWithMendingEnchantments(allItems);
            if (!itemstack.isEmpty() && itemstack.isDamaged()) {
                int i = Math.min((int) (entityXPOrb.getValue()), itemstack.getDamageValue());
                ((ExperienceOrbAccessor) entityXPOrb).invokeSetValue((entityXPOrb.getValue() - i / 2));
                itemstack.setDamageValue(itemstack.getDamageValue() - i);
            }
            if (entityXPOrb.getValue() > 0) {
                this.setExperience(getExperience() + entityXPOrb.getValue());
            }
            entityXPOrb.discard();
        }
    }

    public void pickupPowerPoint(EntityPowerPoint powerPoint) {
        MaidPickupEvent.PowerPointResult pointResult = new MaidPickupEvent.PowerPointResult(this, powerPoint, false);
        MaidPickupEvent.POWERPOINT_RESULT.invoker().onPowerPointResult(pointResult);
        if (pointResult.isCanceled()) {
            return;
        }
        if (!this.level.isClientSide() && powerPoint.isAlive() && powerPoint.throwTime == 0) {
            // 这是向客户端同步数据用的，如果加了这个方法，会有短暂的拾取动画和音效
            powerPoint.take(this, 1);
            this.tryPlayMaidPickupSound();

            // 对经验修补的应用，因为全部来自于原版，所以效果也是相同的
            CombinedInvWrapper allItems = this.getAllInv();
            ItemStack itemstack = this.getRandomItemWithMendingEnchantments(allItems);
            int xpValue = EntityPowerPoint.transPowerValueToXpValue(powerPoint.getValue());
            if (!itemstack.isEmpty() && itemstack.isDamaged()) {
                int i = Math.min((int) (xpValue), itemstack.getDamageValue());
                xpValue -= (i / 2);
                itemstack.setDamageValue(itemstack.getDamageValue() - i);
            }
            if (xpValue > 0) {
                this.setExperience(getExperience() + xpValue);
            }
            powerPoint.discard();
        }
    }

    private ItemStack getRandomItemWithMendingEnchantments(IItemHandler handler) {

        RegistryAccess access = this.level.registryAccess();
        List<ItemStack> stacks = Lists.newArrayList();
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stackInSlot = handler.getStackInSlot(i);
            if (!stackInSlot.isEmpty() && getEnchantmentLevel(access, Enchantments.MENDING, stackInSlot) > 0
                    && stackInSlot.isDamaged() && !stackInSlot.is(TagItem.MAID_MENDING_BLOCKLIST_ITEM)) {
                stacks.add(stackInSlot);
            }
        }
        return stacks.isEmpty() ? ItemStack.EMPTY : stacks.get(this.getRandom().nextInt(stacks.size()));
    }

    public boolean pickupArrow(AbstractArrow arrow, boolean simulate) {
        MaidPickupEvent.ArrowResult arrowResult = new MaidPickupEvent.ArrowResult(this, arrow, simulate);
        MaidPickupEvent.ARROW_RESULT.invoker().onArrowResult(arrowResult);
        if (arrowResult.isCanceled()) {
            return arrowResult.isCanPickup();
        }
        if (!this.level.isClientSide() && arrow.isAlive() && arrow.shakeTime <= 0) {
            // 先判断箭是否处于可以拾起的状态
            if (arrow.pickup != AbstractArrow.Pickup.ALLOWED) {
                return false;
            }
            // 能够塞入
            ItemStack stack = getArrowFromEntity(arrow);
            if (stack.isEmpty()) {
                return false;
            }
            if (!ItemHandlerHelper.insertItemStacked(getAvailableInv(false), stack, simulate).isEmpty()) {
                return false;
            }
            // 非模拟状态下，清除实体箭
            if (!simulate) {
                // 这是向客户端同步数据用的，如果加了这个方法，会有短暂的拾取动画和音效
                this.take(arrow, 1);
                this.tryPlayMaidPickupSound();
                arrow.discard();
            }
            return true;
        }
        return false;
    }

    public void tryPlayMaidPickupSound() {
        var event = new MaidPlaySoundEvent(this);
        MaidPlaySoundEvent.CALLBACK.invoker().post(event);
        if (!event.isCanceled()) {
            pickupSoundCount--;
            if (pickupSoundCount == 0) {
                this.playSound(InitSounds.MAID_ITEM_GET, 1, 1);
                pickupSoundCount = 5;
            }
        }
    }

    private ItemStack getArrowFromEntity(AbstractArrow entity) {
        if (entity instanceof ArrowAccessor mixinArrow) {
            if (mixinArrow.tlmInGround() || entity.isNoPhysics()) {
                return mixinArrow.getTlmPickupItem();
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isWithinMeleeAttackRange(LivingEntity target) {
        int attackPlusDistance = this.favorabilityManager.getAttackDistancePlusByPoint(this.getFavorability());
        double attackDistance = this.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE) + attackPlusDistance;
        return this.distanceTo(target) < attackDistance;
    }

    @Override
    public boolean doHurtTarget(ServerLevel serverLevel, Entity target) {
        MaidHurtTarget.Pre event = new MaidHurtTarget.Pre(this, target);
        MaidHurtTarget.PRE.invoker().onPre(event);
        if (event.isCanceled()) {
            return true;
        }

        // 调用饰品的攻击
        maidBauble.fireEvent((b, s) -> {
            b.onMeleeAttack(this, s, target);
            return false;
        });

        boolean result = super.doHurtTarget(serverLevel, target);
        if (result) {
            // 尝试使用横扫之刃
            this.doSweepHurt(target);
            // 调用 postHurtEnemy 来实现耐久消耗和部分其他功能。
            // 通用攻击流程已经处理命中效果；这里补上原版仅为玩家执行的武器耐久与破坏反馈。
            ItemStack mainHandItem = this.getMainHandItem();
            if (target instanceof LivingEntity livingEntity) {
                mainHandItem.postHurtEnemy(livingEntity, this);
            }
        }

        MaidHurtTarget.Post postEvent = new MaidHurtTarget.Post(this, target, result);
        MaidHurtTarget.POST.invoker().onPost(postEvent);

        // 部分 task 有额外伤害
        if (this.getTask() instanceof IAttackTask attackTask && attackTask.hasExtraAttack(this, target)) {
            boolean extraResult = attackTask.doExtraAttack(this, target);
            return result && extraResult;
        }
        return result;
    }

    private void doSweepHurt(Entity target) {
        ItemStack mainhandItem = this.getItemInHand(InteractionHand.MAIN_HAND);

        boolean canSweep = EnchantmentUtil.canEnchant(mainhandItem, Enchantments.SWEEPING_EDGE, this.registryAccess());
        float sweepingDamageRatio = (float) this.getAttributes().getValue(Attributes.SWEEPING_DAMAGE_RATIO);
        if (canSweep && sweepingDamageRatio > 0) {
            float baseDamage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float sweepDamage = 1.0f + sweepingDamageRatio * baseDamage;
            AABB sweepRange = this.getFavorabilityManager().getSweepRange(target, this.getFavorability());
            List<LivingEntity> hurtEntities = this.level.getEntitiesOfClass(LivingEntity.class, sweepRange);
            for (LivingEntity entity : hurtEntities) {
                if (entity != this && entity != target && !this.isAlliedTo(entity) && canAttack(entity) && canAttackType(entity.getType())) {
                    float posX = Mth.sin(this.getYRot() * ((float) Math.PI / 180F));
                    float posY = -Mth.cos(this.getYRot() * ((float) Math.PI / 180F));
                    entity.knockback(0.4, posX, posY);
                    entity.hurt(this.damageSources().mobAttack(this), sweepDamage);
                }
            }
            this.level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, this.getSoundSource(), 1, 1);
            this.spawnSweepAttackParticle();
        }
    }


    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        MaidAttackEvent event = new MaidAttackEvent(this, source, amount);
        MaidAttackEvent.CALLBACK.invoker().post(event);
        if (event.isCanceled()) {
            return false;
        }
        if (source.getEntity() instanceof Player player && this.isAlliedTo(player)) {
            // 主人和同 Team 玩家对自己女仆的伤害数值为 1/5，最大为 2
            amount = Mth.clamp(amount / 5, 0, 2);
            return super.hurtServer(level, source, amount);
        }
        // 使用盾牌
        if (source.is(DamageTypeTags.IS_PROJECTILE) && this.canUseShield()) {
            boolean isUsingShield = this.isUsingItem() && this.getUsedItemHand() == InteractionHand.OFF_HAND;
            if (!isUsingShield) {
                this.startUsingItem(InteractionHand.OFF_HAND);
                // 使用五秒的盾牌
                AttributeInstance attribute = this.getAttribute(InitAttribute.MAID_PASSIVE_USE_SHIELD_TICK);
                if (attribute != null) {
                    this.passiveUseShieldTick = (int) attribute.getValue();
                } else {
                    this.passiveUseShieldTick = 100;
                }
            }
        }
        return super.hurtServer(level, source, amount);
    }

    /**
     * 重新复写父类方法，添加上自己的 Event
     */
    @Override
    protected void actuallyHurt(ServerLevel serverLevel, DamageSource damageSrc, float damageAmount) {
        if (this.level instanceof ServerLevel sl && !this.isInvulnerableTo(sl, damageSrc)) {


            // 获取盔甲减伤后的数值
            float armorAbsorb = this.getDamageAfterArmorAbsorb(damageSrc, damageAmount);


            float magicAbsorb = this.getDamageAfterMagicAbsorb(damageSrc, armorAbsorb);

            // 获取事件减伤效果
            MaidHurtEvent maidHurtEvent = new MaidHurtEvent(this, damageSrc, magicAbsorb);
            MaidHurtEvent.CALLBACK.invoker().post(maidHurtEvent);
            damageAmount = maidHurtEvent.isCanceled() ? 0 : maidHurtEvent.getAmount();


            float f = damageAmount;
            damageAmount = Math.max(damageAmount - this.getAbsorptionAmount(), 0.0F);
            this.setAbsorptionAmount(this.getAbsorptionAmount() - (f - damageAmount));
            float damageDealtAbsorbed = f - damageAmount;
            if (0 < damageDealtAbsorbed && damageDealtAbsorbed < 3.5 && damageSrc.getEntity() instanceof ServerPlayer player) {
                player.awardStat(Stats.DAMAGE_DEALT_ABSORBED, Math.round(damageDealtAbsorbed * 10));
            }

            // 饰品
            MutableFloat newDamage = new MutableFloat(damageAmount);
            boolean baubleCancel = maidBauble.fireEvent((b, s) -> b.onInjured(this, s, damageSrc, newDamage));
            float damageAfterAbsorption = newDamage.getValue();
            // 如果饰品取消了事件，那么也不触发后续内容了
            if (baubleCancel || damageAfterAbsorption <= 0) {
                return;
            }

            // 再来一次事件
            MaidDamageEvent maidDamageEvent = new MaidDamageEvent(this, damageSrc, damageAfterAbsorption);
            MaidDamageEvent.CALLBACK.invoker().post(maidDamageEvent);
            damageAfterAbsorption = maidDamageEvent.isCanceled() ? 0 : maidDamageEvent.getAmount();

            // 最终运用实际伤害
            if (damageAfterAbsorption != 0) {
                this.getCombatTracker().recordDamage(damageSrc, damageAfterAbsorption);
                this.setHealth(this.getHealth() - damageAfterAbsorption);
                // fabric：按原版写（
                this.setAbsorptionAmount(this.getAbsorptionAmount() - damageAfterAbsorption);
                this.gameEvent(GameEvent.ENTITY_DAMAGE);

            }


            ServerLivingEntityEvents.AFTER_DAMAGE.invoker().afterDamage(this, damageSrc, damageAmount, damageAfterAbsorption, false);
        }
    }


    @Override
    @Nullable
    public Entity teleport(TeleportTransition transition) {
        if (transition.newLevel() != this.level && this.level instanceof ServerLevel && !this.isRemoved()) {
            final int MAX_RETRY = 16;
            for (int i = 0; i < MAX_RETRY; ++i) {
                if (TeleportHelper.teleport(this)) {
                    this.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 1, true, false));
                }
            }
            return null;
        }
        return super.teleport(transition);
    }

    @Override
    public final boolean isAddedToLevel() {
        return this.isAddedToLevel;
    }

    @Override
    public void onAddedToLevel() {
        this.isAddedToLevel = true;
        if (this.getOwner() != null) {
            MaidWorldData data = MaidWorldData.get(this.level);
            if (data != null) {
                data.removeInfo(this);
            }
        }

    }

    @Override
    public void onRemovedFromLevel() {
        this.isAddedToLevel = false;
        if (!this.level.isClientSide() && this.isAlive() && this.getOwner() != null) {
            MaidWorldData data = MaidWorldData.get(this.level);
            if (data != null) {
                data.addInfo(this);
            }
        }
    }

    @Override
    public void die(DamageSource cause) {
        MaidDeathEvent event = new MaidDeathEvent(this, cause);
        MaidDeathEvent.CALLBACK.invoker().post(event);
        boolean baubleCancel = this.maidBauble.fireEvent((b, s) -> b.onDeath(this, s, cause));
        if (!baubleCancel && !event.isCanceled()) {
            // 清除死亡时需要清除的内容
            this.clearFire();
            this.setTicksFrozen(0);
            this.setSharedFlagOnFire(false);
            this.removeAllEffects();
            // 最后父类方法
            super.die(cause);
            // 额外发送女仆所处坐标
            this.sendMaidPos();
        }
    }

    private void sendMaidPos() {
        if (this.isDeadOrDying() && this.level instanceof ServerLevel serverLevel
                && serverLevel.getGameRules().get(GameRules.SHOW_DEATH_MESSAGES)
                && this.getOwner() instanceof ServerPlayer serverPlayer) {
        //     // 支持旅行地图格式
        //     // [name:"name", x:-136, y:36, z:48, dim:minecraft:the_nether]
            BlockPos blockPos = this.blockPosition();
            String name = Identifier.parse(this.getModelId()).getPath();
            String msg = """
                    [name:"%s", x:%d, y:%d, z:%d, dim:%s]""".formatted(
                    name,
                    blockPos.getX(), blockPos.getY(), blockPos.getZ(),
                    this.level.dimension().identifier());
            serverPlayer.sendSystemMessage(Component.literal(msg));
        }
    }

    public boolean canPickup(Entity pickupEntity, boolean checkInWater) {
        if (isPickup()) {
            if (checkInWater && pickupEntity.isInWater()) {
                return false;
            }
            PickType pickupType = this.configManager.getPickupType();
            if (pickupType.canPickItem() && pickupEntity instanceof ItemEntity) {
                return pickupItem((ItemEntity) pickupEntity, true);
            }
            if (pickupType.canPickItem() && pickupEntity instanceof AbstractArrow) {
                return pickupArrow((AbstractArrow) pickupEntity, true);
            }
            if (pickupType.canPickXp() && pickupEntity instanceof ExperienceOrb) {
                return true;
            }
            return pickupType.canPickXp() && pickupEntity instanceof EntityPowerPoint;
        }
        return false;
    }

    public boolean canPickup(Entity pickupEntity) {
        return canPickup(pickupEntity, false);
    }

    @Override
    public void setChargingCrossbow(boolean isCharging) {
        this.entityData.set(DATA_IS_CHARGING_CROSSBOW, isCharging);
    }

    @Override
    public void onCrossbowAttackPerformed() {
        this.noActionTime = 0;
    }

    @Override
    @Nullable
    public LivingEntity getTarget() {
        // 实现 CrossbowAttackMob 接口中拿到目标实体的方法
        return this.brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
    }

    // 弩在装载时的 tryLoadProjectiles 方法会从这里拿到需要装填的物品
    @Override
    public ItemStack getProjectile(ItemStack weaponStack) {
        // 烟花只检查副手：优先检查副手有没有烟花
        if (this.getOffhandItem().getItem() instanceof FireworkRocketItem) {
            return this.getOffhandItem();
        }
        if (!(this.getMainHandItem().getItem() instanceof ProjectileWeaponItem weaponItem)) {
            return ItemStack.EMPTY;
        }
        CombinedInvWrapper handler = this.getAvailableInv(true);
        int slot = ItemsUtil.findStackSlot(handler, weaponItem.getAllSupportedProjectiles());
        if (slot < 0) {
            // 不存在时，返回空
            return ItemStack.EMPTY;
        } else {
            // 拿到弹药物品
            return handler.getStackInSlot(slot);
        }
    }

    @Override
    public void thunderHit(ServerLevel world, LightningBolt lightning) {
        super.thunderHit(world, lightning);
        if (!isStruckByLightning()) {
            double beforeMaxHealth = this.getAttributeBaseValue(Attributes.MAX_HEALTH);
            Objects.requireNonNull(this.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(beforeMaxHealth + 20);
            setStruckByLightning(true);
            if (this.getOwner() instanceof ServerPlayer serverPlayer) {
                InitTrigger.MAID_EVENT.trigger(serverPlayer, TriggerType.LIGHTNING_BOLT);
                if (this.getMaxHealth() >= 100) {
                    InitTrigger.MAID_EVENT.trigger(serverPlayer, TriggerType.MAID_100_HEALTHY);
                }
            }
        }
    }

    @Override
    public void hurtArmor(DamageSource damageSource, float damage) {
        this.doHurtEquipment(damageSource, damage, EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        IMaidTask maidTask = this.getTask();
        if (maidTask instanceof IRangedAttackTask rangedAttackTask) {
            // 调用饰品的攻击
            maidBauble.fireEvent((b, s) -> {
                b.onRangedAttack(this, s, rangedAttackTask);
                return false;
            });
            rangedAttackTask.performRangedAttack(this, target, distanceFactor);
        }
    }

    @Override
    public boolean canAttackType(EntityType<?> typeIn) {
        return typeIn != EntityType.ARMOR_STAND;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (this.getTask() instanceof IAttackTask attackTask) {
            return attackTask.canAttack(this, target);
        }
        return super.canAttack(target);
    }

    /**
     * 女仆在危险情况（比如附近有苦力怕）下是否应该停止骑乘或待命状态
     */
    public boolean shouldLeaveMountOrSitForDanger() {
        // 如果女仆和玩家骑乘同一个扫帚
        Entity vehicle = this.getVehicle();
        if (vehicle != null && vehicle.getControllingPassenger() instanceof Player) {
            return false;
        }
        return true;
    }

    /**
     * 用于物品的耐久损失
     */
    public void hurtAndBreak(ItemStack stack, int amount) {
        if (this.level instanceof ServerLevel serverLevel) {
            stack.hurtAndBreak(amount, serverLevel, null/* 这个 */, stackIn -> NetworkHandler.sendToNearby(this, new ItemBreakPackage(this.getId(), stackIn.getDefaultInstance())));
        }
    }

    private void randomRestoreHealth() {
        if (this.getHealth() < this.getMaxHealth() && random.nextFloat() < 0.0025) {
            this.heal(1);
            this.spawnRestoreHealthParticle(random.nextInt(3) + 7);
        }
    }

    private void spawnPortalParticle() {
        if (this.level.isClientSide() && this.getIsInvulnerable() && MiscConfig.INVULNERABLE_PARTICLE_EFFECT.get() && this.getOwner() != null) {
            this.level.addParticle(ParticleTypes.PORTAL,
                    this.getX() + (this.random.nextDouble() - 0.5D) * (double) this.getBbWidth(),
                    this.getY() + this.random.nextDouble() * (double) this.getBbHeight() - 0.25D,
                    this.getZ() + (this.random.nextDouble() - 0.5D) * (double) this.getBbWidth(),
                    (this.random.nextDouble() - 0.5D) * 2.0D, -this.random.nextDouble(),
                    (this.random.nextDouble() - 0.5D) * 2.0D);
        }
    }

    public void spawnRestoreHealthParticle(int particleCount) {
        if (this.level.isClientSide()) {
            for (int i = 0; i < particleCount; ++i) {
                double xRandom = this.random.nextGaussian() * 0.02D;
                double yRandom = this.random.nextGaussian() * 0.02D;
                double zRandom = this.random.nextGaussian() * 0.02D;

                this.level.addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.9f, 0.1f, 0.1f),
                        this.getX() + (double) (this.random.nextFloat() * this.getBbWidth() * 2.0F) - (double) this.getBbWidth() - xRandom * 10.0D,
                        this.getY() + (double) (this.random.nextFloat() * this.getBbHeight()) - yRandom * 10.0D,
                        this.getZ() + (double) (this.random.nextFloat() * this.getBbWidth() * 2.0F) - (double) this.getBbWidth() - zRandom * 10.0D,
                        0, 0, 0);
            }
        }
    }

    public void spawnExplosionParticle() {
        if (this.level.isClientSide()) {
            for (int i = 0; i < 20; ++i) {
                float mx = (random.nextFloat() - 0.5F) * 0.02F;
                float my = (random.nextFloat() - 0.5F) * 0.02F;
                float mz = (random.nextFloat() - 0.5F) * 0.02F;
                level.addParticle(ParticleTypes.CLOUD,
                        getX() + random.nextFloat() - 0.5F,
                        getY() + random.nextFloat() - 0.5F,
                        getZ() + random.nextFloat() - 0.5F,
                        mx, my, mz);
            }
        }
    }

    public void spawnBubbleParticle() {
        if (this.level.isClientSide()) {
            for (int i = 0; i < 8; ++i) {
                double offsetX = 2 * random.nextDouble() - 1;
                double offsetY = random.nextDouble() / 2;
                double offsetZ = 2 * random.nextDouble() - 1;
                level.addParticle(ParticleTypes.BUBBLE, getX() + offsetX, getY() + offsetY, getZ() + offsetZ,
                        0, 0.1, 0);
            }
        }
    }

    public void spawnHeartParticle() {
        if (this.level.isClientSide()) {
            for (int i = 0; i < 8; ++i) {
                double offsetX = this.random.nextGaussian() * 0.02;
                double offsetY = this.random.nextGaussian() * 0.02;
                double offsetZ = this.random.nextGaussian() * 0.02;
                level.addParticle(ParticleTypes.HEART, this.getRandomX(1.0), this.getRandomY() + 0.5, this.getRandomZ(1.0), offsetX, offsetY, offsetZ);
            }
        }
    }

    @Environment(EnvType.CLIENT)
    public void spawnRankUpParticle() {
        if (this.level.isClientSide()) {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.particleEngine.createTrackingEmitter(this, ParticleTypes.TOTEM_OF_UNDYING, 30);
            this.level.playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.BELL_BLOCK, this.getSoundSource(), 1.0F, 1.0F, false);
            minecraft.gui.setTitle(Component.translatable("message.touhou_little_maid.gomoku.rank_up.title"));
            minecraft.gui.setSubtitle(Component.translatable("message.touhou_little_maid.gomoku.rank_up.subtitle"));
        }
    }

    private void spawnSweepAttackParticle() {
        double xOffset = -Mth.sin(this.getYRot() * ((float) Math.PI / 180F));
        double zOffset = Mth.cos(this.getYRot() * ((float) Math.PI / 180F));
        if (this.level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    this.getX() + xOffset, this.getY(0.5),
                    this.getZ() + zOffset, 0, xOffset, 0, zOffset, 0);
        }
    }

    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);

        output.putString(MODEL_ID_TAG_NAME, getModelId());
        output.putBoolean(IS_YSM_MODEL_TAG, isYsmModel());
        output.putString(YSM_MODEL_ID_TAG, getYsmModelId());
        output.putString(YSM_MODEL_TEXTURE_TAG, getYsmModelTexture());
        output.putString(YSM_ROULETTE_ANIM_TAG, rouletteAnim);
        output.putInt(YSM_ROAMING_UPDATE_FLAG_TAG, roamingVarsUpdateFlag);

        CompoundTag roamingVarsTag = new CompoundTag();
        roamingVars.forEach(roamingVarsTag::putFloat);
        output.store(YSM_ROAMING_VARS_TAG, CustomData.COMPOUND_TAG_CODEC, roamingVarsTag);

        output.putString(YSM_MODEL_NAME_TAG, ComponentSerialization.CODEC
                .encodeStart(this.registryAccess().createSerializationContext(JsonOps.INSTANCE), getYsmModelName())
                .getOrThrow().toString());

        output.putString(SOUND_PACK_ID_TAG, getSoundPackId());
        output.putString(TASK_TAG, getTask().getUid().toString());


        output.store(MAID_INVENTORY_TAG, CustomData.COMPOUND_TAG_CODEC, maidInv.serializeNBT(this.registryAccess()));
        output.store(MAID_BAUBLE_INVENTORY_TAG, CustomData.COMPOUND_TAG_CODEC, maidBauble.serializeNBT(this.registryAccess()));
        output.store(MAID_HIDE_INVENTORY_TAG, CustomData.COMPOUND_TAG_CODEC, hideInv.serializeNBT(this.registryAccess()));
        output.store(MAID_TASK_INVENTORY_TAG, CustomData.COMPOUND_TAG_CODEC, taskInv.serializeNBT(this.registryAccess()));

        output.putBoolean(STRUCK_BY_LIGHTNING_TAG, isStruckByLightning());
        output.putBoolean(INVULNERABLE_TAG, getIsInvulnerable());
        output.putInt(HUNGER_TAG, getHunger());
        output.putInt(FAVORABILITY_TAG, getFavorability());
        output.putInt(EXPERIENCE_TAG, getExperience());
        output.putString(SCHEDULE_MODE_TAG, getSchedule().name());
        output.putString(MAID_BACKPACK_TYPE, getMaidBackpackType().getId().toString());
        output.putBoolean(STRUCTURE_SPAWN_TAG, this.structureSpawn);

        this.configManager.addAdditionalSaveData(output);
        this.gameRecordManager.addAdditionalSaveData(output);
        this.favorabilityManager.addAdditionalSaveData(output);
        this.schedulePos.save(output);

        if (this.backpackData != null) {
            CompoundTag bpTag = new CompoundTag();
            this.backpackData.save(bpTag, this);
            output.store(BACKPACK_DATA_TAG, CustomData.COMPOUND_TAG_CODEC, bpTag);
        } else {
            output.store(BACKPACK_DATA_TAG, CustomData.COMPOUND_TAG_CODEC, new CompoundTag());
        }

        this.taskDataMaps.writeSaveData(output);
        this.killRecordManager.addAdditionalSaveData(output);
        this.aiChatManager.addAdditionalSaveData(output);
    }

    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);

        setModelId(input.getStringOr(MODEL_ID_TAG_NAME, DEFAULT_MODEL_ID));
        setIsYsmModel(input.getBooleanOr(IS_YSM_MODEL_TAG, false));
        setYsmModelId(input.getStringOr(YSM_MODEL_ID_TAG, ""));
        setYsmModelTexture(input.getStringOr(YSM_MODEL_TEXTURE_TAG, ""));
        rouletteAnim = input.getStringOr(YSM_ROULETTE_ANIM_TAG, "empty");
        roamingVarsUpdateFlag = input.getIntOr(YSM_ROAMING_UPDATE_FLAG_TAG, 0);

        input.read(YSM_ROAMING_VARS_TAG, CustomData.COMPOUND_TAG_CODEC).ifPresent(roamingVarsTag ->
                roamingVarsTag.keySet().forEach(key -> roamingVars.put(key, roamingVarsTag.getFloatOr(key, 0f))));

        input.getString(YSM_MODEL_NAME_TAG).ifPresent(json -> {
            try {
                ComponentSerialization.CODEC.parse(this.registryAccess().createSerializationContext(JsonOps.INSTANCE),
                        JsonParser.parseString(json)).result().ifPresent(this::setYsmModelName);
            } catch (Exception ignored) {
            }
        });

        setSoundPackId(input.getStringOr(SOUND_PACK_ID_TAG, DefaultMaidSoundPack.getInitSoundPackId()));
        setSchedule(MaidSchedule.valueOf(input.getStringOr(SCHEDULE_MODE_TAG, "DAY")));

        Identifier uid = Identifier.parse(input.getStringOr(TASK_TAG, "touhou_little_maid:idle"));
        IMaidTask task = TaskManager.findTask(uid).orElse(TaskManager.getIdleTask());
        setTask(task);


        var registryAccess = this.registryAccess();
        input.read(MAID_INVENTORY_TAG, CustomData.COMPOUND_TAG_CODEC)
                .ifPresent(tag -> maidInv.deserializeNBT(registryAccess, tag));
        input.read(MAID_BAUBLE_INVENTORY_TAG, CustomData.COMPOUND_TAG_CODEC)
                .ifPresent(tag -> {

                    int oldSize = tag.getIntOr("Size", 0);
                    if (oldSize > 0 && oldSize < BAUBLE_INV_SIZE) {
                        tag.putInt("Size", BAUBLE_INV_SIZE);
                    }
                    maidBauble.deserializeNBT(registryAccess, tag);
                });
        input.read(MAID_HIDE_INVENTORY_TAG, CustomData.COMPOUND_TAG_CODEC)
                .ifPresent(tag -> hideInv.deserializeNBT(registryAccess, tag));
        input.read(MAID_TASK_INVENTORY_TAG, CustomData.COMPOUND_TAG_CODEC)
                .ifPresent(tag -> taskInv.deserializeNBT(registryAccess, tag));

        setStruckByLightning(input.getBooleanOr(STRUCK_BY_LIGHTNING_TAG, false));
        setEntityInvulnerable(input.getBooleanOr(INVULNERABLE_TAG, false));
        setHunger(input.getIntOr(HUNGER_TAG, 0));
        setFavorability(input.getIntOr(FAVORABILITY_TAG, 0));
        setExperience(input.getIntOr(EXPERIENCE_TAG, 0));
        this.structureSpawn = input.getBooleanOr(STRUCTURE_SPAWN_TAG, false);

        input.getInt(BACKPACK_LEVEL_TAG).ifPresent(backpackLevel -> {
            if (backpackLevel == 1) BackpackManager.findBackpack(SmallBackpack.ID).ifPresent(this::setMaidBackpackType);
            if (backpackLevel == 2) BackpackManager.findBackpack(MiddleBackpack.ID).ifPresent(this::setMaidBackpackType);
            if (backpackLevel == 3) BackpackManager.findBackpack(BigBackpack.ID).ifPresent(this::setMaidBackpackType);
        });

        input.read(RESTRICT_CENTER_TAG, BlockPos.CODEC).ifPresent(pos -> this.schedulePos.setHomeModeEnable(this, pos));
        String bpType = input.getStringOr(MAID_BACKPACK_TYPE, "");
        if (!bpType.isEmpty()) {
            Identifier id = Identifier.parse(bpType);
            IMaidBackpack backpack = BackpackManager.findBackpack(id).orElse(BackpackManager.getEmptyBackpack());
            setMaidBackpackType(backpack);
            if (this.backpackData != null) {
                input.read(BACKPACK_DATA_TAG, CustomData.COMPOUND_TAG_CODEC)
                        .ifPresent(tag -> this.backpackData.load(tag, this));
            }
        }
        this.configManager.readAdditionalSaveData(input);
        this.gameRecordManager.readAdditionalSaveData(input);
        this.favorabilityManager.readAdditionalSaveData(input);
        this.schedulePos.load(input, this);

        this.setBackpackShowItem(maidInv.getStackInSlot(MaidBackpackHandler.BACKPACK_ITEM_SLOT));
        this.taskDataMaps.readSaveData(input);

        this.killRecordManager.readAdditionalSaveData(input);
        this.aiChatManager.readAdditionalSaveData(input);
    }

    public boolean openMaidGui(Player player) {
        return openMaidGui(player, TabIndex.MAIN);
    }

    public boolean openMaidGui(Player player, int tabIndex) {
        if (player instanceof ServerPlayer serverPlayer && !this.isSleeping()) {
            this.navigation.stop();
            MenuProvider guiProvider = getGuiProvider(tabIndex);
            serverPlayer.openMenu(guiProvider);
            // 打开GUI时发包同步客户端流体amount
            if (getBackpackData() instanceof TankBackpackData tankBackpackData) {
                ServerPlayNetworking.send(serverPlayer, new SyncFluidAmountPackage(tankBackpackData.getTank().amount));
            }
        }
        return true;
    }

    private MenuProvider getGuiProvider(int tabIndex) {
        return switch (tabIndex) {
            case TabIndex.TASK_CONFIG -> task.getTaskConfigGuiProvider(this);
            case TabIndex.MAID_CONFIG -> MaidConfigContainer.create(getId());
            case TabIndex.BAUBLE -> BaubleContainer.create(this);
            default -> this.getMaidBackpackType().getGuiProvider(getId());
        };
    }

    @Override
    protected void dropEquipment(ServerLevel serverLevel) {
        if (this.getOwner() != null && !level.isClientSide()) {
            // 掉出世界的判断
            Vec3 position = Vec3.atBottomCenterOf(blockPosition());
            // 防止卡在基岩里？
            if (this.getY() < this.level.getMinY() + 5) {
                position = new Vec3(position.x, this.level.getMinY() + 5, position.z);
            }
            if (this.getY() > this.level.getMaxY()) {
                position = new Vec3(position.x, this.level.getMaxY(), position.z);
            }
            EntityTombstone tombstone = new EntityTombstone(level, this.getOwner().getUUID(), position);
            tombstone.setMaidName(this.getDisplayName());

            // 女仆物品栏
            CombinedInvWrapper invWrapper = new CombinedInvWrapper(armorInvWrapper, handsInvWrapper, maidInv, maidBauble, hideInv, taskInv);
            // 需要考虑消失诅咒附魔
            destroyVanishingCursedItems(invWrapper);
            for (int i = 0; i < invWrapper.getSlots(); i++) {
                int size = invWrapper.getSlotLimit(i);
                tombstone.insertItem(invWrapper.extractItem(i, size, false));
            }
            // 背包额外数据
            IMaidBackpack maidBackpack = this.getMaidBackpackType();
            tombstone.insertItem(maidBackpack.getTakeOffItemStack(ItemStack.EMPTY, null, this));
            maidBackpack.onSpawnTombstone(this, tombstone);
            // 胶片
            ItemStack filmItem = ItemFilm.maidToFilm(this);
            tombstone.insertItem(filmItem);

            // 事件触发，既可以阻断墓碑生成，也可以修改墓碑内容
            MaidTombstoneEvent tombstoneEvent = new MaidTombstoneEvent(this, tombstone);
            MaidTombstoneEvent.CALLBACK.invoker().onTombstone(tombstoneEvent);
            if (tombstoneEvent.isCanceled()) {
                // 如果事件被取消了，那么就不生成墓碑了
                return;
            }

            // 全局记录
            MaidWorldData maidWorldData = MaidWorldData.get(level);
            if (maidWorldData != null) {
                maidWorldData.addTombstones(this, tombstone);
            }

            // 记录墓碑已经生成，避免重复生成
            alreadyDropped = true;
            level.addFreshEntity(tombstone);
        }
    }

    private void destroyVanishingCursedItems(CombinedInvWrapper invWrapper) {

        if (this.level instanceof ServerLevel serverLevel && serverLevel.getGameRules().get(GameRules.KEEP_INVENTORY)) {
            return;
        }
        for (int i = 0; i < invWrapper.getSlots(); ++i) {
            ItemStack stack = invWrapper.getStackInSlot(i);
            if (!stack.isEmpty() && EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP) && !stack.is(TagItem.MAID_VANISHING_BLOCKLIST_ITEM)) {
                invWrapper.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {

        if (reason == RemovalReason.KILLED && !alreadyDropped) {
            // 女仆被指令杀后也正常生成墓碑
            if (this.level instanceof ServerLevel sl) this.dropEquipment(sl);
        }
        super.remove(reason);
    }

    @Override
    protected void completeUsingItem() {
        ItemStack consumed = this.getUseItem().copy();
        InteractionHand usedHand = this.getUsedItemHand();
        boolean wasConsumable = consumed.has(DataComponents.CONSUMABLE);
        this.getSwimManager().resetEatBreatheItem();
        super.completeUsingItem();
        if (wasConsumable) {
            ItemStack foodAfterEat = this.getItemInHand(usedHand);
            MaidAfterEatEvent.CALLBACK.invoker().post(new MaidAfterEatEvent(
                    this, foodAfterEat.isEmpty() ? consumed : foodAfterEat.copy()));
        }
        this.backCurrentHandItemStack();
    }

    /**
     * 当需要临时调换手中物品和背包内物品时，可调用此方法 当置换后的物品使用完后会自动将之前的手中物品再次返回到手上
     *
     * @param itemStack 当前手上的物品（必须是能使用--需要持续使用的物品）
     */
    public void memoryHandItemStack(ItemStack itemStack) {
        // 先检查内部存储是否已经有物品了，有就掉落
        ItemStack hideItemStack = this.getHideInv().getStackInSlot(0);
        if (!hideItemStack.isEmpty()) {
            ItemStack extractItem = this.getHideInv().extractItem(0, hideItemStack.getCount(), false);
            if (!extractItem.isEmpty()) {
                ItemEntity itemEntity = new ItemEntity(this.level(), this.getX(), this.getY() + 0.5, this.getZ(), extractItem);
                this.level.addFreshEntity(itemEntity);
            }
        }
        // 然后存入我们的物品
        ItemHandlerHelper.insertItemStacked(this.getHideInv(), itemStack, false);
    }

    /**
     * 将之前临时存在背包里的物品再次放在对应的手上
     */
    private void backCurrentHandItemStack() {
        // 先看看副手是否为空？
        ItemStack offhandItem = this.getItemInHand(InteractionHand.OFF_HAND);
        if (!offhandItem.isEmpty()) {
            ItemStack stack = ItemHandlerHelper.insertItemStacked(this.getAvailableBackpackInv(), offhandItem.copy(), false);
            if (!stack.isEmpty()) {
                ItemEntity itemEntity = new ItemEntity(this.level(), this.getX(), this.getY() + 0.5, this.getZ(), stack);
                this.level.addFreshEntity(itemEntity);
            }
        }
        // 副手此时为空，那么插入我们的物品
        ItemStack output = this.getHideInv().extractItem(0, this.getHideInv().getStackInSlot(0).getCount(), false);
        this.setItemInHand(InteractionHand.OFF_HAND, output);
    }


    public ItemStack tlmEat(Level level, ItemStack food, FoodProperties pFoodProperties) {
        ItemStack copy = food.copy();
        // 由物品完成使用流程应用 Consumable 效果，并按 USE_REMAINDER 组件生成剩余物。
        ItemStack foodAfterEat = food.getItem().finishUsingItem(food, level, this);
        MaidAfterEatEvent maidAfterEatEvent = new MaidAfterEatEvent(this, foodAfterEat.isEmpty() ? copy : foodAfterEat);
        MaidAfterEatEvent.CALLBACK.invoker().post(maidAfterEatEvent);
        return foodAfterEat;
    }

    @Override
    protected boolean isAlwaysExperienceDropper() {
        return true;
    }

    @Override
    protected int getBaseExperienceReward(ServerLevel level) {
        return this.getExperience();
    }

    @Override
    protected Component getTypeName() {
        // 优先事件系统
        MaidTypeNameEvent typeNameEvent = new MaidTypeNameEvent(this);
        MaidTypeNameEvent.CALLBACK.invoker().onMaidTypeName(typeNameEvent);
        if (typeNameEvent.getTypeName() != null) {
            return typeNameEvent.getTypeName();
        }

        java.util.Optional<com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.MaidModelInfo> info =
                com.github.tartaricacid.touhoulittlemaid.entity.info.ServerCustomPackLoader.SERVER_MAID_MODELS.getInfo(getModelId());
        return info.map(maidModelInfo -> (Component) ParseI18n.parse(maidModelInfo.getName()))
                .orElseGet(() -> Component.literal(getType().getDescriptionId()));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor worldIn, DifficultyInstance difficultyIn, EntitySpawnReason reason, @Nullable SpawnGroupData spawnDataIn) {
        // 为结构生成的女仆添加特殊标签
        if (reason == EntitySpawnReason.STRUCTURE) {
            this.structureSpawn = true;
        }
        int modelSize = ServerCustomPackLoader.SERVER_MAID_MODELS.getModelSize();
        // 这里居然可能为 0
        if (modelSize > 0) {
            int skipRandom = random.nextInt(modelSize);
            Optional<String> modelId = ServerCustomPackLoader.SERVER_MAID_MODELS.getModelIdSet().stream().skip(skipRandom).findFirst();
            return modelId.map(id -> {
                this.setModelId(id);
                return spawnDataIn;
            }).orElse(spawnDataIn);
        }
        return spawnDataIn;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        super.setItemSlot(slot, stack);
        if (!this.level.isClientSide()) {
            MaidEquipEvent maidEquipEvent = new MaidEquipEvent(this, slot, stack);
            MaidEquipEvent.CALLBACK.invoker().post(maidEquipEvent);
        }
    }

    @Override
    public void onEquipItem(EquipmentSlot slot, ItemStack oldItem, ItemStack newItem) {
        super.onEquipItem(slot, oldItem, newItem);
        if (newItem.isEmpty() || this.firstTick || !slot.isArmor()) {
            return;
        }

        // 触发成就
        if (this.getOwner() instanceof ServerPlayer serverPlayer) {
            InitTrigger.MAID_EVENT.trigger(serverPlayer, TriggerType.ANY_EQUIPMENT);
        }

        // 如果是下界合金
        if (isNetheriteArmor(newItem)) {
            // 检查全身装备
            for (EquipmentSlot slotIn : EquipmentSlot.values()) {
                if (!slotIn.isArmor() || slotIn == slot || slotIn == EquipmentSlot.BODY) {
                    continue;
                }
                ItemStack itemBySlot = getItemBySlot(slotIn);
                if (!isNetheriteArmor(itemBySlot)) {
                    return;
                }
            }
            // 触发事件
            if (this.getOwner() instanceof ServerPlayer serverPlayer) {
                InitTrigger.MAID_EVENT.trigger(serverPlayer, TriggerType.ALL_NETHERITE_EQUIPMENT);
            }
        }
    }

    private boolean isNetheriteArmor(ItemStack stack) {
        net.minecraft.world.item.equipment.Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null
                && equippable.assetId().filter(net.minecraft.world.item.equipment.EquipmentAssets.NETHERITE::equals)
                .isPresent();
    }

    @Override
    public void playSound(SoundEvent soundEvent, float volume, float pitch) {
        if (soundEvent.location().getPath().startsWith("maid") && !level.isClientSide()) {
            NetworkHandler.sendToNearby(this,
                    new PlayMaidSoundPackage(soundEvent.location(), this.getSoundPackId(), this.getId()), 16);
        } else {
            super.playSound(soundEvent, volume, pitch);
        }
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        MaidPlaySoundEvent event = new MaidPlaySoundEvent(this);
        MaidPlaySoundEvent.CALLBACK.invoker().post(event);
        if (event.isCanceled()) {
            return null;
        }
        return task.getAmbientSound(this);
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        MaidPlaySoundEvent event = new MaidPlaySoundEvent(this);
        MaidPlaySoundEvent.CALLBACK.invoker().post(event);
        if (event.isCanceled()) {
            return null;
        }
        if (damageSourceIn.is(DamageTypeTags.IS_FIRE)) {
            return InitSounds.MAID_HURT_FIRE;
        } else if (damageSourceIn.getEntity() instanceof Player) {
            if (playerHurtSoundCount == 0) {
                playerHurtSoundCount = 120;
                return InitSounds.MAID_PLAYER;
            } else {
                return null;
            }
        } else {
            return InitSounds.MAID_HURT;
        }
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        MaidPlaySoundEvent event = new MaidPlaySoundEvent(this);
        MaidPlaySoundEvent.CALLBACK.invoker().post(event);
        if (event.isCanceled()) {
            return null;
        }
        return InitSounds.MAID_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 1 + random.nextFloat() * 0.1F;
    }

    @Override
    public float getEyeHeight(Pose pPose) {
        return this.getDimensions(pPose).height() * (isMaidInSittingPose() ? 0.65F : 0.85F);
    }

    @Override
    public boolean isBaby() {
        return false;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverWorld, AgeableMob ageableEntity) {
        return null;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    public boolean canPathReach(BlockPos pos) {
        Path path = this.getNavigation().createPath(pos, 0);
        return path != null && path.canReach();
    }

    public boolean canPathReach(Entity entity) {
        Path path = this.getNavigation().createPath(entity, 0);
        return path != null && path.canReach();
    }

    /**
     * 给 MaidMeleeAttack 使用，用于判断当前任务是否能够近战 <p> 如果返回 true，则表示当前是远程攻击，不是近战攻击
     */
    @Override
    public boolean canUseNonMeleeWeapon(ItemStack itemStack) {
        return getTask() instanceof IRangedAttackTask;
    }

    /**
     * 因为原版默认的攻击识别范围是固定死的 16 格，但是一些远程武器我们希望获得超视距打击 通过修改此处来获得更远的攻击距离
     */
    public boolean canSee(LivingEntity target) {
        if (this.getTask() instanceof IRangedAttackTask rangedTask) {
            return rangedTask.canSee(this, target);
        }
        return BehaviorUtils.canSee(this, target);
    }

    /**
     * 实体搜索范围
     */
    public AABB searchDimension() {
        // 仅工作时，才搜索 task 的范围，避免性能压力
        if (this.getScheduleDetail() == Activity.WORK) {
            return this.getTask().searchDimension(this);
        }
        return TaskManager.getIdleTask().searchDimension(this);
    }

    /**
     * 实体搜索范围的水平范围值
     */
    public float searchRadius() {
        return this.getTask().searchRadius(this);
    }

    // mixin 的自定义方法，保留用于客户端边界框访问
    public AABB tlm$getRenderBoundingBox() {
        return super.getBoundingBox();

    }

    @Override
    @Environment(EnvType.CLIENT)
    public Vec3 getLeashOffset() {
        String modelId = this.getModelId();
        Vec3 pose = getLegacyLeashOffset(modelId);
        if (pose != null) {
            return pose;
        }
        return super.getLeashOffset();
    }

    @Nullable
    @Environment(EnvType.CLIENT)
    private Vec3 getLegacyLeashOffset(String modelId) {
        Optional<EntityMaidModel> modelOptional = CustomPackLoader.MAID_MODELS.getModel(modelId);
        Optional<MaidModelInfo> infoOptional = CustomPackLoader.MAID_MODELS.getInfo(modelId);
        if (modelOptional.isPresent() && infoOptional.isPresent()) {
            EntityMaidModel model = modelOptional.get();
            float renderEntityScale = infoOptional.get().getRenderEntityScale();

            BedrockPart arm = null;
            HumanoidArm armSide = HumanoidArm.RIGHT;
            if (model.hasRightArm()) {
                arm = model.getRightArm();
            } else if (model.hasLeftArm()) {
                arm = model.getLeftArm();
                armSide = HumanoidArm.LEFT;
            }

            if (arm != null) {
                BedrockPart positioningModel = model.getArmPositioningModel(armSide);
                Vector3f positionVec;
                if (positioningModel != null) {
                    positionVec = positioningModel.getTranslateAndRotateVector3f();
                } else {
                    positionVec = new Vector3f(0, 0.5f, 0);
                }
                Vector3f armVec = arm.getTranslateAndRotateVector3f();
                Vector3f pose = armVec.add(positionVec);
                return new Vec3(pose.x() * renderEntityScale, (1.5 - pose.y) * renderEntityScale, pose.z() * renderEntityScale);
            }

            if (model.hasHead()) {
                BedrockPart head = model.getHead();
                return new Vec3(head.x * renderEntityScale, (1.5 - head.y / 16) * renderEntityScale, head.z * renderEntityScale);
            }
        }
        return null;
    }

    @Override
    public void swing(InteractionHand pHand) {
        super.swing(pHand);
    }

    @Override
    protected void updateUsingItem(ItemStack usingItem) {
        // 处理问题 https://github.com/TartaricAcid/TouhouLittleMaid/issues/1003
        // 检测女仆是否处于异常的进食状态：正在使用物品但手中物品不是可正常使用状态下的物品
        if (this.isUsingItem()) {
            ItemStack currentItem = this.getUseItem();
            // 如果正在使用物品但该物品无法继续使用（例如食物已被移除），则强制停止使用
            if (currentItem.isEmpty() || currentItem.getUseDuration(this) <= 0) {
                this.stopUsingItem();
                return;
            }
        }

        if (!usingItem.isEmpty()) {
            AttributeInstance attribute = this.getAttribute(InitAttribute.MAID_USE_ITEM_SPEED);
            if (attribute != null) {
                // MAID_USE_ITEM_SPEED 默认是 1
                // 故这里减去属性值再加 1，保证属性值为 1 时行为和原版一致
                this.useItemRemaining = this.useItemRemaining - (int) attribute.getValue() + 1;
            }
        }
        super.updateUsingItem(usingItem);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        // 修正睡觉时渲染问题，默认 64 格内渲染
        double range = 64.0 * getViewScale();
        return distance < range * range;
    }

    @Override
    public void startSleeping(BlockPos pPos) {
        super.startSleeping(pPos);
        this.setHealth(this.getMaxHealth());
        this.favorabilityManager.apply(Type.SLEEP);
        if (this.getOwner() instanceof ServerPlayer serverPlayer) {
            InitTrigger.MAID_EVENT.trigger(serverPlayer, TriggerType.MAID_SLEEP);
        }
    }

    public void setBackpackDelay() {
        backpackDelay = 20;
    }

    public boolean backpackHasDelay() {
        return backpackDelay > 0;
    }

    @Override
    public String getModelId() {
        return this.entityData.get(DATA_MODEL_ID);
    }

    public void setModelId(String modelId) {
        this.entityData.set(DATA_MODEL_ID, modelId);
    }

    @Override
    public boolean isYsmModel() {
        return this.entityData.get(DATA_IS_YSM_MODEL);
    }

    @Override
    public void setIsYsmModel(boolean isYsmModel) {
        this.entityData.set(DATA_IS_YSM_MODEL, isYsmModel);
    }

    @Override
    public String getYsmModelId() {
        return this.entityData.get(DATA_YSM_MODEL_ID);
    }

    protected void setYsmModelId(String modelId) {
        this.entityData.set(DATA_YSM_MODEL_ID, modelId);
    }

    @Override
    public String getYsmModelTexture() {
        return this.entityData.get(DATA_YSM_MODEL_TEXTURE);
    }

    protected void setYsmModelTexture(String texture) {
        this.entityData.set(DATA_YSM_MODEL_TEXTURE, texture);
    }

    @Override
    public Component getYsmModelName() {
        return this.entityData.get(DATA_YSM_MODEL_NAME);
    }

    protected void setYsmModelName(Component name) {
        this.entityData.set(DATA_YSM_MODEL_NAME, name);
    }

    @Override
    public void setYsmModel(String modelId, String texture, Component name) {
        if (!modelId.equals(this.getYsmModelId())) {
            this.roamingVars = new Object2FloatOpenHashMap<>();
            this.stopRouletteAnim();
        }
        this.setYsmModelId(modelId);
        this.setYsmModelTexture(texture);
        this.setYsmModelName(name);
    }

    @Override
    public void playRouletteAnim(String rouletteAnim) {
        this.rouletteAnimPlaying = true;
        this.rouletteAnim = rouletteAnim;
        this.rouletteAnimDirty = true;
    }

    @Override
    public void stopRouletteAnim() {
        this.rouletteAnimPlaying = false;
        this.rouletteAnimDirty = true;
    }

    public String getSoundPackId() {
        return this.entityData.get(DATA_SOUND_PACK_ID);
    }

    public void setSoundPackId(String soundPackId) {
        this.entityData.set(DATA_SOUND_PACK_ID, soundPackId);
    }

    @Override
    public boolean isMaidInSittingPose() {
        return super.isInSittingPose();
    }

    @Override
    public boolean isBegging() {
        return this.entityData.get(DATA_BEGGING);
    }

    public void setBegging(boolean begging) {
        this.entityData.set(DATA_BEGGING, begging);
    }

    public boolean isHomeModeEnable() {
        return this.configManager.isHomeModeEnable();
    }

    public void setHomeModeEnable(boolean enable) {
        this.configManager.setHomeModeEnable(enable);
    }

    public MaidConfigManager getConfigManager() {
        return configManager;
    }

    @Override
    public boolean isWithinHome() {
        return this.isWithinHome(this.blockPosition());
    }

    @Override
    public boolean isWithinHome(BlockPos pos) {
        if (hasHome()) {
            return this.getHomePosition().distSqr(pos) < (double) (this.getHomeRadius() * this.getHomeRadius());
        }
        return true;
    }

    @Override
    public void setHomeTo(BlockPos pos, int distance) {
        this.entityData.set(RESTRICT_CENTER, pos);
        this.entityData.set(RESTRICT_RADIUS, (float) distance);
    }

    @Override
    public BlockPos getHomePosition() {
        return this.entityData.get(RESTRICT_CENTER);
    }

    @Override
    public int getHomeRadius() {
        return Math.round(this.entityData.get(RESTRICT_RADIUS));
    }

    @Override
    public void clearHome() {
        this.schedulePos.clear(this);
    }

    /**
     * 女仆的 Home 状态保存在同步实体数据中，不使用原版生物的 homeRadius 字段，因此必须覆盖原版判断。
     */
    @Override
    public boolean hasHome() {
        return this.isHomeModeEnable();
    }

    public BlockPos getBrainSearchPos() {
        if (this.hasHome()) {
            return this.getHomePosition();
        } else {
            return this.blockPosition();
        }
    }

    public boolean canBrainMoving() {
        return !this.isMaidInSittingPose() && !this.isPassenger() && !this.isSleeping() && !this.isLeashed();
    }

    public boolean isPickup() {
        return this.configManager.isPickup();
    }

    public void setPickup(boolean isPickup) {
        this.configManager.setPickup(isPickup);
    }

    public boolean isRideable() {
        return this.configManager.isRideable();
    }

    public void setRideable(boolean rideable) {
        this.configManager.setRideable(rideable);
    }

    public int getHunger() {
        return this.entityData.get(DATA_HUNGER);
    }

    public void setHunger(int hunger) {
        this.entityData.set(DATA_HUNGER, hunger);
    }

    @Override
    public int getFavorability() {
        return this.entityData.get(DATA_FAVORABILITY);
    }

    public void setFavorability(int favorability) {
        this.entityData.set(DATA_FAVORABILITY, favorability);
    }

    @Override
    public int getExperience() {
        return this.entityData.get(DATA_EXPERIENCE);
    }

    public void setExperience(int experience) {
        this.entityData.set(DATA_EXPERIENCE, experience);
    }

    public boolean isStruckByLightning() {
        return this.entityData.get(DATA_STRUCK_BY_LIGHTNING);
    }

    public void setStruckByLightning(boolean isStruck) {
        this.entityData.set(DATA_STRUCK_BY_LIGHTNING, isStruck);
    }

    @Override
    public boolean isSwingingArms() {
        return this.entityData.get(DATA_ARM_RISE);
    }

    public void setSwingingArms(boolean swingingArms) {
        this.entityData.set(DATA_ARM_RISE, swingingArms);
    }

    public String getBackpackFluid() {
        return this.entityData.get(BACKPACK_FLUID);
    }

    public void setBackpackFluid(String fluidName) {
        this.entityData.set(BACKPACK_FLUID, fluidName);
    }

    public MaidSchedule getSchedule() {
        return this.entityData.get(SCHEDULE_MODE);
    }

    public void setSchedule(MaidSchedule schedule) {
        this.entityData.set(SCHEDULE_MODE, schedule);
        if (this.level instanceof ServerLevel) {
            this.refreshBrain((ServerLevel) this.level);
        }
    }

    public Activity getScheduleDetail() {
        MaidSchedule schedule = this.getSchedule();
        int time = (int) (this.level.getDayTime() % 24000L);
        switch (schedule) {
            case ALL -> {
                return Activity.WORK;
            }
            case NIGHT -> {

                if (time < 8000) {
                    return Activity.REST;
                } else if (time < 12000) {
                    return Activity.IDLE;
                } else {
                    return Activity.WORK;
                }
            }
            default -> {

                if (time < 12000) {
                    return Activity.WORK;
                } else if (time < 16000) {
                    return Activity.IDLE;
                } else {
                    return Activity.REST;
                }
            }
        }
    }

    public SchedulePos getSchedulePos() {
        return schedulePos;
    }

    @Override
    public ItemStack getBackpackShowItem() {
        return this.entityData.get(BACKPACK_ITEM_SHOW);
    }

    public void setBackpackShowItem(ItemStack stack) {
        this.entityData.set(BACKPACK_ITEM_SHOW, stack);
    }

    @Override
    public IMaidBackpack getMaidBackpackType() {
        Identifier id = Identifier.parse(entityData.get(BACKPACK_TYPE));
        return BackpackManager.findBackpack(id).orElse(BackpackManager.getEmptyBackpack());
    }

    public void setMaidBackpackType(IMaidBackpack backpack) {
        if (backpack == this.backpack) {
            return;
        }
        this.backpack = backpack;
        if (this.backpack.hasBackpackData()) {
            this.backpackData = this.backpack.getBackpackData(this);
        } else {
            this.backpackData = null;
        }
        this.entityData.set(BACKPACK_TYPE, backpack.getId().toString());
    }

    public IBackpackData getBackpackData() {
        return backpackData;
    }

    public ItemStackHandler getMaidInv() {
        return maidInv;
    }

    /**
     * 返回 MaidInvWrapper，方便触发 MaidRequestItemEvent 事件时使用
     */
    public CombinedInvWrapper getAvailableInv(boolean handsFirst) {
        int maxContainerIndex = getMaidBackpackType().getAvailableMaxContainerIndex();
        RangedWrapper combinedInvWrapper = new RangedWrapper(maidInv, 0, maxContainerIndex);
        return handsFirst ? new MaidInvWrapper(this, handsInvWrapper, combinedInvWrapper)
                : new MaidInvWrapper(this, combinedInvWrapper, handsInvWrapper);
    }

    /**
     * 返回 MaidInvWrapper，方便触发 MaidRequestItemEvent 事件时使用
     */
    public CombinedInvWrapper getAvailableBackpackInv() {
        int maxContainerIndex = getMaidBackpackType().getAvailableMaxContainerIndex();
        RangedWrapper rangedWrapper = new RangedWrapper(maidInv, 0, maxContainerIndex);
        return new MaidInvWrapper(this, rangedWrapper);
    }

    public EntityHandsInvWrapper getHandsInvWrapper() {
        return handsInvWrapper;
    }

    public EntityArmorInvWrapper getArmorInvWrapper() {
        return armorInvWrapper;
    }

    public BaubleItemHandler getMaidBauble() {
        return maidBauble;
    }

    public CombinedInvWrapper getAllInv() {
        return new CombinedInvWrapper(getHandsInvWrapper(), getArmorInvWrapper(), getMaidInv(), getMaidBauble());
    }

    /**
     * 获取隐藏物品栏
     */
    public ItemStackHandler getHideInv() {
        return hideInv;
    }

    /**
     * 获取任务物品栏
     */
    public ItemStackHandler getTaskInv() {
        return taskInv;
    }

    public boolean getIsInvulnerable() {
        return this.entityData.get(DATA_INVULNERABLE);
    }

    public void setEntityInvulnerable(boolean isInvulnerable) {
        super.setInvulnerable(isInvulnerable);
        this.entityData.set(DATA_INVULNERABLE, isInvulnerable);
    }

    @Override
    public IMaidTask getTask() {
        Identifier uid = Identifier.parse(entityData.get(DATA_TASK));
        return TaskManager.findTask(uid).orElse(TaskManager.getIdleTask());
    }

    public void setTask(IMaidTask task) {
        if (task == this.task) {
            return;
        }
        this.task = task;
        this.entityData.set(DATA_TASK, task.getUid().toString());
        if (level instanceof ServerLevel) {
            refreshBrain((ServerLevel) level);
        }
    }

    @Override
    public void setInSittingPose(boolean inSittingPose) {
        super.setInSittingPose(inSittingPose);
        setOrderedToSit(inSittingPose);
    }

    public MaidGameRecordManager getGameRecordManager() {
        return gameRecordManager;
    }


    public float getLuck() {
        return (float) this.getAttributeValue(Attributes.LUCK);
    }

    public MaidKillRecordManager getKillRecordManager() {
        return killRecordManager;
    }

    @Override
    public boolean hasFishingHook() {
        return this.fishing != null;
    }

    public boolean isStructureSpawn() {
        return structureSpawn;
    }

    public List<SendEffectPackage.EffectData> getEffects() {
        return effects;
    }

    public void setEffects(List<SendEffectPackage.EffectData> effects) {
        this.effects = effects;
    }

    public boolean canDestroyBlock(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return BlockUtil.canEntityDestroy(state.getBlock(), state, level, pos, this);
    }

    public boolean canPlaceBlock(BlockPos pos) {
        BlockState oldState = level.getBlockState(pos);
        return oldState.canBeReplaced();
    }

    public boolean destroyBlock(BlockPos pos) {
        return destroyBlock(pos, true);
    }

    public boolean destroyBlock(BlockPos pos, boolean dropBlock) {
        return canDestroyBlock(pos) && destroyBlock(level, pos, dropBlock, this);
    }

    public boolean destroyBlock(Level level, BlockPos blockPos, boolean dropBlock, @Nullable Entity entity) {
        BlockState blockState = level.getBlockState(blockPos);
        if (blockState.isAir()) {
            return false;
        } else {
            FluidState fluidState = level.getFluidState(blockPos);
            if (!(blockState.getBlock() instanceof BaseFireBlock)) {
                level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, blockPos, Block.getId(blockState));
            }
            if (dropBlock) {
                BlockEntity blockEntity = blockState.hasBlockEntity() ? level.getBlockEntity(blockPos) : null;
                dropResourcesToMaidInv(blockState, level, blockPos, blockEntity, this, ItemStack.EMPTY);
            }
            boolean setResult = level.setBlock(blockPos, fluidState.createLegacyBlock(), Block.UPDATE_ALL);
            if (setResult) {
                level.gameEvent(GameEvent.BLOCK_DESTROY, blockPos, GameEvent.Context.of(entity, blockState));
            }
            return setResult;
        }
    }

    public void dropResourcesToMaidInv(BlockState state, Level level, BlockPos pos, @Nullable BlockEntity blockEntity, EntityMaid maid, ItemStack tool) {
        if (level instanceof ServerLevel serverLevel) {
            CombinedInvWrapper availableInv = this.getAvailableInv(false);
            Block.getDrops(state, serverLevel, pos, blockEntity, maid, tool).forEach(stack -> {
                ItemStack remindItemStack = ItemHandlerHelper.insertItemStacked(availableInv, stack, false);
                if (!remindItemStack.isEmpty()) {
                    Block.popResource(level, pos, remindItemStack);
                }
            });
            state.spawnAfterBreak(serverLevel, pos, tool, true);
        }
    }

    public boolean placeItemBlock(InteractionHand hand, BlockPos placePos, Direction direction, ItemStack stack) {
        if (stack.getItem() instanceof BlockItem blockItem) {
            return blockItem.place(new BlockPlaceContext(level, null, hand, stack,
                    getBlockRayTraceResult(placePos, direction))).consumesAction();
        }
        return false;
    }

    public boolean placeItemBlock(BlockPos placePos, Direction direction, ItemStack stack) {
        return placeItemBlock(InteractionHand.MAIN_HAND, placePos, direction, stack);
    }

    public boolean placeItemBlock(BlockPos placePos, ItemStack stack) {
        return placeItemBlock(placePos, Direction.UP, stack);
    }

    private BlockHitResult getBlockRayTraceResult(BlockPos pos, Direction direction) {
        return new BlockHitResult(
                new Vec3((double) pos.getX() + 0.5D + (double) direction.getStepX() * 0.5D,
                        (double) pos.getY() + 0.5D + (double) direction.getStepY() * 0.5D,
                        (double) pos.getZ() + 0.5D + (double) direction.getStepZ() * 0.5D),
                direction, pos, false);
    }

    public FavorabilityManager getFavorabilityManager() {
        return favorabilityManager;
    }

    @SuppressWarnings("all")
    public Ingredient getTamedItem() {
        Ingredient configIngredient = getConfigIngredient(MaidConfig.MAID_TAMED_ITEM.get(), Items.CAKE);
        Ingredient tagIngredient = BuiltInRegistries.ITEM.get(TagItem.MAID_TAMED_ITEM)
                .map(Ingredient::of)
                .orElseGet(() -> Ingredient.of(Items.CAKE));
        return mergeIngredients(configIngredient, tagIngredient);
    }

    private static Ingredient mergeIngredients(Ingredient... ingredients) {
        return Ingredient.of(HolderSet.direct(
                java.util.Arrays.stream(ingredients)
                        .flatMap(Ingredient::items)
                        .distinct()
                        .toList()));
    }


    @SuppressWarnings("all")
    public Ingredient getTemptationItem() {
        return getConfigIngredient(MaidConfig.MAID_TEMPTATION_ITEM.get(), Items.CAKE);
    }

    @SuppressWarnings("all")
    public static Ingredient getNtrItem() {
        return Ingredient.of(InitItems.OWNER_CONVERSION_TOOL);
    }

    private static Ingredient getConfigIngredient(String config, Item defaultItem) {
        if (config.startsWith(MaidConfig.TAG_PREFIX)) {
            Identifier key = Identifier.parse(config.substring(1));
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, key);
            return BuiltInRegistries.ITEM.get(tagKey)
                    .map(Ingredient::of)
                    .orElseGet(() -> Ingredient.of(defaultItem));
        } else {
            Identifier key = Identifier.parse(config);
            java.util.Optional<Item> itemOpt = BuiltInRegistries.ITEM.getOptional(key);
            if (itemOpt.isPresent()) {
                return Ingredient.of(itemOpt.get());
            }
        }
        return Ingredient.of(defaultItem);
    }

    @Override
    public EntityMaid asStrictMaid() {
        return this;
    }

    @Override
    public Mob asEntity() {
        return this;
    }

    @Override
    public ItemStack[] getHandItemsForAnimation() {
        return handItemsForAnimation;
    }

    @Override
    public Vec3 handleOnClimbable(Vec3 deltaMovement) {
        Vec3 oriDelta = super.handleOnClimbable(deltaMovement);
        // 主动爬行过程中严禁水平方向偏移，防止摔伤，y轴保持原样
        if (this.isCanClimb()) {
            Vec3 vec3 = this.position();
            if (vec3.x() % 1 != 0.5D || vec3.z() % 1 != 0.5) {
                BlockPos currentPosition = this.blockPosition().mutable();
                Vec3 centerPos = Vec3.atBottomCenterOf(currentPosition);
                this.setPos(centerPos.x, vec3.y(), centerPos.z);
            }
            oriDelta = new Vec3(0, oriDelta.y, 0);
        }
        return oriDelta;
    }

    /**
     * 爬梯子状态加上路径判断
     */
    @Override
    public boolean onClimbable() {
        boolean result = false;
        Path path = this.navigation.getPath();
        if (path != null && !path.isDone()) {
            // 女仆是要爬梯子而不是路过梯子，那么也就意味着当前节点的前后必有一个节点是同坐标的
            for (int i = Math.max(0, path.getNextNodeIndex() - 3); i < Math.min(path.getNodeCount(), path.getNextNodeIndex() + 3) - 1; i++) {
                BlockPos pos1 = path.getNodePos(i);
                BlockPos pos2 = path.getNodePos(i + 1);
                if (pos1.getX() == pos2.getX() && pos1.getZ() == pos2.getZ()) {
                    result = true;
                    break;
                }
            }
        }
        if (result) {
            result = super.onClimbable();
            // 用作脚手架和卡在梯子顶部的特判，避免女仆卡在脚手架顶上
            if (!result && !this.isSpectator()) {
                Optional<BlockPos> ladderPos = CommonHooks.isLivingOnLadder(
                        level.getBlockState(blockPosition().below()),
                        level(), blockPosition().below(), this);
                if (ladderPos.isPresent()) {
                    result = true;
                }
            }
        }
        if (result) {
            // 爬梯后一段时间禁用摔落伤害
            this.climbFallDelayTicks = 30;
            // 爬梯时，禁止旋转
            this.getLastClimbablePos().ifPresent(climbablePos -> {
                BlockState blockState = this.level.getBlockState(climbablePos);
                blockState.getOptionalValue(HorizontalDirectionalBlock.FACING).ifPresent(direction -> {
                    int yRot = direction.getOpposite().get2DDataValue() * 90;
                    this.setYRot(yRot);
                    this.setYHeadRot(yRot);
                });
            });
        }
        return result;
    }

    /**
     * 略微修改原版的方法，禁用了向上的动力源
     */
    @Override
    public Vec3 handleRelativeFrictionAndCalculateMovement(Vec3 deltaMovement, float friction) {
        this.moveRelative(this.getFrictionInfluencedSpeed(friction), deltaMovement);
        this.setDeltaMovement(this.handleOnClimbable(this.getDeltaMovement()));
        this.move(MoverType.SELF, this.getDeltaMovement());
        return this.getDeltaMovement();
    }

    public boolean isCanClimb() {
        return canClimb;
    }

    public void setCanClimb(boolean canClimb) {
        this.canClimb = canClimb;
    }

    public void setNavigation(PathNavigation navigation) {
        this.navigation = navigation;
    }

    public MaidSwimManager getSwimManager() {
        return swimManager;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isPushedByFluid() {
        return !this.getSwimManager().wantToSwim();
    }


    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi() && isInWater()) {
            if (this.getSwimManager().wantToSwim()) {
                this.moveRelative(0.01F, travelVector);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
            } else if (this.getSwimManager().isReadyToLand() || isUnderWater()) {
                super.travel(travelVector.scale(1.2).add(0, 0.5, 0));
            } else {
                super.travel(travelVector.scale(1.2).add(0, 0.05, 0));
            }
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return pose == Pose.SWIMMING ? this.getSwimManager().getSwimmingDimensions() : super.getDefaultDimensions(pose);
    }

    @Override
    public void updateSwimming() {
        this.getSwimManager().updateSwimming();
    }

    @Override
    public boolean isVisuallySwimming() {
        return this.isSwimming();
    }

    public boolean canUseShield() {
        ItemStack offhandItem = this.getOffhandItem();
        return offhandItem.has(DataComponents.BLOCKS_ATTACKS)
                && !this.getCooldowns().isOnCooldown(offhandItem);
    }

    @Override
    @Nullable
    public ItemStack getItemBlockingWith() {
        ItemStack blockingItem = super.getItemBlockingWith();
        if (blockingItem != null) {
            return blockingItem;
        }

        // 女仆举盾后立即进入格挡判定，不等待玩家盾牌的普通启用延迟。
        if (this.isUsingItem()) {
            ItemStack usedItem = this.getUseItem();
            if (usedItem.has(DataComponents.BLOCKS_ATTACKS)) {
                return usedItem;
            }
        }
        return null;
    }

    @Override
    public boolean isBlocking() {
        return this.getItemBlockingWith() != null;
    }

    @Override
    protected void blockUsingItem(ServerLevel level, LivingEntity attacker) {
        super.blockUsingItem(level, attacker);
        ItemStack blockingItem = this.getItemBlockingWith();
        BlocksAttacks blocksAttacks = blockingItem == null ? null : blockingItem.get(DataComponents.BLOCKS_ATTACKS);
        float disableSeconds = attacker.getSecondsToDisableBlocking();
        if (disableSeconds > 0.0F && blocksAttacks != null) {
            int cooldownTicks = Math.round(disableSeconds * blocksAttacks.disableCooldownScale() * 20.0F);
            if (cooldownTicks > 0) {
                // BlocksAttacks.disable 只会为玩家设置冷却；女仆持有等效且按游戏刻更新的 ItemCooldowns，
                // 因此在此补上非玩家实体的冷却，停止格挡与反馈音效仍由数据组件处理。
                this.getCooldowns().addCooldown(blockingItem, cooldownTicks);
            }
            blocksAttacks.disable(level, this, disableSeconds, blockingItem);
        }
    }

    public ItemCooldowns getCooldowns() {
        return cooldowns;
    }

    public MaidAIChatManager getAiChatManager() {
        return aiChatManager;
    }

    public MaidNavigationManager getNavigationManager() {
        return navigationManager;
    }

    /**
     * 参考自 <a href="https://github.com/Snownee/Companion/blob/1.20-forge/src/main/java/snownee/companion/Hooks.java#L313-L322">Snownee's Companion</a> <p> 更加高效的 owner 寻找方式
     */
    @Override
    @Nullable
    public LivingEntity getOwner() {

        EntityReference<LivingEntity> ownerRef = this.getOwnerReference();
        UUID uuid = ownerRef != null ? ownerRef.getUUID() : null;
        if (uuid == null) {
            return null;
        }
        MinecraftServer server = this.level().getServer();
        if (server == null) {
            return this.level().getPlayerByUUID(uuid);
        }
        return server.getPlayerList().getPlayer(uuid);
    }

    public boolean teleportToOwner(LivingEntity owner) {
        BlockPos blockPos = owner.blockPosition();
        for (int i = 0; i < MAX_TELEPORT_ATTEMPTS_TIMES; ++i) {
            int x = this.randomIntInclusive(this.getRandom(), -3, 3);
            int y = this.randomIntInclusive(this.getRandom(), -1, 1);
            int z = this.randomIntInclusive(this.getRandom(), -3, 3);
            if (maybeTeleportTo(owner, blockPos.getX() + x, blockPos.getY() + y, blockPos.getZ() + z)) {
                return true;
            }
        }
        return false;
    }

    private boolean maybeTeleportTo(LivingEntity owner, int x, int y, int z) {
        if (teleportTooClosed(owner, x, z)) {
            return false;
        } else if (!canTeleportTo(new BlockPos(x, y, z))) {
            return false;
        } else {

            this.snapTo(x + 0.5, y, z + 0.5, this.getYRot(), this.getXRot());
            this.getNavigation().stop();
            this.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            this.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
            this.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            this.getBrain().eraseMemory(MemoryModuleType.PATH);
            return true;
        }
    }

    private boolean teleportTooClosed(LivingEntity owner, int x, int z) {
        return Math.abs(x - owner.getX()) < 2 && Math.abs(z - owner.getZ()) < 2;
    }

    private boolean canTeleportTo(BlockPos pos) {
        // 先检查下方方块是否在黑名单中
        BlockState blockState = this.level().getBlockState(pos.below());
        if (blockState.is(TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "maid_avoid_block")))) {
            return false;
        }

        // 再检查路径节点类型和碰撞箱
        PathType pathNodeType = WalkNodeEvaluator.getPathTypeStatic(this, pos);
        if (pathNodeType == PathType.WALKABLE || pathNodeType == PathType.WATER) {
            BlockPos blockPos = pos.subtract(this.blockPosition());
            return this.level().noCollision(this, this.getBoundingBox().move(blockPos));
        }
        return false;
    }

    private int randomIntInclusive(RandomSource random, int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    public ChatBubbleManager getChatBubbleManager() {
        return chatBubbleManager;
    }

    public boolean isAiming() {
        return this.entityData.get(DATA_IS_AIMING);
    }

    public void setAiming(boolean aiming) {
        this.entityData.set(DATA_IS_AIMING, aiming);
    }

    public void spawnFoodParticles(ItemStack stack, int amount) {
        for (int i = 0; i < amount; ++i) {
            Vec3 speed = new Vec3((this.random.nextFloat() - 0.5) * 0.1, Math.random() * 0.1 + 0.1, 0.0);
            speed = speed.xRot(-this.getXRot() * Mth.DEG_TO_RAD);
            speed = speed.yRot(-this.getYRot() * Mth.DEG_TO_RAD);

            double yOffset = -this.random.nextFloat() * 0.6 - 0.3;
            Vec3 pos = new Vec3((this.random.nextFloat() - 0.5) * 0.3, yOffset, 0.6);
            pos = pos.xRot(-this.getXRot() * Mth.DEG_TO_RAD);
            pos = pos.yRot(-this.getYRot() * Mth.DEG_TO_RAD);
            pos = pos.add(this.getX(), this.getEyeY(), this.getZ());

            ItemParticleOption option = new ItemParticleOption(ParticleTypes.ITEM, stack);
            if (this.level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(option, pos.x, pos.y, pos.z, 1, speed.x, speed.y + 0.05, speed.z, 0.0);
            } else {
                this.level.addParticle(option, pos.x, pos.y, pos.z, speed.x, speed.y + 0.05, speed.z);
            }
        }
    }


    /**
     * 因为部分 idea 插件会检查 Map 类里，这些对象做 key 时，是否重写了 equals 和 hashCode 方法， 故这里必须重写这两个方法，但实际上并不需要修改默认父类的实现
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * 因为部分 idea 插件会检查 Map 类里，这些对象做 key 时，是否重写了 equals 和 hashCode 方法， 故这里必须重写这两个方法，但实际上并不需要修改默认父类的实现
     */
    @Override
    public boolean equals(Object pObject) {
        return super.equals(pObject);
    }
}
