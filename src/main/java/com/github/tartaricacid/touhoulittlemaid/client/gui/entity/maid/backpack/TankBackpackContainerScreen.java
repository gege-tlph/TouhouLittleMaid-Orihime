package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.backpack;

import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.AbstractMaidContainerGui;
import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button.BaubleButton;
import com.github.tartaricacid.touhoulittlemaid.compat.curios.CuriosCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.backpack.data.TankBackpackData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.backpack.TankBackpackContainer;
import com.github.tartaricacid.touhoulittlemaid.util.GuiTools;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import com.github.tartaricacid.touhoulittlemaid.util.MaidFluidRender;
import com.google.common.collect.Lists;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.anti_ad.mc.ipn.api.IPNButton;
import org.anti_ad.mc.ipn.api.IPNGuiHint;
import org.anti_ad.mc.ipn.api.IPNPlayerSideOnly;

import java.util.Optional;

@IPNPlayerSideOnly
@IPNGuiHint(button = IPNButton.SORT, horizontalOffset = -36, bottom = -12)
@IPNGuiHint(button = IPNButton.SORT_COLUMNS, horizontalOffset = -24, bottom = -24)
@IPNGuiHint(button = IPNButton.SORT_ROWS, horizontalOffset = -12, bottom = -36)
@IPNGuiHint(button = IPNButton.SHOW_EDITOR, horizontalOffset = -5)
@IPNGuiHint(button = IPNButton.SETTINGS, horizontalOffset = -5)
public class TankBackpackContainerScreen extends AbstractMaidContainerGui<TankBackpackContainer>
        implements IBackpackContainerScreen {
    private static final Identifier BACKPACK = IdentifierUtil.modLoc("textures/gui/maid_tank.png");
    private final EntityMaid maid;

    public TankBackpackContainerScreen(TankBackpackContainer container, Inventory inv, Component titleIn) {
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
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        GuiTools.guiBlit(graphics, BACKPACK, leftPos + 85, topPos + 36, 0, 0, 165, 128);

        // 先画流体、再叠储罐刻度玻璃罩，uv 与行为基准逐字相同
        MaidFluidRender.drawFluid(graphics, leftPos + 200, topPos + 108, 29, 50, maid.getBackpackFluid(), this.menu.getClientFluidCount(), TankBackpackData.CAPACITY);
        GuiTools.guiBlit(graphics, BACKPACK, leftPos + 197, topPos + 104, 165, 0, 34, 50);

        boolean xInRange = leftPos + 196 <= mouseX && mouseX <= leftPos + 196 + 29;
        boolean yInRange = topPos + 108 <= mouseY && mouseY <= topPos + 108 + 50;
        if (xInRange && yInRange) {
            // Fabric 单位（droplet）转 mB：/81
            MutableComponent fluidInfo = Component.translatable("tooltips.touhou_little_maid.tank_backpack.fluid",
                    MaidFluidRender.getFluidName(maid.getBackpackFluid(), this.menu.getClientFluidCount() / 81),
                    this.menu.getClientFluidCount() / 81).withStyle(ChatFormatting.GRAY);
            MutableComponent capacityInfo = Component.translatable("tooltips.touhou_little_maid.tank_backpack.capacity", TankBackpackData.CAPACITY / 81)
                    .withStyle(ChatFormatting.GRAY);
            graphics.setTooltipForNextFrame(font, Lists.newArrayList(fluidInfo, capacityInfo), Optional.empty(), mouseX, mouseY);
        }
    }
}
