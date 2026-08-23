package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.backpack;

import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.AbstractMaidContainerGui;
import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button.BaubleButton;
import com.github.tartaricacid.touhoulittlemaid.compat.curios.CuriosCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.backpack.EnderChestBackpackContainer;
import com.github.tartaricacid.touhoulittlemaid.util.GuiTools;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
public class EnderChestBackpackContainerScreen extends AbstractMaidContainerGui<EnderChestBackpackContainer>
        implements IBackpackContainerScreen {
    private static final Identifier BACKPACK = IdentifierUtil.modLoc("textures/gui/maid_ender_chest.png");
    private final EntityMaid maid;

    public EnderChestBackpackContainerScreen(EnderChestBackpackContainer container, Inventory inv, Component titleIn) {
        // 26.1 把画面尺寸挪进了父类构造器（基准是构造后再设 imageWidth/Height）
        super(container, inv, titleIn, 256, 256);
        this.maid = menu.getMaid();
    }

    @Override
    protected void initAdditionWidgets() {
        BaubleButton button = this.getBaubleButton(maid, leftPos, topPos);
        this.addRenderableWidget(button);

        if (CuriosCompat.isLoadedOrEnable()) {
            this.addRenderableWidget(this.getCuriosButton(maid, leftPos, topPos));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // 26.1：renderBg(GuiGraphics,…) → extractBackground(GuiGraphicsExtractor,…)，且改用 GuiTools.guiBlit
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        GuiTools.guiBlit(graphics, BACKPACK, leftPos + 85, topPos + 36, 0, 0, 165, 128);
    }
}
