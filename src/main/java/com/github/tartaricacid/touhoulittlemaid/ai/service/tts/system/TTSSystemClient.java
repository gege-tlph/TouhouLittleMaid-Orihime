package com.github.tartaricacid.touhoulittlemaid.ai.service.tts.system;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSResponse;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSClient;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSConfig;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSSystemServices;
import com.mojang.text2speech.Narrator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

public class TTSSystemClient implements TTSClient, TTSSystemServices {
    /** 旁白可用与否在一次运行里不会变，告警一次即可 */
    private static boolean narratorUnavailableWarned = false;

    @Override
    public void play(String message, TTSConfig config, TTSResponse callback) {
        if (isClient()) {
            onHandle(message);
        }
    }

    /**
     * 系统语音走的是 <b>MC 旁白</b>（text2speech / Windows SAPI），不是任何合成站点。
     *
     * <p>原版自己调这个 API 前会先查 {@code narrator.active()}（见 {@code GameNarrator#saySystemNow}），
     * <b>我们从前不查</b>：text2speech 没初始化成功时 {@code say} 是一个纯空操作，
     * 于是「女仆张嘴没声音」在任何一侧都不留痕迹。而玩家从未打开过旁白选项时，
     * 原版那句「无法初始化语音库」的提示也不会弹——他没有任何途径知道这件事。</p>
     *
     * <p>注意本方法带 {@code @Environment(CLIENT)}，专服上会被整个剥掉，
     * 因此<b>方法体里不得出现 lambda</b>：javac 生成的合成方法不带这个注解，剥不掉，
     * 会把 {@link Narrator} 留在专服的类签名里（statue/garage_kit/altar 三处已实证过一次）。</p>
     */
    @Environment(EnvType.CLIENT)
    private void onHandle(String message) {
        Minecraft mc = Minecraft.getInstance();
        Narrator narrator = mc.getNarrator().narrator;
        if (!narrator.active()) {
            if (!narratorUnavailableWarned) {
                narratorUnavailableWarned = true;
                TouhouLittleMaid.LOGGER.warn("The system voice cannot speak: this client's text-to-speech library "
                        + "is not active, so every maid using the system voice will stay silent");
            }
            return;
        }
        narrator.say(message, true, 1.0f);
    }
}
