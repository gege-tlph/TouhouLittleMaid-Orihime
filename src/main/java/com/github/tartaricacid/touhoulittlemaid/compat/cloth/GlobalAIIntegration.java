package com.github.tartaricacid.touhoulittlemaid.compat.cloth;

import com.github.tartaricacid.touhoulittlemaid.ai.service.stt.STTApiType;
import com.github.tartaricacid.touhoulittlemaid.client.sound.record.MicrophoneManager;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;

/**
 * Cloth 菜单里的 AI 页，**只剩个人配置那一组**（语音识别）。
 *
 * <p>LLM / TTS 那两组已删：它们现在是<b>实例级 AI 规则</b>（{@code AiServerRuleConfig}，
 * 服务器权威），而本页是客户端直接读写 TOML 的通道。两者不可并存——菜单写 TOML 就绕过了
 * 服务器权威通道，专服上会出现「客户端显示已改、服务端根本不知道」的分叉；
 * 且 AI 规则的 spec 有意不注册，这里的裸 {@code XXX.get()} 会当场抛
 * {@code Cannot get config value before config is loaded}。
 * 这与 TACZ 三滑块当初从个人配置段搬进服务器规则段是同一处教训。</p>
 *
 * <p>⚠️ <b>本类是过渡态</b>：行为基准 {@code port/1.21.11-fabric} 上整个文件都不存在——
 * AI 的一切（含这里剩下的语音识别项）都收进了游戏内的 AI 设置屏五页
 * （{@code AIChatSettingsSTTConfigScreen} 保存时只写 {@code AiClientConfig.CONFIG}）。
 * 那屏属审计 §3.C 的 GUI 那一刀；<b>它落地时本文件连同 {@code MenuIntegration} 里的调用点
 * 一并删除</b>。在此之前保留这一组，否则个人语音配置在这一轮里会无处可改。</p>
 */
public class GlobalAIIntegration {
    public static void aiChat(ConfigBuilder root, ConfigEntryBuilder entryBuilder) {
        ConfigCategory aiChat = root.getOrCreateCategory(Component.translatable("config.touhou_little_maid.global_ai"));
        sttConfig(entryBuilder, aiChat);
    }

    private static void sttConfig(ConfigEntryBuilder entryBuilder, ConfigCategory aiChat) {
        SubCategoryBuilder builder = entryBuilder.startSubCategory(Component.translatable("config.touhou_little_maid.global_ai.stt"));
        builder.setExpanded(true);

        builder.add(entryBuilder.startBooleanToggle(Component.translatable("config.touhou_little_maid.global_ai.stt_enable"), AIConfig.STT_ENABLED.get())
                .setDefaultValue(true).setTooltip(Component.translatable("config.touhou_little_maid.global_ai.stt_enable.tooltip"))
                .setSaveConsumer(s -> {
                    AIConfig.STT_ENABLED.set(s);
                    AIConfig.STT_ENABLED.save();
                }).build());

        builder.add(entryBuilder.startEnumSelector(Component.translatable("config.touhou_little_maid.global_ai.stt_type"), STTApiType.class, AIConfig.STT_TYPE.get())
                .setDefaultValue(STTApiType.PLAYER2).setTooltip(Component.translatable("config.touhou_little_maid.global_ai.stt_type.tooltip"))
                .setSaveConsumer(s -> {
                    AIConfig.STT_TYPE.set(s);
                    AIConfig.STT_TYPE.save();
                }).build());

        builder.add(entryBuilder.startSelector(Component.translatable("config.touhou_little_maid.global_ai.stt_microphone"),
                        MicrophoneManager.getAllMicrophoneName(), AIConfig.STT_MICROPHONE.get())
                .setDefaultValue(StringUtils.EMPTY).setTooltip(Component.translatable("config.touhou_little_maid.global_ai.stt_microphone.tooltip"))
                .setSaveConsumer(s -> {
                    AIConfig.STT_MICROPHONE.set(s);
                    AIConfig.STT_MICROPHONE.save();
                }).build());

        builder.add(entryBuilder.startIntSlider(Component.translatable("config.touhou_little_maid.global_ai.maid_can_chat_distance"),
                        AIConfig.MAID_CAN_CHAT_DISTANCE.get(), 1, 256).setDefaultValue(12)
                .setTooltip(Component.translatable("config.touhou_little_maid.global_ai.maid_can_chat_distance.tooltip"))
                .setSaveConsumer(s -> {
                    AIConfig.MAID_CAN_CHAT_DISTANCE.set(s);
                    AIConfig.MAID_CAN_CHAT_DISTANCE.save();
                }).build());

        builder.add(entryBuilder.startStrField(Component.translatable("config.touhou_little_maid.global_ai.stt_proxy_address"), AIConfig.STT_PROXY_ADDRESS.get())
                .setDefaultValue(StringUtils.EMPTY)
                .setTooltip(Component.translatable("config.touhou_little_maid.global_ai.stt_proxy_address.tooltip"))
                .setSaveConsumer(s -> {
                    AIConfig.STT_PROXY_ADDRESS.set(s);
                    AIConfig.STT_PROXY_ADDRESS.save();
                }).build());

        aiChat.addEntry(builder.build());
    }
}
