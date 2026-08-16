package cn.sh1rocu.touhoulittlemaid;

import cn.sh1rocu.touhoulittlemaid.api.event.*;
import cn.sh1rocu.touhoulittlemaid.api.extension.IBedBlock;
import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.event.InteractMaidEvent;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidDamageEvent;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidDeathEvent;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidFavorabilityLevelChangeEvent;
import com.github.tartaricacid.touhoulittlemaid.config.AiClientConfig;
import com.github.tartaricacid.touhoulittlemaid.config.AiServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.config.CommonConfig;
import com.github.tartaricacid.touhoulittlemaid.config.ConfigFileMigration;
import com.github.tartaricacid.touhoulittlemaid.config.ServerConfig;
import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.debug.event.DebugStickClickEvent;
import com.github.tartaricacid.touhoulittlemaid.debug.target.SendMaidDebugDataEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.combat.MaidCombatDamageListener;
import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.RandomEmoji;
import com.github.tartaricacid.touhoulittlemaid.event.*;
import com.github.tartaricacid.touhoulittlemaid.event.maid.*;
import com.github.tartaricacid.touhoulittlemaid.init.registry.*;
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import fuzs.forgeconfigapiport.fabric.api.v5.ModConfigEvents;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.util.EventResult;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

public class TouhouLittleMaidFabric implements ModInitializer {
    public static final Identifier HIGHEST = Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "event_highest_priority");
    public static final Identifier HIGH = Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "event_high_priority");
    // NORMAL用Fabric的DEFAULT
    // public static final Identifier NORMAL = Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "event_normal_priority");
    public static final Identifier LOW = Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "event_low_priority");
    public static final Identifier LOWEST = Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "event_lowest_priority");

    @Nullable
    private static WeakReference<MinecraftServer> server;

    @Nullable
    public static MinecraftServer getServer() {
        if (server == null) {
            return null;
        }
        return server.get();
    }

    @Override
    public void onInitialize() {
        // AI模块初始化较快，需要最优先加载config，否则ConfigProxySelector的config字段可能为null
        registerConfiguration();
        // 必须先 commonSetup（解包内置默认模型包到 tlm_custom_pack）再 onSetupEvent（扫描服务端模型表）：
        // 顺序反了则首装后的整个首个进程里模型表为空，女仆名退回裸实体键名，直到重启才恢复
        TouhouLittleMaid.commonSetup();
        CommonRegistry.onSetupEvent();
        CompatRegistry.onEnqueue();
        DatapackRegistry.onAddReloadListenerEvent();
        DatapackSyncEvent.onDatapackSyncEvent();

        subscribeEvents();
        subscribeDebugEvents();
    }

    private static void registerConfiguration() {
        // 世界规则的 spec 先建起来（校验与默认值要用），但**有意不注册**：注册 Type.SERVER 会让
        // Forge Config API Port 自己去管 <world>/serverconfig/touhou_little_maid-server.toml，
        // 与 ServerRuleConfig 争同一个文件。详见 ServerConfig 的类注释。
        ServerConfig.init();
        // AI 规则的 spec 同理只建不注册（它由 AiServerRuleConfig 独占那个文件），
        // 且必须在任何 ServerRuleConfig.get 路由调用之前建好——归属表是在这里登记的。
        AiServerRuleConfig.init();
        ModConfigSpec aiClientSpec = AiClientConfig.getConfigSpec();

        // 以下三条迁移**必须全部先于 COMMON spec 注册**：这些键原属 COMMON spec，
        // 注册那一刻 correct() 会把「已不在 spec 里」的它们整批剥掉，旧值就没了。
        ConfigFileMigration.migrateServerFileIfNeeded(ServerRuleConfig.values(), ServerConfig.CONFIG);
        ConfigFileMigration.migrateAiFileIfNeeded(AiClientConfig.values(), aiClientSpec);
        // 旧文件缺岩浆怪独立开关时继承史莱姆开关的旧值
        ConfigFileMigration.inheritMagmaCubeFromSlime();

        ServerRuleConfig.initializeDefaults();
        AiServerRuleConfig.initializeDefaults();
        ConfigRegistry.INSTANCE.register(TouhouLittleMaid.MOD_ID, ModConfig.Type.COMMON, CommonConfig.init());
        // 个人 AI 配置是独立文件，正常交给 FCAP 管（与上面那份 COMMON 同类型、不同文件名）
        ConfigRegistry.INSTANCE.register(TouhouLittleMaid.MOD_ID, ModConfig.Type.COMMON,
                aiClientSpec, ConfigFileMigration.AI_FILE_NAME);

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            if (!ServerRuleConfig.loadForServer(server)) {
                throw new IllegalStateException("Failed to load Touhou Little Maid world config");
            }
            // AI 规则是实例级文件；首次启动会先从本存档的旧世界文件与 common 播种老值
            if (!AiServerRuleConfig.loadForServer(server)) {
                throw new IllegalStateException("Failed to load Touhou Little Maid AI rule config");
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ServerRuleConfig.unloadWorld();
            AiServerRuleConfig.unload();
        });
    }

    private void subscribeEvents() {
        ServerLifecycleEvents.SERVER_STARTING.register((server) -> TouhouLittleMaidFabric.server = new WeakReference<>(server));

        EntitySleepEvents.SET_BED_OCCUPATION_STATE.register((entity, sleepingPos, bedState, occupied) -> {
            if (bedState.getBlock() instanceof IBedBlock bedBlock && bedBlock.tlm$isBed(bedState, entity.level(), sleepingPos, entity)) {
                entity.level().setBlock(sleepingPos, bedState.setValue(BedBlock.OCCUPIED, true), 3);
                return true;
            }
            return false;
        });
        EntitySleepEvents.MODIFY_SLEEPING_DIRECTION.register((entity, sleepingPos, direction) -> {
            var bedState = entity.level().getBlockState(sleepingPos);
            if (bedState.getBlock() instanceof IBedBlock bedBlock && bedBlock.tlm$isBed(bedState, entity.level(), sleepingPos, entity)) {
                return bedState.getValue(HorizontalDirectionalBlock.FACING);
            }
            return direction;
        });
        EntitySleepEvents.ALLOW_BED.register((entity, sleepingPos, bedState, vanillaResult) -> {
            if (bedState.getBlock() instanceof IBedBlock bedBlock && bedBlock.tlm$isBed(bedState, entity.level(), sleepingPos, entity)) {
                return EventResult.ALLOW;
            }
            return EventResult.PASS;
        });
        EntityDeathEvent.onEntityDeath();
        EntityDeathEvent.onPlayerCloned();
        // 威胁响应（§3.B）：只认「最终生效的正伤害」，女仆侧由 passive.MaidCombatManager.actuallyHurt 手动 invoke
        ServerLivingEntityEvents.AFTER_DAMAGE.register(MaidCombatDamageListener::onAfterDamage);
        PotentialSpawnsEvent.CALLBACK.register(MobSpawnInfoRegistry::addMobSpawnInfo);
        UseItemCallback.EVENT.register(CancelSaddleMaidEvent::onItemRightClick);
        UseEntityCallback.EVENT.register(CopyEntityIdEvent::copyEntityId);
        UseEntityCallback.EVENT.register(InstallChairEvent::onPlayerEntityInteract);
        ServerPlayerEvents.JOIN.register(EnterServerEvent::onAttachCapabilityEvent);
        ProjectileImpactEvent.CALLBACK.register(EntityHurtEvent::onArrowImpact);
        ServerEntityEvents.ENTITY_LOAD.register(EntityJoinWorldEvent::onCreeperJoinWorld);
        ServerEntityEvents.ENTITY_LOAD.register(EntityJoinWorldEvent::onAnimalJoinWorld);
        ServerEntityEvents.ENTITY_LOAD.register(EntityJoinWorldEvent::onPlayerJoinWorld);
        EntityTrackingEvents.START_TRACKING.register(MaidTrackEvent::onTrackingPlayer);
        InteractMaidEvent.CALLBACK.register(ApplyGoldenAppleEvent::onInteractMaid);
        InteractMaidEvent.CALLBACK.register(ApplyPotionEffectEvent::onInteractMaid);
        InteractMaidEvent.CALLBACK.register(LOW, DismountMaidEvent::onInteract);
        InteractMaidEvent.CALLBACK.register(GetExpBottleEvent::onInteract);
        InteractMaidEvent.CALLBACK.register(HandleBackpackEvent::onInteractMaid);
        InteractMaidEvent.CALLBACK.register(MaidAreaClickEvent::onInteract);
        MaidDeathEvent.CALLBACK.register(MaidDeathFavorability::onDeath);
        FarmlandTrampleEvent.CALLBACK.register(MaidFarmlandTrample::onFarmlandTrample);
        EntityMountEvent.CALLBACK.register(MaidMountEvent::onMaidMount);
        LivingEntityUseItemFinishEvent.CALLBACK.register(PotionItemUse::onMaidPotionItemUse);
        InteractMaidEvent.CALLBACK.register(SaddleMaidEvent::onInteract);
        InteractMaidEvent.CALLBACK.register(SlabClickEvent::onInteract);
        InteractMaidEvent.CALLBACK.register(LOWEST, SwitchSittingEvent::onInteractMaid);
        InteractMaidEvent.CALLBACK.register(UseFavorabilityToolEvent::onInteract);
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
            InteractMaidEvent.CALLBACK.register(UseNameTagEvent::onInteractServer);
        }

        MaidDamageEvent.CALLBACK.register(LOWEST, RandomEmoji::addHurtChatText);

        MaidFavorabilityLevelChangeEvent.CALLBACK.register(MaidDropBaubleEvent::onFavorabilityLevelChange);
    }

    private static void subscribeDebugEvents() {
        InteractMaidEvent.CALLBACK.register(DebugStickClickEvent::onInteract);
        PlayerTickEvent.START.register(SendMaidDebugDataEvent::onPlayerTick);
    }
}
