package com.github.tartaricacid.touhoulittlemaid.client.event;

import com.github.tartaricacid.touhoulittlemaid.init.registry.CompatRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.*;

import java.net.URI;

public class PlayerLoggedInNotice {
    private static boolean notFirst = false;

    public static void onEnterGame(Minecraft client) {
        boolean missingPatchouli = !FabricLoader.getInstance().isModLoaded(CompatRegistry.PATCHOULI);
        if (notFirst || client.player == null) {
            return;
        }
        if (missingPatchouli) {
            MutableComponent title = Component.translatable("message.touhou_little_maid.missing_patchouli.title")
                    .withStyle(style -> style.withColor(ChatFormatting.GREEN).withBold(true));
            ClickEvent clickEvent = new ClickEvent.OpenUrl(URI.create(I18n.get("message.touhou_little_maid.missing_patchouli.url")));
            HoverEvent hoverEvent = new HoverEvent.ShowText(Component.translatable("message.touhou_little_maid.missing_patchouli.url"));
            MutableComponent base = Component.translatable("message.touhou_little_maid.missing_patchouli.click_here")
                    .withStyle(style -> style.withColor(ChatFormatting.GOLD).withBold(false).withUnderlined(true).withClickEvent(clickEvent).withHoverEvent(hoverEvent));
            client.player.displayClientMessage(title.append(CommonComponents.SPACE).append(base), false);
        }
        notFirst = true;
    }
}
