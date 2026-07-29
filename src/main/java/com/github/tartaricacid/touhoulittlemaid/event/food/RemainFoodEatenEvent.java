package com.github.tartaricacid.touhoulittlemaid.event.food;

import cn.sh1rocu.touhoulittlemaid.util.itemhandler.CombinedInvWrapper;
import cn.sh1rocu.touhoulittlemaid.util.itemhandler.ItemHandlerHelper;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidAfterEatEvent;
import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class RemainFoodEatenEvent {
    private static final Logger LOGGER = LogManager.getLogger(RemainFoodEatenEvent.class);

    public static void onAfterMaidEat(MaidAfterEatEvent event) {
        ItemStack foodAfterEat = event.getFoodAfterEat();
        if (!foodAfterEat.isEmpty()) {
            ItemStack craftingRemainingItem = foodAfterEat.getRecipeRemainder();

            if (craftingRemainingItem.isEmpty()) {
                String itemId = ItemsUtil.getItemId(foodAfterEat.getItem());
                // 上游缺陷（TartaricAcid/TouhouLittleMaid#1139）：配置声明为 List<List<String>>，
                // 但 MaidConfig 用的是无校验器的 builder.define，磁盘上写成扁平 ["minecraft:bowl"]
                // 会在增强 for 的隐式 checkcast 上抛 ClassCastException，写成 [["minecraft:bowl"]]
                // 会在 get(1) 抛 IndexOutOfBoundsException。本方法挂在 MaidAfterEatEvent 上、
                // 跑在服务端 tick 内，一条手写坏的配置就能让 tick 崩。故按 Object 迭代逐项校验形状。
                for (Object entry : ServerRuleConfig.get(MaidConfig.MAID_EATEN_RETURN_CONTAINER_LIST)) {
                    if (!(entry instanceof List<?> pair) || pair.size() < 2
                            || !(pair.get(0) instanceof String foodId) || !(pair.get(1) instanceof String containerId)) {
                        LOGGER.warn("Ignoring malformed MaidEatenReturnContainerList entry {}; expected [item, container]", entry);
                        continue;
                    }
                    if (foodId.equals(itemId)) {
                        craftingRemainingItem = getItemStack(containerId);
                        break;
                    }
                }
            }

            if (!craftingRemainingItem.isEmpty()) {
                EntityMaid maid = event.getMaid();
                CombinedInvWrapper availableInv = maid.getAvailableInv(false);
                ItemStack result = ItemHandlerHelper.insertItemStacked(availableInv, craftingRemainingItem, false);
                // 如果女仆背包满了，掉落在地上
                if (!result.isEmpty()) {
                    ItemEntity itemEntity = new ItemEntity(maid.level, maid.getX(), maid.getY(), maid.getZ(), craftingRemainingItem);
                    maid.level.addFreshEntity(itemEntity);
                }
            }
        }
    }

    private static ItemStack getItemStack(String itemId) {
        // 同 #1139：容器 id 同样来自手改配置，非法字符会让 Identifier.parse 抛异常，
        // 一样能崩掉服务端 tick，故用 tryParse 的等价语义降级为「忽略该项」。
        Identifier resourceLocation = Identifier.tryParse(itemId);
        if (resourceLocation == null) {
            LOGGER.warn("Ignoring malformed MaidEatenReturnContainerList container id {}", itemId);
            return ItemStack.EMPTY;
        }
        // B6b: 1.21.11 BuiltInRegistries.ITEM.get(id) 返回 Optional<Holder.Reference<Item>>（javap 确认）
        return BuiltInRegistries.ITEM.get(resourceLocation)
                .map(ref -> new ItemStack(ref.value()))
                .orElse(ItemStack.EMPTY);
    }
}
