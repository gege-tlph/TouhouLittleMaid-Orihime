package com.github.tartaricacid.touhoulittlemaid.compat.cloth;

import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

/**
 * 只有副作用、没有值的一行：左边一个标题，右边一颗按钮。
 *
 * <p>Cloth 的条目模型假定每一行代表一个可编辑的值，而这一行代表的是「跳到别处去」，
 * 所以取值/默认值/保存全是空实现，{@link #isEdited()} 恒 {@code false}——
 * 它永远不该让菜单显示成「有未保存改动」。</p>
 *
 * <p>⚠️ <b>渲染写法对 26.1.2，不对行为基准</b>：26.1.2 删掉了 {@code GuiGraphics}，
 * Cloth 26.1 的条目改成抽取模型 {@code extractRenderState(GuiGraphicsExtractor, …)}，
 * 文本走 {@code extractor.text(...)}、子控件走它自己的 {@code extractRenderState}。
 * 照基准那份 {@code render(GuiGraphics, …)} 抄过来是编译不过的
 * （「能整取」按子系统成立，而 GUI 这一层的渲染骨架被重写了）。</p>
 */
final class ActionButtonListEntry extends TooltipListEntry<Void> {
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    /** 标题基线相对行顶的偏移，与 Cloth 自带条目一致，免得这一行看起来比邻居高。 */
    private static final int LABEL_Y_OFFSET = 6;

    private final Button button;

    ActionButtonListEntry(Component fieldName, Component buttonText, Runnable action) {
        super(fieldName, Optional::empty);
        this.button = Button.builder(buttonText, ignored -> action.run())
                .bounds(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT).build();
    }

    @Override
    public boolean isEdited() {
        return false;
    }

    @Override
    public Void getValue() {
        return null;
    }

    @Override
    public Optional<Void> getDefaultValue() {
        return Optional.empty();
    }

    @Override
    public void save() {
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return List.of(this.button);
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return List.of(this.button);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int index, int y, int x,
                                   int entryWidth, int entryHeight, int mouseX, int mouseY,
                                   boolean hovered, float partialTick) {
        super.extractRenderState(extractor, index, y, x, entryWidth, entryHeight,
                mouseX, mouseY, hovered, partialTick);
        this.button.setX(x + entryWidth - this.button.getWidth());
        this.button.setY(y);
        extractor.text(Minecraft.getInstance().font, this.getDisplayedFieldName(),
                x, y + LABEL_Y_OFFSET, this.getPreferredTextColor(), false);
        this.button.extractRenderState(extractor, mouseX, mouseY, partialTick);
    }
}
