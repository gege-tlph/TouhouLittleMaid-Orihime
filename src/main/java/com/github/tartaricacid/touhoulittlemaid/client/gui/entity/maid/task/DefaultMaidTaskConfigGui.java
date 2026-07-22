package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.task;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.task.TaskConfigContainer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.anti_ad.mc.ipn.api.IPNButton;
import org.anti_ad.mc.ipn.api.IPNGuiHint;
import org.anti_ad.mc.ipn.api.IPNPlayerSideOnly;

@IPNPlayerSideOnly
@IPNGuiHint(button = IPNButton.SORT, horizontalOffset = -36, bottom = -12)
@IPNGuiHint(button = IPNButton.SORT_COLUMNS, horizontalOffset = -24, bottom = -24)
@IPNGuiHint(button = IPNButton.SORT_ROWS, horizontalOffset = -12, bottom = -36)
@IPNGuiHint(button = IPNButton.SHOW_EDITOR, horizontalOffset = -5)
@IPNGuiHint(button = IPNButton.SETTINGS, horizontalOffset = -5)
public class DefaultMaidTaskConfigGui extends MaidTaskConfigGui<TaskConfigContainer> {
    private static final Identifier BG = Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "textures/gui/default_task_config.png");

    public DefaultMaidTaskConfigGui(TaskConfigContainer screenContainer, Inventory inv, Component titleIn) {
        super(screenContainer, inv, titleIn);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int x, int y) {
        super.renderBg(graphics, partialTicks, x, y);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BG, leftPos + 80, topPos + 28, 0F, 0F, imageWidth, 137, 256, 256);
    }

    @Override
    protected void renderAddition(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.drawWordWrap(font, Component.translatable("gui.touhou_little_maid.default_task_config.title"), leftPos + 88, topPos + 38, 160, 0xFF000000 | ChatFormatting.DARK_GRAY.getColor());
    }
}
