package com.github.tartaricacid.touhoulittlemaid.api.backpack;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * 实现该接口的 SlotItemHandler 类会在槽位变化时触发饰品或者背包 takeoff 事件
 * <p>
 * Shift 点击转移物品时，槽位回调触发前原物品可能已经被清空，无法取得变化前的数据，
 * 因此由该接口额外保存并传递槽位变更信息。
 */
public interface ITriggerSlotChange {
    /**
     * 当玩家 Shift 点击物品从槽位中取出时触发
     *
     * @param player 触发事件的玩家，可能为 null
     * @param stack  被取出的物品
     */
    void onShiftTakeoff(@Nullable Player player, ItemStack stack);
}
