package com.github.tartaricacid.touhoulittlemaid.network.client.ai;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.client.sound.data.MaidAISoundInstance;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.network.message.ai.TTSAudioToClientPackage;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/**
 * 远程 TTS 的落地端：服务端合成好音频后下发，这里播出来。
 *
 * <p>诊断可见性与系统 TTS 那条路对齐——`789465799` 已为系统路补过 warn，<b>这条路当时被记为
 * 「尚未审查」</b>（2026-07-28 审计补上）。三个前置条件不满足时原先一律静默 return，
 * 而它们在玩家侧的表现完全一样：「女仆张嘴没声音」，处置却完全不同。
 * 账本里那个未定位的「管理员配好却不说话」，排查第一步就是查客户端日志；
 * 若这条路一声不响，配了远程站点的管理员等于没有第一步。</p>
 */
public final class TTSAudioToClientPackageProxy {
    public static void handle(TTSAudioToClientPackage message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            TouhouLittleMaid.LOGGER.warn("Received TTS audio for maid {} while no level is loaded", message.maidId());
            return;
        }
        Entity entity = mc.level.getEntity(message.maidId());
        if (!(entity instanceof EntityMaid maid)) {
            // 服务端按「女仆的主人」发包，而主人可能离女仆很远——那时她不在本客户端的加载范围里
            TouhouLittleMaid.LOGGER.warn("Received TTS audio for entity {}, which is not a loaded maid here",
                    message.maidId());
            return;
        }
        if (!maid.isAlive()) {
            TouhouLittleMaid.LOGGER.warn("Received TTS audio for maid {}, but she is no longer alive", message.maidId());
            return;
        }
        mc.getSoundManager().play(new MaidAISoundInstance(maid, message.data()));
    }
}
