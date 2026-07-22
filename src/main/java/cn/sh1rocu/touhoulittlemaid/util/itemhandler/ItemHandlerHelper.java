package cn.sh1rocu.touhoulittlemaid.util.itemhandler;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemHandlerHelper {
    public static ItemStack insertItem(IItemHandler dest, ItemStack stack, boolean simulate) {
        if (dest == null || stack.isEmpty())
            return stack;

        for (int i = 0; i < dest.getSlots(); i++) {
            stack = dest.insertItem(i, stack, simulate);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }

        return stack;
    }

    public static ItemStack insertItemStacked(IItemHandler inventory, ItemStack stack, boolean simulate) {
        if (inventory == null || stack.isEmpty())
            return stack;

        // 不可堆叠的物品直接尝试放入空槽位。
        if (!stack.isStackable()) {
            return insertItem(inventory, stack, simulate);
        }

        int sizeInventory = inventory.getSlots();

        // 优先合并到物品栏中已有的同类物品堆。
        for (int i = 0; i < sizeInventory; i++) {
            ItemStack slot = inventory.getStackInSlot(i);
            if (ItemStack.isSameItemSameComponents(slot, stack)) {
                stack = inventory.insertItem(i, stack, simulate);

                if (stack.isEmpty()) {
                    break;
                }
            }
        }

        // 将剩余物品放入空槽位。
        if (!stack.isEmpty()) {
            // 查找空槽位。
            for (int i = 0; i < sizeInventory; i++) {
                if (inventory.getStackInSlot(i).isEmpty()) {
                    stack = inventory.insertItem(i, stack, simulate);
                    if (stack.isEmpty()) {
                        break;
                    }
                }
            }
        }

        return stack;
    }

    public static void giveItemToPlayer(Player player, ItemStack stack) {
        giveItemToPlayer(player, stack, -1);
    }

    public static void giveItemToPlayer(Player player, ItemStack stack, int preferredSlot) {
        if (stack.isEmpty()) return;

        IItemHandler inventory = new PlayerMainInvWrapper(player.getInventory());
        Level level = player.level();

        // 尝试将物品交给玩家。
        ItemStack remainder = stack;
        // 优先放入指定槽位。
        if (preferredSlot >= 0 && preferredSlot < inventory.getSlots()) {
            remainder = inventory.insertItem(preferredSlot, stack, false);
        }
        // 剩余部分再放入普通物品栏槽位。
        if (!remainder.isEmpty()) {
            remainder = insertItemStacked(inventory, remainder, false);
        }

        // 只要有物品成功进入物品栏，就播放拾取音效。
        if (remainder.isEmpty() || remainder.getCount() != stack.getCount()) {
            level.playSound(null, player.getX(), player.getY() + 0.5, player.getZ(),
                    SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, ((level.random.nextFloat() - level.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
        }

        // 服务端将无法放入物品栏的剩余物品生成到玩家附近。
        if (!remainder.isEmpty() && !level.isClientSide()) {
            ItemEntity entityitem = new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), remainder);
            entityitem.setPickUpDelay(40);
            entityitem.setDeltaMovement(entityitem.getDeltaMovement().multiply(0, 1, 0));

            level.addFreshEntity(entityitem);
        }
    }
}
