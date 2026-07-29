package com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class ButtonWithId extends Button {
    private final Consumer<Integer> onClick;
    private final int id;

    public ButtonWithId(int id, int x, int y, int width, int height, Component title, Consumer<Integer> onClick) {
/*        super(Button.builder(title, (b) -> {
        }).pos(x, y).size(width, height));*/
        super(x, y, width, height, title, b -> {
        }, Button.DEFAULT_NARRATION);
        this.id = id;
        this.onClick = onClick;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        super.onPress(input);
        this.onClick.accept(this.id);
    }

    // 1.21.11: Button 现为 abstract，子类须实现 renderContents（默认按钮=sprite+label）
    @Override
    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderDefaultSprite(graphics);
        this.renderDefaultLabel(graphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE));
    }
}
