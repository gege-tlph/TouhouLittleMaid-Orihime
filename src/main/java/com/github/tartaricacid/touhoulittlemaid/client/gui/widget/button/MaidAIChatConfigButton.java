package com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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

public class MaidAIChatConfigButton extends Button {
    private static final Identifier ICON = Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "textures/gui/maid_ai_chat_config.png");
    private final MaidAIChatConfigButton.OnPress leftPress;
    private final MaidAIChatConfigButton.OnPress rightPress;
    private boolean leftClicked = false;
    private Component value;

    public MaidAIChatConfigButton(int x, int y, Component title, Component value, MaidAIChatConfigButton.OnPress onLeftPressIn, MaidAIChatConfigButton.OnPress onRightPressIn) {

        super(x, y, 164, 13, title, b -> {
        }, Button.DEFAULT_NARRATION);
        this.leftPress = onLeftPressIn;
        this.rightPress = onRightPressIn;
        this.value = value;
    }

    public MaidAIChatConfigButton(int x, int y, Component title, Component value, MaidAIChatConfigButton.OnPress onPress) {
        this(x, y, title, value, onPress, onPress);
    }

    @Override

    protected void renderContents(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (this.isHovered) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, ICON, this.getX(), this.getY(), 6F, 150F, this.width, this.height, 256, 256);
        } else {
            graphics.blit(RenderPipelines.GUI_TEXTURED, ICON, this.getX(), this.getY(), 6F, 137F, this.width, this.height, 256, 256);
        }
        drawButtonText(graphics, mc.font);
    }

    public void setValue(Component value) {
        this.value = value;
    }


    protected boolean clicked(double mouseX, double mouseY) {
        if (!this.active || !this.visible) {
            return false;
        }
        boolean leftClickX = (this.getX() + 62) <= mouseX && mouseX <= (this.getX() + 72);
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

    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.isValidClickButton(event.buttonInfo()) && this.clicked(event.x(), event.y())) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            this.onClick(event, doubleClick);
            return true;
        }
        return false;
    }

    @Override

    public void onPress(InputWithModifiers input) {
        if (leftClicked) {
            leftPress.onPress(this);
        } else {
            rightPress.onPress(this);
        }
    }

    public void drawButtonText(GuiGraphics graphics, Font font) {
        float scale = 0.75f;

        FormattedCharSequence leftText = this.getMessage().getVisualOrderText();
        FormattedCharSequence rightText = this.value.getVisualOrderText();

        float leftTextX = (this.getX() + 5) / scale;
        float leftTextY = (this.getY() + 4) / scale;
        float rightTextX = (this.getX() + 113 - font.width(rightText) * scale / 2f) / scale;
        float rightTextY = (this.getY() + 4) / scale;

        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);
        graphics.drawString(font, leftText, (int) leftTextX, (int) leftTextY, 0xFF444444, false);
        graphics.drawString(font, rightText, (int) rightTextX, (int) rightTextY, 0xFF55FF55, false);
        graphics.pose().popMatrix();
    }

    @Environment(EnvType.CLIENT)
    public interface OnPress {
        void onPress(MaidAIChatConfigButton button);
    }
}
