package com.github.tartaricacid.touhoulittlemaid.compat.kaleidoscopetavern.meal;

import com.github.ysbbbbbb.kaleidoscopetavern.init.ModItems;
import net.minecraft.world.item.ItemStack;

/**
 * Keeps raw grape harvests available for processing and storage instead of
 * spending them on the maid's periodic work-meal ritual. Emergency healing
 * uses a separate meal type and may still consume these foods when needed.
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
