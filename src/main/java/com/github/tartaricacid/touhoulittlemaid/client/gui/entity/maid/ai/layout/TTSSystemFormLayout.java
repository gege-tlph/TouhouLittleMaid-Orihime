package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.layout;

import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.system.TTSSystemSite;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 系统 TTS：无可编辑字段，仅显示提示文本
 */
public class TTSSystemFormLayout extends TTSSiteFormLayout {
    public TTSSystemFormLayout(TTSSite sourceSite) {
        super(sourceSite);
    }

    @Override
    public List<FieldDescriptor> getFieldDescriptors() {
        return List.of();
    }

    /**
     * 系统站点没有任何可配置项，而**女仆的合成语种在这里不生效**，必须说出来。
     *
     * <p>{@code TTSSystemClient.play} 只把文本交给原版朗读器，传进来的 {@code TTSConfig}
     * （含语种与音色）从未被读取；原版朗读器接口也不接受这两个参数，语音完全由操作系统决定。
     * 这一行不是装饰——聊天屏的 🌐 语种选择对本站点是个无效开关，
     * 不写出来玩家只会以为「设了没用是 bug」。</p>
     *
     * <p>⚠️ 采取的是**诚实标注**而非行为修复：原版朗读器接口给不出语种参数，
     * 想真正生效只能换实现（那是超基准新功能）。标注的成本是一行字，
     * 而沉默的成本是玩家反复调一个不可能生效的开关。</p>
     */
    @Override
    public List<Component> hints() {
        return List.of(
                Component.translatable("ai.touhou_little_maid.chat.settings.hub.system_tts_hint"),
                Component.translatable("ai.touhou_little_maid.chat.settings.hub.system_tts_language_hint"));
    }

    @Override
    public @Nullable TTSSite buildSite(Function<String, String> fieldValues, Map<String, String> models, Consumer<Component> showStatus) {
        TTSSystemSite site = (TTSSystemSite) this.sourceSite;
        return new TTSSystemSite(site.id(), site.icon(), site.enabled());
    }
}
