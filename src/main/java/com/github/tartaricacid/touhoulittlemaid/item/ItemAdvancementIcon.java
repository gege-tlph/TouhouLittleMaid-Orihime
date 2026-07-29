package com.github.tartaricacid.touhoulittlemaid.item;

import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ItemAdvancementIcon extends Item {
    public ItemAdvancementIcon(Identifier id) {
        // 1.21.11: Item.getDescriptionId() 现 final，不可覆盖 → 用 Properties.overrideDescription 直接设描述 ID（javap 确认）。
        //   本类注册于 9 个不同 ID（change_chair_model/kill_100…），HEAD 用覆盖强制统一为 advancement_icon → 必须保留统一键，行为等价。
        super((new Properties()).setId(ResourceKey.create(Registries.ITEM, id)).stacksTo(1).overrideDescription("item.touhou_little_maid.advancement_icon"));
    }

    @Override
    public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, TooltipDisplay tooltipDisplay, Consumer<Component> components, TooltipFlag pIsAdvanced) {
        components.accept(Component.translatable("tooltips.touhou_little_maid.advancement_icon.desc").withStyle(ChatFormatting.GRAY));
    }
}