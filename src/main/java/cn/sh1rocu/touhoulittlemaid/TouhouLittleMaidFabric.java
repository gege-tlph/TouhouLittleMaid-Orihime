package cn.sh1rocu.touhoulittlemaid;

import cn.sh1rocu.touhoulittlemaid.api.event.*;
import cn.sh1rocu.touhoulittlemaid.api.extension.IBedBlock;
import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.event.*;
import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.RandomEmoji;
import com.github.tartaricacid.touhoulittlemaid.config.AiClientConfig;
import com.github.tartaricacid.touhoulittlemaid.config.AiServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.config.GeneralConfig;
import com.github.tartaricacid.touhoulittlemaid.config.ConfigFileMigration;
import com.github.tartaricacid.touhoulittlemaid.config.ServerConfig;
import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.debug.event.DebugStickClickEvent;
import com.github.tartaricacid.touhoulittlemaid.debug.target.SendMaidDebugDataEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.combat.MaidCombatDamageListener;
import com.github.tartaricacid.touhoulittlemaid.event.*;
import com.github.tartaricacid.touhoulittlemaid.event.food.ConvertFoodEatenEvent;
import com.github.tartaricacid.touhoulittlemaid.event.food.RemainFoodEatenEvent;
import com.github.tartaricacid.touhoulittlemaid.event.maid.*;
import com.github.tartaricacid.touhoulittlemaid.init.registry.CommonRegistry;
import com.github.tartaricacid.touhoulittlemaid.init.registry.CompatRegistry;
import com.github.tartaricacid.touhoulittlemaid.init.registry.DatapackRegistry;
import com.github.tartaricacid.touhoulittlemaid.init.registry.MobSpawnInfoRegistry;
import com.github.tartaricacid.touhoulittlemaid.item.ItemSubstituteJizo;
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.fml.config.ModConfig;

public class TouhouLittleMaidFabric implements ModInitializer {
    // Fabric event phase constants (Identifier type in Fabric API 0.141+)
    public static final net.minecraft.resources.Identifier HIGHEST = net.minecraft.resources.Identifier.fromNamespaceAndPath("touhou_little_maid", "highest");
    public static final net.minecraft.resources.Identifier HIGH = net.minecraft.resources.Identifier.fromNamespaceAndPath("touhou_little_maid", "high");
    public static final net.minecraft.resources.Identifier LOW = net.minecraft.resources.Identifier.fromNamespaceAndPath("touhou_little_maid", "low");
    public static final net.minecraft.resources.Identifier LOWEST = net.minecraft.resources.Identifier.fromNamespaceAndPath("touhou_little_maid", "lowest");

    @Override
    public void onInitialize() {
        // B6e (2026-07-16): 入口点已恢复，顺序严格对齐 HEAD。
        //   AI 模块初始化较快，需最优先加载 config，否则 ConfigProxySelector 的 config 字段可能为 null（HEAD 原注）。
        registerConfiguration();
        // Register content and unpack the bundled default model pack before the
        // server-side model index scans tlm_custom_pack.  Scanning first leaves
        // the index empty for the entire first process after installing the mod,
        // so maid names fall back to the raw entity translation key until restart.
        TouhouLittleMaid.commonSetup();
        CommonRegistry.onSetupEvent();
        CompatRegistry.onEnqueue();
        DatapackRegistry.onAddReloadListenerEvent();

        subscribeEvents();
        subscribeDebugEvents();
    }

