package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.layout;

import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSSite;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.editor.TTSSiteEditorScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.Translations.MODELS_NAME;

/**
 * TTS 站点编辑器的布局策略基类
 */
public abstract class TTSSiteFormLayout {
    protected final TTSSite sourceSite;

    protected TTSSiteFormLayout(TTSSite sourceSite) {
        this.sourceSite = sourceSite;
    }

    public abstract List<FieldDescriptor> getFieldDescriptors();

    /**
     * 站点级说明，画在字段区里。
     *
     * <p>存在的理由是**不许有说谎的标签**：系统站点一个字段都没有，空白页无法告诉管理员
     * 「这里本来就没有可配置项」还是「界面坏了」；更要紧的是它必须说清
     * <b>女仆的合成语种设置在这个站点上不生效</b>——那个设置就在聊天屏的 🌐 里，
     * 看得见、点得动、存得下，而 {@code TTSSystemClient} 把 {@code TTSConfig} 整个丢掉了。
     * 一个能设置却不起作用的选项，比没有这个选项更糟。</p>
     */
    public List<Component> hints() {
        return List.of();
    }

    /**
     * 这个站点有没有可配置的东西。
     *
     * <p>没有的话，「保存」与「检查配置」都是**只能骗人的按钮**：系统朗读器站点
     * {@code url()} 返回空串，检查必然停在「站点还没填地址」这条红字上，永远不可能成功；
     * 而它的 {@code buildSite} 返回的是一份逐字相同的副本，存了等于没存。</p>
     *
     * <p>按「不许有说谎的标签」，这种按钮不该只是没用，而是<b>不该出现</b>。
     * 判据取「有没有可编辑字段或模型行」而不是写死站点 id——以后再加同类站点会自动适用。</p>
     */
    public boolean isConfigurable() {
        return !this.getFieldDescriptors().isEmpty() || this.supportsModelRows();
    }

    @Nullable
    public abstract TTSSite buildSite(Function<String, String> fieldValues, Map<String, String> models, Consumer<Component> showStatus);

    /**
     * 额外添加按钮等组件
     *
     * @param x      按钮起始 x 坐标
     * @param y      按钮起始 y 坐标
     * @param width  可用宽度（不包含左右边距）
     * @param screen 当前编辑界面实例
     * @return 该组件整体占用的高度，用于后续其他组件调整自身 Y 值
     */
    public int extraInit(int x, int y, int width, TTSSiteEditorScreen screen) {
        return 0;
    }

    public Map<String, String> getInitialModels() {
        return Map.of();
    }

    public boolean supportsModelRows() {
        return false;
    }

    public MutableComponent modelsTitle() {
        return MODELS_NAME;
    }
}
