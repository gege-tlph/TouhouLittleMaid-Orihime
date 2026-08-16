package com.github.tartaricacid.touhoulittlemaid.network.message.ai;

import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.AvailableSites;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SerializerRegister;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSSystemServices;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.siliconflow.TTSSiliconflowSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.system.TTSSystemSite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tartaricacid.touhoulittlemaid.network.message.ai.RequestVoicePreviewPackage.Validation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * 试听请求的服务端校验——四个「不可用」分支不需要网络，也不该需要网络才能测。
 *
 * <p>成功路径要真实密钥与真实 API，归实机验收；无密钥的失败路径（no_secret）是确定性的，
 * 同样归实机清单——这里只钉校验层。</p>
 */
class VoicePreviewRequestValidationTest {
    @BeforeEach
    void initialize() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        AvailableSites.TTS_SITES.clear();

        TTSSiliconflowSite enabled = new TTSSiliconflowSite.Serializer().defaultSite();
        enabled.setEnabled(true);
        AvailableSites.TTS_SITES.put(enabled.id(), enabled);

        TTSSiliconflowSite disabled = new TTSSiliconflowSite.Serializer().defaultSite();
        disabled.setEnabled(false);
        AvailableSites.TTS_SITES.put("disabled-clone", disabled);

        // 「本地播放」现在按 client 类型判，站点必须真的在表里才判得出来
        TTSSystemSite system = new TTSSystemSite.Serializer().defaultSite();
        system.setEnabled(true);
        AvailableSites.TTS_SITES.put(system.id(), system);
    }

    @Test
    void aMissingSiteIsUnavailable() {
        assertEquals(Validation.MISSING, RequestVoicePreviewPackage.validate("no-such-site", ""));
    }

    @Test
    void aDisabledSiteIsUnavailable() {
        assertEquals(Validation.DISABLED, RequestVoicePreviewPackage.validate("disabled-clone", ""));
    }

    @Test
    void theSystemVoiceNeverSynthesisesOnTheServer() {
        // 客户端会本地直合成，正常流程根本不发这个包；服务端仍然拦——防未来客户端改动漏掉特例
        assertEquals(Validation.CLIENT_LOCAL, RequestVoicePreviewPackage.validate(TTSSystemSite.API_TYPE, ""));
    }

    /**
     * <b>凡本地播放的站点都必须判成 CLIENT_LOCAL，不管它叫什么 id。</b>
     *
     * <p>这条是为 Player2 立的：它和系统语音一样只在物理客户端动作、且丢弃 callback，
     * 但因为 id 不是 {@code system}，旧判据把它当云站点送进服务端合成——服务端既不出声也永不回包，
     * 玩家只能干等 10 秒超时。判据从「id 等于 system」改成「client 是本地播放型」后，
     * 将来再加同类站点也自动覆盖，不必有人记得回来改这里。</p>
     */
    @Test
    void everyLocallyPlayingSiteIsRefusedByTheServer() {
        for (var entry : SerializerRegister.TTS_SERIALIZER.entrySet()) {
            TTSSite site = entry.getValue().defaultSite();
            site.setEnabled(true);
            AvailableSites.TTS_SITES.put(site.id(), site);
            if (site.client() instanceof TTSSystemServices) {
                assertEquals(Validation.CLIENT_LOCAL, RequestVoicePreviewPackage.validate(site.id(), ""),
                        site.id() + " 的 client 是本地播放型，服务端必须回绝，否则它永远不会有终态");
            } else {
                assertNotEquals(Validation.CLIENT_LOCAL, RequestVoicePreviewPackage.validate(site.id(), ""),
                        site.id() + " 是远程站点，不该被当成本地播放");
            }
        }
    }

    @Test
    void anUnknownVoiceIdIsUnavailable() {
        String siteId = new TTSSiliconflowSite.Serializer().defaultSite().id();
        assertEquals(Validation.BAD_MODEL, RequestVoicePreviewPackage.validate(siteId, "no-such-voice"));
    }

    @Test
    void aValidRequestPassesValidation() {
        String siteId = new TTSSiliconflowSite.Serializer().defaultSite().id();
        assertEquals(Validation.OK, RequestVoicePreviewPackage.validate(siteId, ""));
    }

    /**
     * 服务端限频：正常客户端单请求在途碰不到它，这道闸拦的是改装客户端刷云合成。
     * 拒绝不滑动窗口——连点不能把冷却越推越远，否则等满一秒的老实人反而永远进不来。
     */
    @Test
    void spamIsThrottledPerPlayerWithoutSlidingTheWindow() {
        java.util.UUID player = java.util.UUID.randomUUID();
        java.util.UUID other = java.util.UUID.randomUUID();
        long t0 = 1_000_000L;

        org.junit.jupiter.api.Assertions.assertTrue(RequestVoicePreviewPackage.tryAcquire(player, t0));
        org.junit.jupiter.api.Assertions.assertFalse(RequestVoicePreviewPackage.tryAcquire(player, t0 + 200));
        org.junit.jupiter.api.Assertions.assertFalse(RequestVoicePreviewPackage.tryAcquire(player, t0 + 900));
        // 被拒的两次没有推远窗口：距首次接受满 1s 即恢复
        org.junit.jupiter.api.Assertions.assertTrue(RequestVoicePreviewPackage.tryAcquire(player, t0 + 1000));
        // 限频按玩家计，别人不被殃及
        org.junit.jupiter.api.Assertions.assertTrue(RequestVoicePreviewPackage.tryAcquire(other, t0 + 200));
    }
}
