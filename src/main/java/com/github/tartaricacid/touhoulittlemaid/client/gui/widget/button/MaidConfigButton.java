package com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

public class MaidConfigButton extends Button {
    private static final Identifier ICON = Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "textures/gui/maid_gui_button.png");
    private final OnPress leftPress;
    private final OnPress rightPress;
    private boolean leftClicked = false;
    private Component value;

    public MaidConfigButton(int x, int y, Component title, Component value, OnPress onLeftPressIn, OnPress onRightPressIn) {
/*        super(Button.builder(title, b -> {
        }).pos(x, y).size(164, 13));*/
        super(x, y, 164, 13, title, b -> {
        }, Button.DEFAULT_NARRATION);
        this.leftPress = onLeftPressIn;
        this.rightPress = onRightPressIn;
        this.value = value;
    }

    public MaidConfigButton(int x, int y, Component title, Component value, OnPress onPress) {
        this(x, y, title, value, onPress, onPress);
    }

    @Override
    // 1.21.11: renderWidget 现为 final → 覆写 renderContents；RenderSystem.enableDepthTest 移除（管线接管）
    protected void renderContents(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (this.isHovered) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, ICON, this.getX(), this.getY(), 63F, 141F, this.width, this.height, 256, 256);
        } else {
            graphics.blit(RenderPipelines.GUI_TEXTURED, ICON, this.getX(), this.getY(), 63F, 128F, this.width, this.height, 256, 256);
        }
        graphics.drawString(mc.font, this.getMessage(), this.getX() + 5, this.getY() + 3, 0xFF444444, false);
        drawCenteredStringWithoutShadow(graphics, mc.font, this.value, this.getX() + 142, this.getY() + 3, 0xFF000000 | ChatFormatting.GREEN.getColor());
    }

    public void setValue(Component value) {
        this.value = value;
    }

    // 1.21.11: AbstractWidget.clicked(double,double) 钩子已移除 → 降级为普通方法，由下方 mouseClicked 复刻 origin 判定链
    protected boolean clicked(double mouseX, double mouseY) {
        if (!this.active || !this.visible) {
            return false;
        }
        boolean leftClickX = (this.getX() + 120) <= mouseX && mouseX <= (this.getX() + 130);
        boolean rightClickX = (this.getX() + 154) <= mouseX && mouseX <= (this.getX() + 164);
        boolean clickY = this.getY() <= mouseY && mouseY <= (this.getY() + this.getHeight());
        if (leftClickX && clickY) {
            leftClicked = true;
            return true;
        }
        if (rightClickX && clickY) {
            leftClicked = false;
            return true;
        }
        return false;
    }

    @Override
    // 1.21.11: 复刻 1.21.1 AbstractWidget.mouseClicked 流程（isValidClickButton → clicked → playDownSound → onClick），
    // 保持 origin 行为：仅左右两个热区可点击，点击按钮其余区域不消费事件
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.isValidClickButton(event.buttonInfo()) && this.clicked(event.x(), event.y())) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            this.onClick(event, doubleClick);
            return true;
        }
        return false;
    }

    @Override
    // 1.21.11: onPress() → onPress(InputWithModifiers)
    public void onPress(InputWithModifiers input) {
        if (leftClicked) {
            leftPress.onPress(this);
        } else {
            rightPress.onPress(this);
        }
    }

    public void drawCenteredStringWithoutShadow(GuiGraphics graphics, Font pFont, Component pText, int pX, int pY, int pColor) {
        FormattedCharSequence formattedcharsequence = pText.getVisualOrderText();
        graphics.drawString(pFont, formattedcharsequence, pX - pFont.width(formattedcharsequence) / 2, pY, pColor, false);
    }

    @Environment(EnvType.CLIENT)
    public interface OnPress {
        void onPress(MaidConfigButton button);
    }
}
