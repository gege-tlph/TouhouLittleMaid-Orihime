package com.github.tartaricacid.touhoulittlemaid.event.food;

import cn.sh1rocu.touhoulittlemaid.util.itemhandler.CombinedInvWrapper;
import cn.sh1rocu.touhoulittlemaid.util.itemhandler.ItemHandlerHelper;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidAfterEatEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class ConvertFoodEatenEvent {
    public static void onAfterMaidEat(MaidAfterEatEvent event) {
        ItemStack foodAfterEat = event.getFoodAfterEat();
        EntityMaid maid = event.getMaid();

        net.minecraft.world.item.component.UseRemainder useRemainder = foodAfterEat.get(DataComponents.USE_REMAINDER);
        if (!foodAfterEat.isEmpty() && useRemainder != null && !useRemainder.convertInto().isEmpty()) {
            ItemStack convertedStack = useRemainder.convertInto();
            {
                CombinedInvWrapper availableInv = maid.getAvailableInv(false);
                ItemStack result = ItemHandlerHelper.insertItemStacked(availableInv, convertedStack.copy(), false);
                // 如果女仆背包满了，掉落在地上
                if (!result.isEmpty()) {
                    ItemEntity itemEntity = new ItemEntity(maid.level, maid.getX(), maid.getY(), maid.getZ(), convertedStack.copy());
                    maid.level.addFreshEntity(itemEntity);
                }
            }
        }
    }
}
