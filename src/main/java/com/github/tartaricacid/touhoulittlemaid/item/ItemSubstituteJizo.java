package com.github.tartaricacid.touhoulittlemaid.item;

import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import com.github.tartaricacid.touhoulittlemaid.api.event.InteractMaidEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nullable;
import java.util.List;

public class ItemSubstituteJizo extends Item {
    public ItemSubstituteJizo(Identifier id) {
        super((new Properties()).setId(ResourceKey.create(Registries.ITEM, id)).stacksTo(1).rarity(Rarity.EPIC));
    }

    public static void onEntityInteract(InteractMaidEvent event) {
        EntityMaid maid = event.getMaid();
        ItemStack stack = event.getStack();
        Player player = event.getPlayer();
        if (maid.isOwnedBy(player) && stack.getItem() == InitItems.SUBSTITUTE_JIZO && !maid.getIsInvulnerable()) {
            maid.setEntityInvulnerable(true);
            stack.shrink(1);
            event.setCanceled(true);
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void appendHoverText(ItemStack stack, Item.TooltipContext worldIn, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flagIn){
        tooltip.accept(Component.translatable("tooltips.touhou_little_maid.substitute_jizo.desc").withStyle(ChatFormatting.GRAY));
    }
}
