package com.github.tartaricacid.touhoulittlemaid.client.sound;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.AvailableSites;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSConfig;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSSystemServices;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.system.TTSSystemSite;
import com.github.tartaricacid.touhoulittlemaid.client.sound.data.PreviewVoiceSoundInstance;
import com.github.tartaricacid.touhoulittlemaid.network.message.ai.RequestVoicePreviewPackage;
import com.github.tartaricacid.touhoulittlemaid.network.message.ai.VoicePreviewResultPackage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

/**
 * 音色试听的客户端状态机：一次只有一个在途请求，四态齐全。
 *
 * <ul>
 *   <li>请求中——按钮/行内显示等待字样，10 秒超时按失败处理</li>
 *   <li>成功——非定位播放（{@link PreviewVoiceSoundInstance}）</li>
 *   <li>失败——按来源反馈：设置屏走行内红字，T 屏走聊天栏（那一屏的告知媒介本来就是聊天栏）</li>
 *   <li>不可用——服务端校验回绝（站点缺失/被禁/模型非法），同一条反馈通道</li>
 * </ul>
 *
 * <p>系统语音特例：**客户端本地直合成**，不走网络——它的合成本来就发生在每台机器上。</p>
 */
@Environment(EnvType.CLIENT)
public final class VoicePreviewClient {
    private static final long TIMEOUT_MS = 10_000;
    private static final String LANG_PREFIX = "ai.touhou_little_maid.";
    private static final String SAMPLE_ZH = "你好呀，主人，今天想听我说点什么？";
    private static final String SAMPLE_EN = "Hello, master. How do I sound today?";

    public enum Origin {
        /** 设置屏：失败走行内红字 */
        SETTINGS,
        /** T 屏弹出层：失败走聊天栏 */
        CHAT
    }

    private static int nextRequestId = 1;
    private static int pendingId = -1;
    private static long pendingSince;
    /** 上一次真正发出云试听的时刻；界面据它多憋一个服务端冷却期，见 isBusy() */
    private static long lastRequestAt;
    private static String pendingSite = "";
    private static String pendingModel = "";
    private static Origin pendingOrigin = Origin.SETTINGS;

    private static @Nullable Component inlineError;
    private static long inlineErrorUntil;

    private VoicePreviewClient() {
    }

    /**
     * 发起一次试听。
     *
     * <p><b>客户端只回答一个问题：这个站点能不能在本机直接合成。</b>能——本地放，不走网络；
     * 不能（含「本机这张表里根本没有它」）——原样交给服务端，由它权威判定。</p>
     *
     * <p>为什么不在本地判「站点不存在」：{@code AvailableSites} 在两侧各自由**自己那份**
     * {@code sites/tts.json} 填充（`init()` 挂在 main entrypoint，两侧都跑）。专服上管理员配的站点
     * 客户端本地多半没有，本地一拒就等于「服务端明明有，玩家却按不动」——而单人档里两张表是同一张，
     * 这个洞在单人档下按定义测不出来。存在性判定归服务端，它有权威表也有对应的 UNAVAILABLE 回执。</p>
     */
    public static void request(String siteId, String modelId, Origin origin) {
        // 本地合成先判：它不花服务器的钱、没有 requestId、也没有什么会过期，
        // 因此不该被一个在途的云试听挡住（否则等 10 秒才能听系统语音）
        String effectiveSite = StringUtils.isBlank(siteId) ? TTSSystemSite.API_TYPE : siteId;
        TTSSite site = AvailableSites.getTTSSite(effectiveSite);
        if (site != null && site.client() instanceof TTSSystemServices services) {
            inlineError = null;
            playLocalSample(services, modelId);
            return;
        }
        if (isBusy()) {
            return;
        }
        inlineError = null;
        pendingId = nextRequestId++;
        pendingSince = System.currentTimeMillis();
        lastRequestAt = pendingSince;
        pendingSite = effectiveSite;
        pendingModel = StringUtils.defaultString(modelId);
        pendingOrigin = origin;
        // 带上本机界面语言：样例语句给谁听就说谁的语言（世界默认语种已删）
        ClientPlayNetworking.send(new RequestVoicePreviewPackage(effectiveSite, pendingModel, pendingId,
                Minecraft.getInstance().getLanguageManager().getSelected()));
    }

