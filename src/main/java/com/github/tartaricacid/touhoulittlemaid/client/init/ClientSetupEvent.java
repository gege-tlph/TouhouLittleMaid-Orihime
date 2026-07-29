package com.github.tartaricacid.touhoulittlemaid.client.init;

import cn.sh1rocu.touhoulittlemaid.api.event.AddPackFindersEvent;
import com.github.tartaricacid.touhoulittlemaid.client.animation.HardcodedAnimationManger;
import com.github.tartaricacid.touhoulittlemaid.client.animation.gecko.AnimationRegister;
import com.github.tartaricacid.touhoulittlemaid.client.animation.gecko.magic.MagicCastingAnimationManager;
import com.github.tartaricacid.touhoulittlemaid.client.event.ShowOptifineScreen;
import com.github.tartaricacid.touhoulittlemaid.client.input.DismountBroomKey;
import com.github.tartaricacid.touhoulittlemaid.client.input.STTChatKey;
import com.github.tartaricacid.touhoulittlemaid.client.overlay.BroomTipsOverlay;
import com.github.tartaricacid.touhoulittlemaid.client.overlay.MaidTipsOverlay;
import com.github.tartaricacid.touhoulittlemaid.client.overlay.ShowPowerOverlay;
import com.github.tartaricacid.touhoulittlemaid.client.resource.LegacyPackRepositorySource;
import com.github.tartaricacid.touhoulittlemaid.client.resource.listener.EmojiReloadListener;
import com.github.tartaricacid.touhoulittlemaid.compat.iris.IrisCompat;
import com.github.tartaricacid.touhoulittlemaid.compat.patpat.PatPatCompat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;

@Environment(EnvType.CLIENT)
public class ClientSetupEvent {
    // private static final Identifier CROSSHAIR = Identifier.withDefaultNamespace("hud/crosshair");
    // private static final Identifier HOTBAR = Identifier.withDefaultNamespace("hud/hotbar");

    public static void onClientSetup() {
        AnimationRegister.registerAnimationState();
        MaidTipsOverlay.init();
        ShowOptifineScreen.checkOptifineIsLoaded();
        HardcodedAnimationManger.init();
        MagicCastingAnimationManager.init();
        resisterKeyMappings();
        AddPackFindersEvent.CALLBACK.register(ClientSetupEvent::onAddPackFinders);

        // 客户端兼容
        IrisCompat.init();
        // The 1.21.11 renderer submits ordinary VertexConsumer geometry and is
        // natively accepted by Sodium. The removed legacy fast path had no live
        // consumer and repeated its own face culling, which broke thin Gecko
        // eye/tail planes instead of providing a compatibility requirement.
        PatPatCompat.init();
    }

    public static void onRegisterGuiLayers() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR,
                Identifier.fromNamespaceAndPath("touhou_little_maid", "maid_tips"), MaidTipsOverlay.INSTANCE::render);
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR,
                Identifier.fromNamespaceAndPath("touhou_little_maid", "broom_tips"), BroomTipsOverlay.INSTANCE::render);
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR,
                Identifier.fromNamespaceAndPath("touhou_little_maid", "show_power"), ShowPowerOverlay.INSTANCE::render);
        // 追踪标记：必须是 HUD 层，世界内的三种画法都会被光影管线吃掉或反过来破坏画面，
        // 详见 TrackerMarkerOverlay 的类注释
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR,
                Identifier.fromNamespaceAndPath("touhou_little_maid", "tracker_marker"),
                com.github.tartaricacid.touhoulittlemaid.client.overlay.TrackerMarkerOverlay.INSTANCE::render);
    }

    public static void resisterKeyMappings() {
        KeyBindingHelper.registerKeyBinding(STTChatKey.STT_CHAT_KEY);
        KeyBindingHelper.registerKeyBinding(DismountBroomKey.DISMOUNT_KEY);
    }

    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            event.addRepositorySource(new LegacyPackRepositorySource());
        }
    }

    public static void onRegisterClientReloadListeners() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new EmojiReloadListener());
    }
}
