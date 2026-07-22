package com.github.tartaricacid.touhoulittlemaid.compat.kaleidoscopetavern.meal;

import com.github.ysbbbbbb.kaleidoscopetavern.init.ModItems;
import net.minecraft.world.item.ItemStack;

/**
 * 保留未采摘的葡萄用于加工和储存，而不是将它们花在女仆定期的工作餐仪式上。紧急治疗使用单独的膳食类型，并且在需要时仍可能食用这些食物。
 */
public final class TavernMealCompat {
    private TavernMealCompat() {
    }

    public static boolean isRawHarvestedGrape(ItemStack stack) {
        return stack.is(ModItems.GRAPE)
                || stack.is(ModItems.ICE_GRAPE)
                || stack.is(ModItems.GOLD_GRAPE)
                || stack.is(ModItems.GREEN_GRAPE);
    }
}