    private static void registerConfiguration() {
        ServerConfig.init();
        AiServerRuleConfig.init();
        ServerRuleConfig.initializeDefaults();
        AiServerRuleConfig.initializeDefaults();
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            var spec = GeneralConfig.getConfigSpec();
            var aiSpec = AiClientConfig.getConfigSpec();
            ConfigFileMigration.migrateGlobalFileIfNeeded(GeneralConfig.values(), spec);
            // 必须先于 spec 加载：旧 global 文件缺 ReplaceMagmaCubeModel 时继承旧史莱姆值，
            // 否则缺键会被默认 true 补掉。
            ConfigFileMigration.inheritMagmaCubeFromSlime();
            // 同样必须先于注册：注册加载会把 -global.toml 里已不在 spec 的 ai 节剥掉，老值要先搬走
            ConfigFileMigration.migrateAiFileIfNeeded(AiClientConfig.values(), aiSpec);
            ConfigRegistry.INSTANCE.register(TouhouLittleMaid.MOD_ID, ModConfig.Type.CLIENT,
                    spec, ConfigFileMigration.GLOBAL_FILE_NAME);
            ConfigRegistry.INSTANCE.register(TouhouLittleMaid.MOD_ID, ModConfig.Type.CLIENT,
                    aiSpec, ConfigFileMigration.AI_FILE_NAME);
        }
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            if (!ServerRuleConfig.loadForServer(server)) {
                throw new IllegalStateException("Failed to load Touhou Little Maid world config");
            }
            // AI 规则是实例级文件（§17 v2）；首次启动会先从本存档的旧世界文件播种老值
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
        ServerLivingEntityEvents.AFTER_DAMAGE.register(MaidCombatDamageListener::onAfterDamage);
        EntitySleepEvents.SET_BED_OCCUPATION_STATE.register((entity, sleepingPos, bedState, occupied) -> {
            if (bedState.getBlock() instanceof IBedBlock bedBlock && bedBlock.tlm$isBed(bedState, entity.level(), sleepingPos, entity)) {
                // The Fabric 1.21.1 baseline accidentally hard-coded true here. Its NeoForge
                // counterpart writes false from stopSleeping; this event argument is that
                // same start/stop state and must be preserved so the bed can be reused.
                entity.level().setBlock(sleepingPos, bedState.setValue(BedBlock.OCCUPIED, occupied), 3);
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
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
        EntityDeathEvent.onEntityDeath();
        EntityDeathEvent.onPlayerCloned();
        PotentialSpawnsEvent.CALLBACK.register(MobSpawnInfoRegistry::addMobSpawnInfo);
        UseItemCallback.EVENT.register(CancelSaddleMaidEvent::onItemRightClick);
        UseEntityCallback.EVENT.register(CopyEntityIdEvent::copyEntityId);
        UseEntityCallback.EVENT.register(InstallChairEvent::onPlayerEntityInteract);
        PlayerLoggedInEvent.CALLBACK.register(EnterServerEvent::onAttachCapabilityEvent);
        ProjectileImpactEvent.CALLBACK.register(EntityHurtEvent::onArrowImpact);
        EntityJoinLevelEvent.CALLBACK.register(EntityJoinWorldEvent::onCreeperJoinWorld);
        EntityJoinLevelEvent.CALLBACK.register(EntityJoinWorldEvent::onAnimalJoinWorld);
        EntityJoinLevelEvent.CALLBACK.register(EntityJoinWorldEvent::onPlayerJoinWorld);
        EntityTrackingEvents.START_TRACKING.register(MaidTrackEvent::onTrackingPlayer);
        MaidAfterEatEvent.CALLBACK.register(ConvertFoodEatenEvent::onAfterMaidEat);
        MaidAfterEatEvent.CALLBACK.register(RemainFoodEatenEvent::onAfterMaidEat);
        InteractMaidEvent.CALLBACK.register(ApplyGoldenAppleEvent::onInteractMaid);
        InteractMaidEvent.CALLBACK.register(ApplyPotionEffectEvent::onInteractMaid);
        InteractMaidEvent.CALLBACK.register(LOW, DismountMaidEvent::onInteract);
        InteractMaidEvent.CALLBACK.register(GetExpBottleEvent::onInteract);
        InteractMaidEvent.CALLBACK.register(HandleBackpackEvent::onInteractMaid);
        InteractMaidEvent.CALLBACK.register(MaidAreaClickEvent::onInteract);
        MaidDeathEvent.CALLBACK.register(MaidDeathFavorability::onDeath);
        FarmlandTrampleEvent.CALLBACK.register(MaidFarmlandTrample::onFarmlandTrample);
        // This also enforces the general rideable switch for boats, minecarts
        // and other vehicles; the callback is not chair-only.
        EntityMountEvent.CALLBACK.register(MaidMountEvent::onMaidMount);
        LivingEntityUseItemFinishEvent.CALLBACK.register(PotionItemUse::onMaidPotionItemUse);
        InteractMaidEvent.CALLBACK.register(SaddleMaidEvent::onInteract);
        InteractMaidEvent.CALLBACK.register(SlabClickEvent::onInteract);
        InteractMaidEvent.CALLBACK.register(LOWEST, SwitchSittingEvent::onInteractMaid);
        InteractMaidEvent.CALLBACK.register(UseFavorabilityToolEvent::onInteract);
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
            InteractMaidEvent.CALLBACK.register(UseNameTagEvent::onInteractServer);
        }
        InteractMaidEvent.CALLBACK.register(ItemSubstituteJizo::onEntityInteract);

        MaidDamageEvent.CALLBACK.register(LOWEST, RandomEmoji::addHurtChatText);

        MaidFavorabilityLevelChangeEvent.CALLBACK.register(MaidDropBaubleEvent::onFavorabilityLevelChange);
    }

    private static void subscribeDebugEvents() {
        InteractMaidEvent.CALLBACK.register(DebugStickClickEvent::onInteract);
        PlayerTickEvent.START.register(SendMaidDebugDataEvent::onPlayerTick);
    }
}
