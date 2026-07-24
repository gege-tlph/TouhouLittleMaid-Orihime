package com.github.tartaricacid.touhoulittlemaid.client.animation.gecko.condition;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;

import net.fabricmc.fabric.api.tag.convention.v1.ConventionalItemTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ThrowablePotionItem;

public class InnerClassify {
    private static final String EMPTY = "";

    public static String doClassifyTest(String extraPre, EntityMaid maid, InteractionHand hand) {
        ItemStack itemInHand = maid.getItemInHand(hand);
        String classify = getClassify(itemInHand);
        if (!classify.equals(EMPTY)) {
            return extraPre + classify;
        }
        return EMPTY;
    }

    /**
     * mojang 在新版本越来越倾向于用组件机制替代继承，故武器类型判断放弃继承判断，尽可能采用 tag 进行判断
     */
    public static String getClassify(ItemStack itemInHand) {
        Item item = itemInHand.getItem();

        if (item instanceof MaceItem) {
            return "mace";
        }
        if (item instanceof ThrowablePotionItem) {
            return "throwable_potion";
        }
        if (itemInHand.is(ItemTags.SWORDS)) {
            return "sword";
        }
        if (itemInHand.is(ItemTags.AXES)) {
            return "axe";
        }
        if (itemInHand.is(ItemTags.PICKAXES)) {
            return "pickaxe";
        }
        if (itemInHand.is(ItemTags.SHOVELS)) {
            return "shovel";
        }
        if (itemInHand.is(ItemTags.HOES)) {
            return "hoe";
        }
        if (itemInHand.is(ConventionalItemTags.SHIELDS)) {
            return "shield";
        }
        if (itemInHand.is(ItemTags.CROSSBOW_ENCHANTABLE)) {
            return "crossbow";
        }
        if (itemInHand.is(ConventionalItemTags.BOWS)) {
            return "bow";
        }
        if (itemInHand.is(ItemTags.FISHING_ENCHANTABLE)) {
            return "fishing_rod";
        }
        // 因为在更新矛之前，mojang 把三叉戟命名为此名称，故为了保证兼容性，仍然使用此名称
        if (itemInHand.is(ConventionalItemTags.SPEARS)) {
            return "spear";
        }
        // 因为命名冲突问题，故将其修改为 lance 骑枪
        // 矛没有具体的物品类，仅能通过 tag 区分
        if (itemInHand.is(ItemTags.SPEARS)) {
            return "lance";
        }
        return EMPTY;
    }
}
