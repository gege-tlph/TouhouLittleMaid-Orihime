package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.backpack;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.AbstractMaidContainerGui;
import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button.BaubleButton;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.backpack.FurnaceBackpackContainer;
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
public class FurnaceBackpackContainerScreen extends AbstractMaidContainerGui<FurnaceBackpackContainer> implements IBackpackContainerScreen {
    private static final Identifier BACKPACK = Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "textures/gui/maid_furnace.png");

    public FurnaceBackpackContainerScreen(FurnaceBackpackContainer container, Inventory inv, Component titleIn) {
        super(container, inv, titleIn);
        this.imageHeight = 256;
        this.imageWidth = 256;
    }

    @Override
    protected void initAdditionWidgets() {
        BaubleButton button = this.getBaubleButton(maid, leftPos, topPos);
        this.addRenderableWidget(button);

    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int x, int y) {
        super.renderBg(graphics, partialTicks, x, y);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKPACK, leftPos + 85, topPos + 36, (float) 0, (float) 0, 165, 128, 256, 256);
        if (this.menu.isLit()) {
            int litProgress = this.menu.getLitProgress();
            graphics.blit(RenderPipelines.GUI_TEXTURED, BACKPACK, leftPos + 161, topPos + 122 + 12 - litProgress, (float) 165, (float) (12 - litProgress), 14, litProgress + 1, 256, 256);
        }
        int burnProgress = this.menu.getBurnProgress();
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKPACK, leftPos + 184, topPos + 120, (float) 165, (float) 14, burnProgress + 1, 16, 256, 256);
    }
}
