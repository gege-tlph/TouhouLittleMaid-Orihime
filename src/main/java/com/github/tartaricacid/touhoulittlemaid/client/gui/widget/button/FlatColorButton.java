package com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button;

import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
    /**
     * 文字颜色覆写，{@code 0} 表示不覆写（沿用 active 白 / 禁用灰）。
     *
     * <p>0 当哨兵是安全的：它是「全透明黑」，没有任何调用方会真想要那个值。
     * 传进来的值<b>必须带 alpha</b>——26.1.2 的 {@code Font} 不再把 alpha=0 补成不透明。</p>
     */
    private int messageColorOverride = 0;

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

    /** 去掉悬停提示。空列表不行——那会画出一个空提示框 */
    public FlatColorButton clearTooltips() {
        this.tooltips = null;
        return this;
    }

    /**
     * @param argb 带 alpha 的文字颜色；{@code 0} 恢复默认取色
     */
    public FlatColorButton setMessageColor(int argb) {
        this.messageColorOverride = argb;
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

    public void renderToolTip(GuiGraphicsExtractor graphics, Screen screen, int pMouseX, int pMouseY) {
        if (this.isHovered && tooltips != null) {
            graphics.setTooltipForNextFrame(Screens.getMinecraft(screen).font, tooltips, Optional.empty(), pMouseX, pMouseY);
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float pPartialTick) {
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
        //int i = getFGColor();
        int i = this.active ? 16777215 : 10526880;
        int color = this.messageColorOverride != 0
                ? this.messageColorOverride
                : i | Mth.ceil(this.alpha * 255.0F) << 24;
        this.renderString(graphics, minecraft.font, color);
    }

    /**
     * ⚠️ 用形参 {@code pColor}，不是宿主原先硬编码的奶白 {@code 0xFFF3EFE0}——与行为基准
     * {@code port/1.21.11-fabric} 一致。两个后果都是有意的：<b>禁用态文字变灰</b>
     * （试听冷却、无改动时的保存按钮靠它才有可读信号），以及<b>激活态由奶白变纯白</b>
     * （{@code active ? 16777215 : 10526880}）。后者波及全树所有 {@code FlatColorButton}。
     */
    public void renderString(GuiGraphicsExtractor graphics, Font font, int pColor) {
        if (this.textScale == 1.0f && this.textOffsetX == 0.0f && this.textOffsetY == 0.0f) {
            graphics.centeredText(font, this.getMessage(), this.getX() + this.width / 2,
                    this.getY() + (this.height - 8) / 2, pColor);
            return;
        }

        float centerX = this.getX() + this.width / 2.0f;
        float topY = this.getY() + (this.height - 8.0f * this.textScale) / 2.0f;
        graphics.pose().pushMatrix();
        graphics.pose().translate(this.textOffsetX, this.textOffsetY);
        graphics.pose().scale(this.textScale, this.textScale);
        graphics.centeredText(font, this.getMessage(), Math.round(centerX / this.textScale),
                Math.round(topY / this.textScale), pColor);
        graphics.pose().popMatrix();
    }

    public void setSelect(boolean select) {
        isSelect = select;
    }
}
