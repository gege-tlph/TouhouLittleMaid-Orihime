package com.github.tartaricacid.touhoulittlemaid.item;

import com.github.tartaricacid.touhoulittlemaid.datagen.tag.TagItem;
import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

public class ItemDamageableBauble extends Item {
    public ItemDamageableBauble(Identifier id, int durability) {
        super((new Properties()).setId(ResourceKey.create(Registries.ITEM, id)).durability(durability)/*setNoRepair()*/);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

//    @Override
//    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
//        if (enchantment == Enchantments.MENDING && stack.is(TagItem.MAID_MENDING_BLOCKLIST_ITEM)) {
//            return false;
//        }
//        if (enchantment == Enchantments.VANISHING_CURSE && stack.is(TagItem.MAID_VANISHING_BLOCKLIST_ITEM)) {
//            return false;
//        }
//        return super.supportsEnchantment(stack, enchantment);
//    }

    @Override
    public boolean canBeEnchantedWith(ItemStack stack, Holder<Enchantment> enchantment, EnchantingContext context) {
        // B4 恢复：修补/消失诅咒黑名单（TagItem un-excluded）。
        //   1.21.11 API 变更（javap 确认）：Enchantments.X 现为 ResourceKey，enchantment 为 Holder →
        //   HEAD 的 `enchantment == Enchantments.X` 不再成立，改用 `enchantment.is(Enchantments.X)`。
        if (enchantment.is(Enchantments.MENDING) && stack.is(TagItem.MAID_MENDING_BLOCKLIST_ITEM)) {
            return false;
        }
        if (enchantment.is(Enchantments.VANISHING_CURSE) && stack.is(TagItem.MAID_VANISHING_BLOCKLIST_ITEM)) {
            return false;
        }
        return super.canBeEnchantedWith(stack, enchantment, context);
    }
}