    /**
     * 本机合成的站点（系统语音、Player2）在本地直出——网络上什么都不该出现。
     *
     * <p><b>必须给真实的 {@link TTSConfig}</b>：系统语音的 client 忽略它，但 Player2 的实现会
     * 解引用 {@code config.model()}。曾经这里传 null，对 Player2 就是把一次静默超时换成 NPE。</p>
     */
    private static void playLocalSample(TTSSystemServices services, String modelId) {
        String language = Minecraft.getInstance().getLanguageManager().getSelected();
        String prefix = language.contains("_") ? language.split("_")[0] : language;
        services.play(prefix.startsWith("zh") ? SAMPLE_ZH : SAMPLE_EN,
                new TTSConfig(StringUtils.defaultString(modelId), prefix), null);
    }

    /** 离开界面时丢弃在途请求：不报失败，只是别让下一个屏被一个孤儿请求闷住 10 秒 */
    public static void forget() {
        clearPending();
        inlineError = null;
    }

    public static boolean isPendingAny() {
        expireIfTimedOut();
        return pendingId >= 0;
    }

    /**
     * 界面用的「现在点不动」判据：在途，**或者**距上次发出不足服务端的冷却期。
     *
     * <p>后半句是为了让两道闸别互相打架：服务端每玩家 1 秒限频，而一次瞬时失败（比如没密钥）
     * 会立刻清掉在途状态——玩家紧接着再点一下就会撞上限频，看到一句莫名其妙的「太频繁」。
     * 界面自己多憋这一秒，那条提示就回归它本来的用途：只有改装客户端才会看到。</p>
     */
    public static boolean isBusy() {
        if (isPendingAny()) {
            return true;
        }
        return lastRequestAt > 0
                && System.currentTimeMillis() - lastRequestAt
                < com.github.tartaricacid.touhoulittlemaid.network.message.ai.RequestVoicePreviewPackage.COOLDOWN_MS;
    }

    public static boolean isPendingFor(String siteId, String modelId) {
        expireIfTimedOut();
        return pendingId >= 0 && pendingSite.equals(siteId)
                && pendingModel.equals(StringUtils.defaultString(modelId));
    }

    private static void expireIfTimedOut() {
        if (pendingId >= 0 && System.currentTimeMillis() - pendingSince > TIMEOUT_MS) {
            Origin origin = pendingOrigin;
            clearPending();
            fail(Component.translatable("ai.touhou_little_maid.chat.preview.timeout"), origin);
        }
    }

    public static void handleResult(VoicePreviewResultPackage message) {
        if (message.requestId() != pendingId) {
            // 过期结果：玩家已经点了下一次，旧音频不该出声
            return;
        }
        Origin origin = pendingOrigin;
        clearPending();
        switch (message.status()) {
            case OK -> Minecraft.getInstance().getSoundManager()
                    .play(new PreviewVoiceSoundInstance(message.audio()));
            case UNAVAILABLE -> fail(Component.translatable(message.detail()), origin);
            case FAILED -> fail(failureText(message.detail()), origin);
        }
    }

    private static Component failureText(String detail) {
        // 已知失败发的是 lang 键（如凭据缺失），未知失败是 HTTP 层原文——两种都要能显示
        if (detail.startsWith(LANG_PREFIX)) {
            return Component.translatable(detail);
        }
        return Component.translatable("ai.touhou_little_maid.chat.preview.failed", detail);
    }

    private static void fail(Component reason, Origin origin) {
        TouhouLittleMaid.LOGGER.warn("Voice preview did not play: {}", reason.getString());
        if (origin == Origin.CHAT) {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                player.displayClientMessage(((MutableComponent) reason).withStyle(ChatFormatting.RED), false);
                return;
            }
        }
        inlineError = reason;
        inlineErrorUntil = System.currentTimeMillis() + 5_000;
    }

    /** 设置屏行内红字；过期自动消失 */
    public static @Nullable Component inlineError() {
        if (inlineError != null && System.currentTimeMillis() > inlineErrorUntil) {
            inlineError = null;
        }
        return inlineError;
    }

    public static Component buttonLabel(String siteId, String modelId) {
        if (isPendingFor(siteId, modelId)) {
            return Component.translatable("ai.touhou_little_maid.chat.preview.requesting");
        }
        return Component.translatable("ai.touhou_little_maid.chat.preview.button");
    }

    private static void clearPending() {
        pendingId = -1;
        pendingSite = "";
        pendingModel = "";
    }
}
