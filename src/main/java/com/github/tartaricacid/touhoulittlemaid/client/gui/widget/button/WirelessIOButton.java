package com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.function.BiConsumer;

public class WirelessIOButton extends TouhouStateSwitchButton {
    protected final ITooltip onTooltip;
    private final BiConsumer<Double, Double> onClick;

    public WirelessIOButton(int xIn, int yIn, int widthIn, int heightIn, boolean triggered, BiConsumer<Double, Double> onClick, ITooltip onTooltip) {
        super(xIn, yIn, widthIn, heightIn, triggered);
        this.onClick = onClick;
        this.onTooltip = onTooltip;
    }

    @Override
    // 1.21.11: onClick(double,double) → onClick(MouseButtonEvent, boolean)
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        this.isStateTriggered = !this.isStateTriggered;
        onClick.accept(event.x(), event.y());
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderWidget(graphics, mouseX, mouseY, partialTicks);
        if (this.isHovered()) {
            this.onTooltip.onTooltip(graphics, mouseX, mouseY);
        }
    }

    @Environment(EnvType.CLIENT)
    public interface ITooltip {
        void onTooltip(GuiGraphics graphics, int x, int y);
    }
}
