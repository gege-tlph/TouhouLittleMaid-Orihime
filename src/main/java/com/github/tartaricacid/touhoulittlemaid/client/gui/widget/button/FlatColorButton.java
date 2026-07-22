package com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button;

import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Collections;
import java.util.List;
import java.util.Optional;


public class FlatColorButton extends Button {
    private List<Component> tooltips;
    protected boolean isSelect = false;
    private float textScale = 1.0f;
    private float textOffsetX = 0.0f;
    private float textOffsetY = 0.0f;

    public FlatColorButton(int pX, int pY, int pWidth, int pHeight, Component pMessage, OnPress pOnPress) {
        super(pX, pY, pWidth, pHeight, pMessage, pOnPress, DEFAULT_NARRATION);
    }

    public FlatColorButton setTooltips(String key) {
        tooltips = Collections.singletonList(Component.translatable(key));
        return this;
    }

    public FlatColorButton setTooltips(List<Component> tooltips) {
        this.tooltips = tooltips;
        return this;
    }

    public FlatColorButton setTextScale(float textScale) {
        return this.setTextTransform(textScale, 0.0f, 0.0f);
    }

    public FlatColorButton setTextTransform(float textScale, float textOffsetX, float textOffsetY) {
        this.textScale = Mth.clamp(textScale, 0.5f, 2.0f);
        this.textOffsetX = textOffsetX;
        this.textOffsetY = textOffsetY;
        return this;
    }

    public void renderToolTip(GuiGraphics graphics, Screen screen, int pMouseX, int pMouseY) {
        if (this.isHovered && tooltips != null) {

            graphics.setTooltipForNextFrame(Screens.getClient(screen).font, tooltips, Optional.empty(), pMouseX, pMouseY);
        }
    }

    @Override

    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float pPartialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (isSelect) {
            graphics.fillGradient(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xff_1E90FF, 0xff_1E90FF);
        } else {
            graphics.fillGradient(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xff_434242, 0xff_434242);
        }
        if (this.isHoveredOrFocused()) {
            graphics.fillGradient(this.getX(), this.getY() + 1, this.getX() + 1, this.getY() + this.height - 1, 0xff_F3EFE0, 0xff_F3EFE0);
            graphics.fillGradient(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, 0xff_F3EFE0, 0xff_F3EFE0);
            graphics.fillGradient(this.getX() + this.width - 1, this.getY() + 1, this.getX() + this.width, this.getY() + this.height - 1, 0xff_F3EFE0, 0xff_F3EFE0);
            graphics.fillGradient(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, 0xff_F3EFE0, 0xff_F3EFE0);
        }

        int i = this.active ? 16777215 : 10526880;
        this.renderString(graphics, minecraft.font, i | Mth.ceil(this.alpha * 255.0F) << 24);
    }


    public void renderString(GuiGraphics graphics, Font font, int pColor) {
        if (this.textScale == 1.0f && this.textOffsetX == 0.0f && this.textOffsetY == 0.0f) {
            graphics.drawCenteredString(font, this.getMessage(), this.getX() + this.width / 2,
                    this.getY() + (this.height - 8) / 2, pColor);
            return;
        }

        float centerX = this.getX() + this.width / 2.0f;
        float topY = this.getY() + (this.height - 8.0f * this.textScale) / 2.0f;
        graphics.pose().pushMatrix();
        graphics.pose().translate(this.textOffsetX, this.textOffsetY);
        graphics.pose().scale(this.textScale, this.textScale);
        graphics.drawCenteredString(font, this.getMessage(), Math.round(centerX / this.textScale),
                Math.round(topY / this.textScale), pColor);
        graphics.pose().popMatrix();
    }

    public void setSelect(boolean select) {
        isSelect = select;
    }
}
