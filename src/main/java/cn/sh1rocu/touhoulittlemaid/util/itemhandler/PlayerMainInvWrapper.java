package cn.sh1rocu.touhoulittlemaid.util.itemhandler;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class PlayerMainInvWrapper extends RangedWrapper {
    private final Inventory inventoryPlayer;

    public PlayerMainInvWrapper(Inventory inv) {
        super(new InvWrapper(inv), 0, inv.getNonEquipmentItems().size());
        inventoryPlayer = inv;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        ItemStack rest = super.insertItem(slot, stack, simulate);
        if (rest.getCount() != stack.getCount()) {
        // 槽位中的物品堆发生变化时，触发对应的物品栏动画。
            ItemStack inSlot = getStackInSlot(slot);
            if (!inSlot.isEmpty()) {
                if (getInventoryPlayer().player.level().isClientSide()) {
                    inSlot.setPopTime(5);
                } else if (getInventoryPlayer().player instanceof ServerPlayer) {
                    getInventoryPlayer().player.containerMenu.broadcastChanges();
                }
            }
        }
        return rest;
    }

    public Inventory getInventoryPlayer() {
        return inventoryPlayer;
    }
}
