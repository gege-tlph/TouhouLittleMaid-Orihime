package cn.sh1rocu.touhoulittlemaid;

import cn.sh1rocu.touhoulittlemaid.api.event.*;
import cn.sh1rocu.touhoulittlemaid.api.extension.IBedBlock;
import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.event.*;
import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.RandomEmoji;
import com.github.tartaricacid.touhoulittlemaid.config.GeneralConfig;
import com.github.tartaricacid.touhoulittlemaid.config.ServerConfig;
import com.github.tartaricacid.touhoulittlemaid.debug.event.DebugStickClickEvent;
import com.github.tartaricacid.touhoulittlemaid.debug.target.SendMaidDebugDataEvent;
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
import fuzs.forgeconfigapiport.fabric.api.v5.ModConfigEvents;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.fml.config.ModConfig;

public class TouhouLittleMaidFabric implements ModInitializer {

    public static final net.minecraft.resources.Identifier HIGHEST = net.minecraft.resources.Identifier.fromNamespaceAndPath("touhou_little_maid", "highest");
    public static final net.minecraft.resources.Identifier HIGH = net.minecraft.resources.Identifier.fromNamespaceAndPath("touhou_little_maid", "high");
    public static final net.minecraft.resources.Identifier LOW = net.minecraft.resources.Identifier.fromNamespaceAndPath("touhou_little_maid", "low");
    public static final net.minecraft.resources.Identifier LOWEST = net.minecraft.resources.Identifier.fromNamespaceAndPath("touhou_little_maid", "lowest");

    @Override
    public void onInitialize() {

        registerConfiguration();
        // 在服务器端模型索引扫描 tlm_custom_pack 之前注册内容并解压捆绑的默认模型包。  安装 mod 后，首先扫描会使整个第一个进程的索引为空，因此女仆名称会回退到原始实体翻译键，直到重新启动。
        TouhouLittleMaid.commonSetup();
        CommonRegistry.onSetupEvent();
        CompatRegistry.onEnqueue();
        DatapackRegistry.onAddReloadListenerEvent();

        subscribeEvents();
        subscribeDebugEvents();
    }

    private static void registerConfiguration() {
        ConfigRegistry.INSTANCE.register(TouhouLittleMaid.MOD_ID, ModConfig.Type.COMMON, GeneralConfig.getConfigSpec());
        ConfigRegistry.INSTANCE.register(TouhouLittleMaid.MOD_ID, ModConfig.Type.SERVER, ServerConfig.init());
    }

    private void subscribeEvents() {
        EntitySleepEvents.SET_BED_OCCUPATION_STATE.register((entity, sleepingPos, bedState, occupied) -> {
            if (bedState.getBlock() instanceof IBedBlock bedBlock && bedBlock.tlm$isBed(bedState, entity.level(), sleepingPos, entity)) {
                // 必须写入事件提供的占用状态；若固定为 true，女仆起床后床仍会保持占用。
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

        ModConfigEvents.loading(TouhouLittleMaid.MOD_ID).register(MaidMealRegConfigEvent::onEvent);
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
        // 这也强制执行了船、矿车和其他车辆的一般可乘坐开关；回调不仅仅限于椅子。
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
