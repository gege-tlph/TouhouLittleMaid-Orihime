package com.github.tartaricacid.touhoulittlemaid.client.gui.item;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button.TouhouImageButton;
import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button.WirelessIOButton;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.other.WirelessIOContainer;
import com.github.tartaricacid.touhoulittlemaid.item.ItemWirelessIO;
import com.github.tartaricacid.touhoulittlemaid.network.message.WirelessIOGuiPackage;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.anti_ad.mc.ipn.api.IPNIgnore;

@IPNIgnore
public class WirelessIOContainerGui extends AbstractContainerScreen<WirelessIOContainer> {
    private static final Identifier MAIN = Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "textures/gui/wireless_io.png");
    private boolean isMaidToChest;
    private boolean isBlacklist;

    public WirelessIOContainerGui(WirelessIOContainer container, Inventory inv, Component titleIn) {
        super(container, inv, titleIn);
        this.isMaidToChest = ItemWirelessIO.isMaidToChest(container.getWirelessIO());
        this.isBlacklist = ItemWirelessIO.isBlacklist(container.getWirelessIO());
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        WirelessIOButton ioModeToggle = new WirelessIOButton(leftPos + 23, topPos + 34, 18, 18, isMaidToChest,
                (x, y) -> {
                    isMaidToChest = !isMaidToChest;
                    ClientPlayNetworking.send(new WirelessIOGuiPackage(isMaidToChest, isBlacklist));
                }, (m, x, y) -> m.setTooltipForNextFrame(font, Component.translatable("gui.touhou_little_maid.wireless_io.io_mode"), x, y));

        ioModeToggle.initTextureValues(194, 32, -18, 18, MAIN);
        WirelessIOButton filterModeToggle = new WirelessIOButton(leftPos + 136, topPos + 26, 16, 16, isBlacklist,
                (x, y) -> {
                    isBlacklist = !isBlacklist;
                    ClientPlayNetworking.send(new WirelessIOGuiPackage(isMaidToChest, isBlacklist));
                }, (m, x, y) -> m.setTooltipForNextFrame(font, Component.translatable("gui.touhou_little_maid.wireless_io.filter_mode"), x, y));
        filterModeToggle.initTextureValues(176, 0, 16, 16, MAIN);

        TouhouImageButton configButton = new TouhouImageButton(leftPos + 136, topPos + 44, 16, 16, 208, 0, 16,
                MAIN, 256, 256, buttons -> this.minecraft.setScreen(new WirelessIOConfigSlotGui(menu.getWirelessIO())));
        configButton.setTooltip(Tooltip.create(Component.translatable("gui.touhou_little_maid.wireless_io.config_slot")));

        addRenderableWidget(filterModeToggle);
        addRenderableWidget(configButton);
        addRenderableWidget(ioModeToggle);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int x, int y) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, MAIN, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        if (isBlacklist) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, MAIN, leftPos + 61, topPos + 15, 0F, 166F, 54, 55, 256, 256);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int x, int y) {
    }
}
