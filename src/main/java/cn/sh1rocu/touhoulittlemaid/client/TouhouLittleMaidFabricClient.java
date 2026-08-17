package cn.sh1rocu.touhoulittlemaid.client;

import cn.sh1rocu.touhoulittlemaid.api.event.KeyInputCallback;
import cn.sh1rocu.touhoulittlemaid.api.event.PlaySoundEvent;
import cn.sh1rocu.touhoulittlemaid.api.event.PlaySoundSourceEvent;
import cn.sh1rocu.touhoulittlemaid.api.event.RenderHandEvent;
import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaidClient;
import com.github.tartaricacid.touhoulittlemaid.api.event.InteractMaidEvent;
import com.github.tartaricacid.touhoulittlemaid.api.event.client.MaidPackLoaderEvent;
import com.github.tartaricacid.touhoulittlemaid.client.animation.special.HardcodedAnimation;
import com.github.tartaricacid.touhoulittlemaid.client.download.InfoGetManager;
import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.network.client.config.ServerRulesClientCache;
import com.github.tartaricacid.touhoulittlemaid.client.event.*;
import com.github.tartaricacid.touhoulittlemaid.client.init.*;
import com.github.tartaricacid.touhoulittlemaid.client.input.DismountBroomKey;
import com.github.tartaricacid.touhoulittlemaid.client.input.STTChatKey;
import com.github.tartaricacid.touhoulittlemaid.debug.target.DebugClientRenderEvent;
import com.github.tartaricacid.touhoulittlemaid.event.ClientExtensionsEvent;
import com.github.tartaricacid.touhoulittlemaid.event.ClientTickEvent;
import com.github.tartaricacid.touhoulittlemaid.network.NetworkHandler;
import com.github.tartaricacid.touhoulittlemaid.util.EntityCacheUtil;
import fuzs.forgeconfigapiport.fabric.api.v5.ModConfigEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.recipe.v1.sync.ClientRecipeSynchronizedEvent;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.Event;

import static cn.sh1rocu.touhoulittlemaid.TouhouLittleMaidFabric.LOW;
import static cn.sh1rocu.touhoulittlemaid.TouhouLittleMaidFabric.LOWEST;

public class TouhouLittleMaidFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientRecipeSynchronizedEvent.EVENT.register(ClientRecipeEvent::onRecipeReceived);
        TouhouLittleMaidClient.setup();
        NetworkHandler.registerClientReceivers();
        // 离开服务器后必须复位：运行期快照是那台服务器的值，缓存里还留着它的编辑权与文件值。
        // 不清的话，下一次进单人档会先按上一台服务器的规则跑，直到它的首包到达。
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> {
                    ServerRulesClientCache.clear();
                    ServerRuleConfig.activatePendingValues();
                });
        ClientExtensionsEvent.RegisterClientExtensions();
        InfoGetManager.onClientSetup();

        ClientReloadListenerRegistry.onRegisterClientReloadListeners();
        RegisterSpecialModelEvent.registerSpecialModelRenderers();

        MaidPackLoaderEvent.LEGACY.register(HardcodedAnimation::onMaidPackLoader);

        ItemTooltipCallback.EVENT.addPhaseOrdering(Event.DEFAULT_PHASE, LOW);
        ItemTooltipCallback.EVENT.addPhaseOrdering(LOW, LOWEST);
        ItemTooltipCallback.EVENT.register(LOWEST, AddInformationEvent::onRenderTooltips);

        RenderHandEvent.CALLBACK.register(CarryMaidHideArmEvent::onRenderHandEvent);
        ModConfigEvents.loading(TouhouLittleMaid.MOD_ID).register(ClientPackDownloadEvent::onLoadingConfig);
        ModConfigEvents.reloading(TouhouLittleMaid.MOD_ID).register(ClientPackDownloadEvent::onReloadingConfig);
        LevelRenderEvents.AFTER_SOLID_FEATURES.register(CompassRenderEvent::onRender);
        LevelRenderEvents.AFTER_SOLID_FEATURES.register(MaidAreaRenderEvent::onRender);
        InteractMaidEvent.CALLBACK.register(UseNameTagEvent::onInteractClient);
        ServerPlayerEvents.JOIN.register(PlayerLoggedInNotice::onEnterGame);
        PlaySoundEvent.CALLBACK.register(MaidSoundFreqEvent::onPlaySoundEvent);
        PlaySoundSourceEvent.CALLBACK.register(PlayMaidSoundEvent::onPlaySoundSource);
        KeyInputCallback.EVENT.register(PressAIChatKeyEvent::onOpenConfig);
        KeyInputCallback.EVENT.register(STTChatKey::onSttChatPress);
        KeyInputCallback.EVENT.register(DismountBroomKey::onDismountPress);
        // 追踪标记不再画在世界里：世界内的「置顶」会清主渲染目标的深度贴图，光影下地面发白。
        // 改由 HUD 层的 TrackerMarkerOverlay 承担（ClientSetupEvent 注册）
        ScreenEvents.AFTER_INIT.register(ShowOptifineScreen::showOptifineWarning);

        LevelRenderEvents.AFTER_SOLID_FEATURES.register(WirelessIORenderEvent::onRender);
        ClientSetupEvent.onClientSetup();
        ClientSetupEvent.onRegisterGuiLayers();
        ClientTooltipComponentCallback.EVENT.register(InitClientTooltip::onRegisterClientTooltip);
        InitContainerGui.clientSetup();
        InitEntitiesRender.onEntityRenderers();
        // 图腾/经验瓶的可替换物品模型（VanillaConfig 后两个开关的消费者）
        net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin.register(
                new com.github.tartaricacid.touhoulittlemaid.client.init.InitSpecialItemRender());
        InitEntitiesRender.onRegisterLayers();
        ClientEntityEvents.ENTITY_LOAD.register(EntityCacheUtil::onChangeDim);
        LevelRenderEvents.AFTER_SOLID_FEATURES.register(DebugClientRenderEvent::onRender);
        ClientTickEvents.START_CLIENT_TICK.register(ClientTickEvent::onClientTick);
    }
}
