package com.github.tartaricacid.touhoulittlemaid.client.input;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaidClient;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.STTCallback;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.AvailableSites;
import com.github.tartaricacid.touhoulittlemaid.ai.service.stt.STTConfig;
import com.github.tartaricacid.touhoulittlemaid.ai.service.stt.STTSite;
import com.github.tartaricacid.touhoulittlemaid.client.sound.record.MicrophoneManager;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitSounds;
import com.github.tartaricacid.touhoulittlemaid.network.client.config.ServerRulesClientCache;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class STTChatKey {
    private static STTSite activeRecordingSite;
    private static boolean activeRecordingUsesServer;

    public static final KeyMapping STT_CHAT_KEY = new KeyMapping("key.touhou_little_maid.stt_chat.desc",
// KeyConflictContext.IN_GAME,KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            TouhouLittleMaidClient.KEY_CATEGORY);

    public static void onSttChatPress(int key, int scanCode, int action, int mods) {
        if (keyIsMatch(key, scanCode, action, mods)) {
            if (!ServerRuleConfig.get(AIConfig.LLM_ENABLED)) {
                return;
            }
            if (!AIConfig.STT_ENABLED.get()) {
                return;
            }
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null || player.isSpectator()) {
                return;
            }
            if (!isInGame()) {
                return;
            }
            STT_CHAT_KEY.consumeClick();
            if (action == GLFW.GLFW_PRESS) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(InitSounds.RECORDING_START, 1f));
                getNearestMaid(player, STTChatKey::sttStart, true);
                return;
            }
            if (action == GLFW.GLFW_RELEASE) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(InitSounds.RECORDING_END, 1f));
                getNearestMaid(player, STTChatKey::sttStop, false);
            }
        }
    }

    private static boolean keyIsMatch(int key, int scanCode, int action, int mods) {
        return STT_CHAT_KEY.matches(new KeyEvent(key, scanCode, mods))
;
    }

    private static void getNearestMaid(LocalPlayer player, Consumer<EntityMaid> consumer, boolean isStart) {
        Level level = player.level;
        int range = AIConfig.MAID_CAN_CHAT_DISTANCE.get();
        AABB aabb = player.getBoundingBox().inflate(range);
        List<EntityMaid> maids = level.getEntitiesOfClass(EntityMaid.class, aabb, maid -> maid.isOwnedBy(player) && maid.isAlive());
        maids.sort(Comparator.comparingDouble(maid -> maid.distanceToSqr(player)));
        if (!maids.isEmpty()) {
            consumer.accept(maids.get(0));
            return;
        }
        if (isStart) {
            player.displayClientMessage(Component.translatable("ai.touhou_little_maid.chat.stt.no_maid_found", range), false);
        }
    }

    private static boolean isInGame() {
        Minecraft mc = Minecraft.getInstance();
        // 不能是加载界面
        if (mc.getOverlay() != null) {
            return false;
        }
        // 不能打开任何 GUI
        if (mc.screen != null) {
            return false;
        }
        // 当前窗口捕获鼠标操作
        if (!mc.mouseHandler.isMouseGrabbed()) {
            return false;
        }
        // 选择了当前窗口
        return mc.isWindowActive();
    }

    private static void sttStart(EntityMaid maid) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        activeRecordingSite = null;
        activeRecordingUsesServer = false;
        boolean serverProvided = ServerRulesClientCache.isUsingServerStt();
        STTSite sttSite = serverProvided
                ? ServerRulesClientCache.runtimeServerSttSite()
                : AvailableSites.getSTTSite(AIConfig.STT_TYPE.get().getName());
        if (sttSite == null) {
            if (serverProvided) {
                ServerRulesClientCache.requestServerSttSite();
                player.displayClientMessage(Component.translatable(
                        "ai.touhou_little_maid.chat.stt.server_unavailable"), false);
            } else {
                player.displayClientMessage(Component.translatable("ai.touhou_little_maid.chat.stt.empty"), false);
            }
            return;
        }
        activeRecordingSite = sttSite;
        activeRecordingUsesServer = serverProvided;
        tryToStart(maid, player, sttSite, serverProvided);
    }

    private static void tryToStart(EntityMaid maid, LocalPlayer player, STTSite sttSite,
                                   boolean serverProvided) {
        STTConfig config = new STTConfig();
        STTCallback callback = new STTCallback(player, maid, serverProvided);
        sttSite.client().startRecord(config, callback);
    }

    private static void sttStop(EntityMaid maid) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        STTSite sttSite = activeRecordingSite;
        if (sttSite != null) {
            STTConfig config = new STTConfig();
            STTCallback callback = new STTCallback(player, maid, activeRecordingUsesServer);
            sttSite.client().stopRecord(config, callback);
        }
        activeRecordingSite = null;
        activeRecordingUsesServer = false;
    }

    public static void cancelServerRecording() {
        if (!activeRecordingUsesServer) {
            return;
        }
        MicrophoneManager.cancelRecord();
        activeRecordingSite = null;
        activeRecordingUsesServer = false;
    }
}
