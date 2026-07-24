package cn.sh1rocu.touhoulittlemaid.client;

import cn.sh1rocu.touhoulittlemaid.api.event.*;
import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaidClient;
import com.github.tartaricacid.touhoulittlemaid.api.event.InteractMaidEvent;
import com.github.tartaricacid.touhoulittlemaid.api.event.client.MaidPackLoaderEvent;
import com.github.tartaricacid.touhoulittlemaid.api.event.client.RenderMaidEvent;
import com.github.tartaricacid.touhoulittlemaid.client.animation.special.HardcodedAnimation;
import com.github.tartaricacid.touhoulittlemaid.client.download.InfoGetManager;
import com.github.tartaricacid.touhoulittlemaid.client.event.*;
import com.github.tartaricacid.touhoulittlemaid.client.init.*;
import com.github.tartaricacid.touhoulittlemaid.client.input.DismountBroomKey;

import com.github.tartaricacid.touhoulittlemaid.client.input.STTChatKey;

import com.github.tartaricacid.touhoulittlemaid.event.ClientExtensionsEvent;
import com.github.tartaricacid.touhoulittlemaid.event.ClientTickEvent;

import com.github.tartaricacid.touhoulittlemaid.event.maid.UseNameTagEvent;
import com.github.tartaricacid.touhoulittlemaid.network.NetworkHandler;
import com.github.tartaricacid.touhoulittlemaid.network.client.ClientAltarRecipeCache;
import com.github.tartaricacid.touhoulittlemaid.util.EntityCacheUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.Event;

import static cn.sh1rocu.touhoulittlemaid.TouhouLittleMaidFabric.*;

public class TouhouLittleMaidFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        TouhouLittleMaidClient.setup();
        NetworkHandler.registerClientReceivers();
        ClientExtensionsEvent.RegisterClientExtensions();
        InfoGetManager.onClientSetup();
        RegisterSpecialModelEvent.registerSpecialModelRenderers();

        ClientTickEvents.START_CLIENT_TICK.register(ClientTickEvent::onClientTick);

        // 必须先于渲染器注册：Gecko 横幅层需要内部模型管理器。
        com.github.tartaricacid.touhoulittlemaid.client.init.ClientReloadListenerRegistry.onRegisterClientReloadListeners();

        ItemTooltipCallback.EVENT.addPhaseOrdering(Event.DEFAULT_PHASE, LOW);
        ItemTooltipCallback.EVENT.addPhaseOrdering(LOW, LOWEST);
        ItemTooltipCallback.EVENT.register(LOWEST, AddInformationEvent::onRenderTooltips);

        RenderHandEvent.CALLBACK.register(CarryMaidHideArmEvent::onRenderHandEvent);
        WorldRenderEvents.END_EXTRACTION.register(CompassRenderEvent::onRender);
        WorldRenderEvents.END_EXTRACTION.register(MaidAreaRenderEvent::onRender);

        InteractMaidEvent.CALLBACK.register(UseNameTagEvent::onInteractClient);

        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register(
                (handler, sender, client) -> PlayerLoggedInNotice.onEnterGame(client));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> {
                    ClientAltarRecipeCache.clear();
                    com.github.tartaricacid.touhoulittlemaid.network.client.config.ServerRulesClientCache.clear();
                    com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig.activatePendingValues();
                    com.github.tartaricacid.touhoulittlemaid.client.download.ClientPackDownloadManager.downloadClientPack();
                });
        PlaySoundEvent.CALLBACK.register(MaidSoundFreqEvent::onPlaySoundEvent);
        PlaySoundSourceEvent.CALLBACK.register(PlayMaidSoundEvent::onPlaySoundSource);

        KeyInputCallback.EVENT.register(PressAIChatKeyEvent::onOpenConfig);
        KeyInputCallback.EVENT.register(DismountBroomKey::onDismountPress);

        KeyInputCallback.EVENT.register(STTChatKey::onSttChatPress);
        WorldRenderEvents.END_EXTRACTION.register(ScrollRenderEvent::onRenderWorldLastEvent);

        ScreenEvents.AFTER_INIT.register(ShowOptifineScreen::showOptifineWarning);

        // 模型详情屏调试地板的自定义 PiP 渲染器（AbstractModelDetailsGui showFloor 分支的提交端）
        net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry.register(
                ctx -> new com.github.tartaricacid.touhoulittlemaid.client.gui.entity.detail.DebugFloorPiPRenderer(ctx.vertexConsumers()));

        RenderMaidEvent.CALLBACK.register(HIGHEST, SpecialMaidRenderEvent::onRenderPlayerNamedMaid);
        RenderMaidEvent.CALLBACK.register(Event.DEFAULT_PHASE, SpecialMaidRenderEvent::onRenderEncryptNamedMaid);
        RenderMaidEvent.CALLBACK.register(LOW, SpecialMaidRenderEvent::onRenderNormalNamedMaid);
        RenderMaidEvent.CALLBACK.register(LOWEST, SpecialMaidRenderEvent::onRenderEasterEggModel);
        MaidPackLoaderEvent.LEGACY.register(HardcodedAnimation::onMaidPackLoader);

        WorldRenderEvents.END_EXTRACTION.register(WirelessIORenderEvent::onRender);

        ClientSetupEvent.onClientSetup();
        ClientSetupEvent.onRegisterGuiLayers();
        ClientSetupEvent.onRegisterClientReloadListeners();

        TooltipComponentCallback.EVENT.register(InitClientTooltip::onRegisterClientTooltip);
        InitContainerGui.clientSetup();
        InitEntitiesRender.onEntityRenderers();
        InitEntitiesRender.onRegisterLayers();
        ModelLoadingPlugin.register(new InitSpecialItemRender());
        EntityJoinLevelEvent.CALLBACK.register(EntityCacheUtil::onChangeDim);


    }
}
