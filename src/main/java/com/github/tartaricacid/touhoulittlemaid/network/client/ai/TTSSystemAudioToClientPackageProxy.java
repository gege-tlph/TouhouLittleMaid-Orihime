package com.github.tartaricacid.touhoulittlemaid.network.client.ai;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.AvailableSites;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSSite;
import com.github.tartaricacid.touhoulittlemaid.network.message.ai.TTSSystemAudioToClientPackage;

public final class TTSSystemAudioToClientPackageProxy {
    public static void handle(TTSSystemAudioToClientPackage message) {
        String siteName = message.siteName();
        TTSSite ttsSite = AvailableSites.getTTSSite(siteName);
        // 服务端已经决定要播这一句，客户端却放不出来：静默 return 的表现是「女仆张嘴没声音」，
        // 玩家和管理员都无从判断是站点没同步过来，还是本地把它禁用了，故必须留下痕迹
        if (ttsSite == null) {
            TouhouLittleMaid.LOGGER.warn("Server asked to play TTS from site {}, "
                    + "but the client site registry has no entry for it", siteName);
            return;
        }
        if (!ttsSite.enabled()) {
            TouhouLittleMaid.LOGGER.warn("Server asked to play TTS from site {}, "
                    + "but it is disabled on this client", siteName);
            return;
        }
        ttsSite.client().play(message.chatText(), message.config(), null);
    }
}
