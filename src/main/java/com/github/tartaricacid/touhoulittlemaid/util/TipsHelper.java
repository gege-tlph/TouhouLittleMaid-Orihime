package com.github.tartaricacid.touhoulittlemaid.util;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.List;

public final class TipsHelper {
    private static final Identifier BUTTON = Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "textures/gui/maid_gui_button.png");
    // 1.21.9+ GUI 重写后 drawString 需显式 alpha，故补 0xFF（颜色值与 HEAD 的 0xFFFF55 相同）
    private static final int TEXT_COLOR = 0xFF_FF_FF_55;

    public static void renderTips(GuiGraphics graphics, Button button, Component text) {
        // 空文本检查前置：HEAD 在 pushPose 之后才 return，会漏掉 popPose（原有缺陷）。
        // 新的 nextStratum() 无需配对，前置后该缺陷自然消失。
        if (text.equals(Component.empty())) {
            return;
        }
        // 1.21.9+ Blaze3D 重写：GuiGraphics.pose() 现在返回 2D 的 Matrix3x2fStack，没有 z 轴，
        // 故 pushPose()+translate(0,0,450)+popPose() 这种「抬高 z 以置顶」的手法已不可用。
        // 新的深度分层原语是 nextStratum()（vanilla AbstractContainerScreen 亦如此用）。
        graphics.nextStratum();

        int xOffset = button.getX() + button.getWidth() - 8;
        int yOffset = button.getY() - 12;
        long number = (System.currentTimeMillis() / 400) % 2;
        if (number == 1) {
            yOffset += 1;
        }

        Font font = Minecraft.getInstance().font;
        List<FormattedCharSequence> split = font.split(text, 124);
        int size = split.size();

        if (size == 1) {
            int textWidth = Mth.clamp(font.width(text) + 26, 40, 150);
            blitButton(graphics, xOffset, yOffset, 0, 128, 10, 20);
            int times = (textWidth - 20) / 20;
            int startX = xOffset + 10;
            for (int i = 0; i < times; i++) {
                blitButton(graphics, startX, yOffset, 10, 128, 20, 20);
                startX = startX + 20;
            }
            int last = textWidth - times * 20 - 20;
            blitButton(graphics, startX, yOffset, 10, 128, last, 20);
            blitButton(graphics, xOffset + textWidth - 10, yOffset, 30, 128, 10, 20);
            blitButton(graphics, xOffset + textWidth - 20, yOffset - 2, 42, 128, 16, 16);
            graphics.drawString(font, text, xOffset + 5, yOffset + 4, TEXT_COLOR, false);
        }

        if (size == 2) {
            yOffset = yOffset - 10;
            int textWidth = font.width(split.get(0)) + 26;
            blitButton(graphics, xOffset, yOffset, 0, 149, 10, 30);
            int times = (textWidth - 20) / 20;
            int startX = xOffset + 10;
            for (int i = 0; i < times; i++) {
                blitButton(graphics, startX, yOffset, 10, 149, 20, 30);
                startX = startX + 20;
            }
            int last = textWidth - times * 20 - 20;
            blitButton(graphics, startX, yOffset, 10, 149, last, 30);
            blitButton(graphics, xOffset + textWidth - 10, yOffset, 30, 149, 10, 30);
            blitButton(graphics, xOffset + textWidth - 20, yOffset + 5, 42, 128, 16, 16);
            graphics.drawString(font, split.get(0), xOffset + 5, yOffset + 4, TEXT_COLOR, false);
            graphics.drawString(font, split.get(1), xOffset + 5, yOffset + 14, TEXT_COLOR, false);
        }

        if (size >= 3) {
            yOffset = yOffset - 20;
            int textWidth = font.width(split.get(0)) + 26;
            blitButton(graphics, xOffset, yOffset, 0, 180, 10, 40);
            int times = (textWidth - 20) / 20;
            int startX = xOffset + 10;
            for (int i = 0; i < times; i++) {
                blitButton(graphics, startX, yOffset, 10, 180, 20, 40);
                startX = startX + 20;
            }
            int last = textWidth - times * 20 - 20;
            blitButton(graphics, startX, yOffset, 10, 180, last, 40);
            blitButton(graphics, xOffset + textWidth - 10, yOffset, 30, 180, 10, 40);
            blitButton(graphics, xOffset + textWidth - 20, yOffset + 10, 42, 128, 16, 16);
            graphics.drawString(font, split.get(0), xOffset + 5, yOffset + 4, TEXT_COLOR, false);
            graphics.drawString(font, split.get(1), xOffset + 5, yOffset + 14, TEXT_COLOR, false);
            graphics.drawString(font, split.get(2), xOffset + 5, yOffset + 24, TEXT_COLOR, false);
        }
    }

    /**
     * 1.21.9+：blit 首参改为 RenderPipeline，且纹理尺寸须显式给出
     * （旧的 7 参便捷重载隐含 256x256）。RenderSystem.enableDepthTest() 已移除 ——
     * 深度状态现由 RenderPipeline 声明式描述，故原先每次 blit 前的手动调用一并删除。
     * 结构参考 origin/26.1，但其 GuiGraphicsExtractor / graphics.text() 是 MC 26.1.2 专有，
     * 1.21.11 不存在，故沿用 HEAD 的 GuiGraphics / drawString。
     */
    private static void blitButton(GuiGraphics graphics, int x, int y, int u, int v, int w, int h) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, BUTTON, x, y, (float) u, (float) v, w, h, 256, 256);
    }
}
