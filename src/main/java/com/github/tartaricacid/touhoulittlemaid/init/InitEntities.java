package com.github.tartaricacid.touhoulittlemaid.init;

import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityBroom;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntitySit;
import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.MaidSchedule;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.sensor.MaidHostilesSensor;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.sensor.MaidNearestLivingEntitySensor;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.sensor.MaidPickupEntitiesSensor;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.edible.MaidEdibleBlockAction;
import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.ChatBubbleRegister;
import com.github.tartaricacid.touhoulittlemaid.entity.item.*;
import com.github.tartaricacid.touhoulittlemaid.entity.monster.EntityFairy;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.projectile.EntityDanmaku;
import com.github.tartaricacid.touhoulittlemaid.entity.projectile.EntityThrowPowerPoint;
import com.github.tartaricacid.touhoulittlemaid.entity.projectile.MaidFishingHook;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricTrackedDataRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
// 1.21.11: Schedule/ScheduleBuilder 移除 → EnvironmentAttribute<Activity>
import net.minecraft.world.attribute.AttributeTypes;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;
import java.util.Optional;

public final class InitEntities {
    public static void init() {
        registerSerializer();
        addEntityAttributes();
        addEntitySpawnPlacements();
    }

    public static EntityType<EntityMaid> MAID = registerEntityType("maid", EntityMaid.TYPE);
    public static EntityType<EntityChair> CHAIR = registerEntityType("chair", EntityChair.TYPE);
    public static EntityType<EntityFairy> FAIRY = registerEntityType("fairy", EntityFairy.TYPE);
    public static EntityType<EntityDanmaku> DANMAKU = registerEntityType("danmaku", EntityDanmaku.TYPE);
    public static EntityType<EntityPowerPoint> POWER_POINT = registerEntityType("power_point", EntityPowerPoint.TYPE);
    public static EntityType<EntityExtinguishingAgent> EXTINGUISHING_AGENT = registerEntityType("extinguishing_agent", EntityExtinguishingAgent.TYPE);
    public static EntityType<EntityBox> BOX = registerEntityType("box", EntityBox.TYPE);
    public static EntityType<EntityThrowPowerPoint> THROW_POWER_POINT = registerEntityType("throw_power_point", EntityThrowPowerPoint.TYPE);
    public static EntityType<EntityTombstone> TOMBSTONE = registerEntityType("tombstone", EntityTombstone.TYPE);
    public static EntityType<EntitySit> SIT = registerEntityType("sit", EntitySit.TYPE);
    public static EntityType<EntityBroom> BROOM = registerEntityType("broom", EntityBroom.TYPE);
    public static EntityType<MaidFishingHook> FISHING_HOOK = registerEntityType("fishing_hook", MaidFishingHook.TYPE);

    public static Activity RIDE_IDLE = registerActivity("ride_idle", new Activity("tlm_ride_idle"));
    public static Activity RIDE_WORK = registerActivity("ride_work", new Activity("tlm_ride_work"));
    public static Activity RIDE_REST = registerActivity("ride_rest", new Activity("tlm_ride_rest"));
    public static Activity EMERGENCY_COMBAT = registerActivity("emergency_combat", new Activity("tlm_emergency_combat"));

    public static MemoryModuleType<List<Entity>> VISIBLE_PICKUP_ENTITIES = registerMemoryModuleType("visible_pickup_entities", new MemoryModuleType<>(Optional.empty()));
    public static MemoryModuleType<PositionTracker> TARGET_POS = registerMemoryModuleType("target_pos", new MemoryModuleType<>(Optional.empty()));
    public static MemoryModuleType<MaidEdibleBlockAction> MAID_EDIBLE_BLOCK_ACTION = registerMemoryModuleType("maid_edible_block_action", new MemoryModuleType<>(Optional.empty()));
    /**
     * 偷吃/摆盘持有 {@link #TARGET_POS} 的到期游戏时刻。
     *
     * <p>无 codec，因此不随实体存档持久化——它是一个 tick 级仲裁量，重进世界后重新计时才是正确行为。</p>
     */
    public static MemoryModuleType<Long> MAID_EDIBLE_HOLD_EXPIRY = registerMemoryModuleType("maid_edible_hold_expiry", new MemoryModuleType<>(Optional.empty()));
    public static MemoryModuleType<Boolean> EMERGENCY_COMBAT_ACTIVE = registerMemoryModuleType("emergency_combat_active", new MemoryModuleType<>(Optional.empty()));
    public static SensorType<MaidNearestLivingEntitySensor> MAID_NEAREST_LIVING_ENTITY_SENSOR = registerSensorType("maid_nearest_living_entity", new SensorType<>(MaidNearestLivingEntitySensor::new));
    public static SensorType<MaidHostilesSensor> MAID_HOSTILES_SENSOR = registerSensorType("maid_hostiles", new SensorType<>(MaidHostilesSensor::new));
    public static SensorType<MaidPickupEntitiesSensor> MAID_PICKUP_ENTITIES_SENSOR = registerSensorType("maid_pickup_entities", new SensorType<>(MaidPickupEntitiesSensor::new));

