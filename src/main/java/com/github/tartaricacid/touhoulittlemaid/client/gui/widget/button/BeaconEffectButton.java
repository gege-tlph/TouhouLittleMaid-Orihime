package com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.network.message.SetBeaconPotionPackage;
import com.github.tartaricacid.touhoulittlemaid.tileentity.TileEntityMaidBeacon;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;

public class BeaconEffectButton extends TouhouStateSwitchButton {
    private static final Identifier BG = Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "textures/gui/maid_beacon.png");
    // 1.21.11: MobEffectTextureManager（TextureAtlasSprite）已移除 → gui sprite Identifier（Gui.getMobEffectSprite，同一 mob_effect/* 贴图）
    private final Identifier sprite;
    private final Component tooltips;
    private final int potionIndex;
    private final BlockPos pos;
    private final Consumer<Boolean> onClick;

    public BeaconEffectButton(TileEntityMaidBeacon.BeaconEffect effect, int xIn, int yIn, int potionIndex, TileEntityMaidBeacon beacon, Consumer<Boolean> onClick) {
        super(xIn, yIn, 22, 22, potionIndex == effect.ordinal());
        this.initTextureValues(0, 111, 22, 22, BG);
        this.sprite = Gui.getMobEffectSprite(effect.getEffect());
        this.tooltips = effect.getEffect().value().getDisplayName();
        this.potionIndex = effect.ordinal();
        this.pos = beacon.getBlockPos();
        this.onClick = onClick;
    }

    @Override
    // 1.21.11: onClick(double,double) → onClick(MouseButtonEvent, boolean)
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        this.isStateTriggered = !this.isStateTriggered;
        ClientPlayNetworking.send(new SetBeaconPotionPackage(pos, isStateTriggered ? potionIndex : -1));
        this.onClick.accept(this.isStateTriggered);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderWidget(graphics, mouseX, mouseY, partialTicks);
        // 1.21.11: blit(x,y,z,w,h,TextureAtlasSprite) 移除 → blitSprite（z 丢弃，管线接管）
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.sprite, this.getX() + 2, this.getY() + 2, 18, 18);
    }

    public void renderToolTip(GuiGraphics graphics, Screen screen, int pMouseX, int pMouseY) {
        if (this.isHovered) {
            // 1.21.11: renderTooltip 移除 → setTooltipForNextFrame（Component 重载）
            graphics.setTooltipForNextFrame(Screens.getClient(screen).font, tooltips, pMouseX, pMouseY);
        }
    }
}