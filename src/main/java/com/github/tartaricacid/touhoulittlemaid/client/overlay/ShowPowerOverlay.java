package com.github.tartaricacid.touhoulittlemaid.client.overlay;

import com.github.tartaricacid.touhoulittlemaid.data.PowerAttachment;
import com.github.tartaricacid.touhoulittlemaid.init.InitDataAttachment;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import com.github.tartaricacid.touhoulittlemaid.item.ItemHakureiGohei;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
// TODO: 等待 1.21.11 版本依赖
// import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ShowPowerOverlay {
    private static ItemStack POWER_POINT;

    public static final ShowPowerOverlay INSTANCE = new ShowPowerOverlay();

    // TODO: 等待 1.21.11 版本依赖
    // @Override
    public void render(@NotNull GuiGraphics guiGraphics, @NotNull DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Options options = minecraft.options;
        Player player = minecraft.player;
        if (player == null || options.hideGui) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!ItemHakureiGohei.isGohei(stack)) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        if (POWER_POINT == null) {
            POWER_POINT = InitItems.POWER_POINT.getDefaultInstance();
        }
        guiGraphics.renderItem(POWER_POINT, 5, 5);
        PowerAttachment cap = player.getAttachedOrCreate(InitDataAttachment.POWER_NUM, () -> new PowerAttachment(0));
        guiGraphics.drawString(font, String.format("%s×%.2f", ChatFormatting.BOLD, cap.get()), 20, 10, 0xFFFFFFFF);
    }
}