    // 1.21.11: Schedule/ScheduleBuilder 移除 → 调度改用 EnvironmentAttribute<Activity>（javap 确认）。
    // 关键帧不再写在代码里，而在 datapack timeline JSON（data/touhou_little_maid/timeline/maid_schedule.json，
    // 由 #minecraft:timeline/universal tag 烘焙）。此处仅注册 attribute key + 默认活动（timeline 缺失时的回退）。
    // 日程行为（HEAD 原值，见 timeline JSON）：
    //   day_shift : 06:00→WORK(0) / 18:00→IDLE(12000) / 22:00→REST(16000)
    //   night_shift: 06:00→REST(0) / 14:00→IDLE(8000) / 18:00→WORK(12000)
    //   all_day   : WORK(0)
    // ID 与 timeline track key 必须逐字匹配（否则调度静默退化为 defaultValue）。
    public static EnvironmentAttribute<Activity> MAID_DAY_SHIFT_ACTIVITY = registerEnvironment("gameplay/maid_day_shift_activity",
            EnvironmentAttribute.builder(AttributeTypes.ACTIVITY).defaultValue(Activity.IDLE).build());
    public static EnvironmentAttribute<Activity> MAID_NIGHT_SHIFT_ACTIVITY = registerEnvironment("gameplay/maid_night_shift_activity",
            EnvironmentAttribute.builder(AttributeTypes.ACTIVITY).defaultValue(Activity.IDLE).build());
    public static EnvironmentAttribute<Activity> MAID_ALL_DAY_ACTIVITY = registerEnvironment("gameplay/maid_all_day_activity",
            EnvironmentAttribute.builder(AttributeTypes.ACTIVITY).defaultValue(Activity.WORK).build());

    private static <T extends EntityType<?>> T registerEntityType(String id, T eType) {
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, id), eType);
    }

    private static <T extends MemoryModuleType<?>> T registerMemoryModuleType(String id, T mType) {
        return Registry.register(BuiltInRegistries.MEMORY_MODULE_TYPE, Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, id), mType);
    }

    private static <T extends SensorType<?>> T registerSensorType(String id, T sType) {
        return Registry.register(BuiltInRegistries.SENSOR_TYPE, Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, id), sType);
    }

    // 1.21.11: registerSchedule → registerEnvironment（BuiltInRegistries.SCHEDULE 移除 → ENVIRONMENT_ATTRIBUTE）
    private static EnvironmentAttribute<Activity> registerEnvironment(String id, EnvironmentAttribute<Activity> attribute) {
        return Registry.register(BuiltInRegistries.ENVIRONMENT_ATTRIBUTE, Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, id), attribute);
    }

    private static Activity registerActivity(String id, Activity activity) {
        return Registry.register(BuiltInRegistries.ACTIVITY, Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, id), activity);
    }

    private static void registerSerializer() {
        // 1.21.11 Fabric: 自定义 EntityDataSerializer 禁止用 vanilla EntityDataSerializers.registerSerializer
        //   （会 desync）→ 改用 FabricTrackedDataRegistry.register(Identifier, handler) 分配稳定网络 id。
        FabricTrackedDataRegistry.register(Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "maid_schedule"), MaidSchedule.DATA);
        //   EntityGraphics/ChatBubbleRenderer/IChatBubbleRenderer 三个核心渲染类被掏空为 stub，且实体渲染管线需 1.21.11 重设计）。
        //   此序列化器已孤立（其消费方 EntityMaid:245 的 CHAT_BUBBLE 数据字段已注释，A12）→ 冻结零功能损失。
        FabricTrackedDataRegistry.register(Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "chat_bubble"), ChatBubbleRegister.INSTANCE);
    }

    private static void addEntityAttribute(EntityType<? extends LivingEntity> type, AttributeSupplier.Builder builder) {
        FabricDefaultAttributeRegistry.register(type, builder);
    }

    private static void addEntityAttributes() {
        addEntityAttribute(EntityMaid.TYPE, EntityMaid.createAttributes());
        addEntityAttribute(EntityChair.TYPE, LivingEntity.createLivingAttributes());
        addEntityAttribute(EntityBroom.TYPE, LivingEntity.createLivingAttributes());
        addEntityAttribute(EntityFairy.TYPE, EntityFairy.createFairyAttributes());
    }

    private static void addEntitySpawnPlacements() {
        SpawnPlacements.register(InitEntities.FAIRY, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EntityFairy::checkFairySpawnRules);
    }
}
