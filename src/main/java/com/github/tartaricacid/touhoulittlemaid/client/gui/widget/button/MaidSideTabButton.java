package com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.client.gui.ITooltipButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

/**
 * 女仆界面侧边栏按钮
 */
public class MaidSideTabButton extends Button implements ITooltipButton {
    private static final Identifier SIDE = Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "textures/gui/maid_gui_side.png");
    private static final int V_OFFSET = 107;
    private final List<Component> tooltips;
    private final int top;

    public MaidSideTabButton(int x, int y, int top, OnPress onPressIn, List<Component> tooltips) {
        //super(Button.builder(Component.empty(), onPressIn).pos(x, y).size(26, 24));
        super(x, y, 26, 24, Component.empty(), onPressIn, Button.DEFAULT_NARRATION);
        this.top = V_OFFSET + top;
        this.tooltips = tooltips;
    }

    @Override
    // 1.21.11: renderWidget 现为 final → 覆写 renderContents；RenderSystem.enableDepthTest 移除（管线接管）
    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (!this.active) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, SIDE, this.getX() + 2, this.getY(), 209F, top, this.width, this.height, 256, 256);
        }
        // 193, 111
        graphics.blit(RenderPipelines.GUI_TEXTURED, SIDE, this.getX() + 6, this.getY() + 4, 193F, top + 4, 16, 16, 256, 256);
    }

    @Override
    public boolean isTooltipHovered() {
        return this.isHovered();
    }

    @Override
    public void renderTooltip(GuiGraphics graphics, Minecraft mc, int mouseX, int mouseY) {
        // 1.21.11: renderComponentTooltip 移除 → setTooltipForNextFrame（延迟到帧末渲染）
        graphics.setTooltipForNextFrame(mc.font, this.tooltips, Optional.empty(), mouseX, mouseY);
    }
}
