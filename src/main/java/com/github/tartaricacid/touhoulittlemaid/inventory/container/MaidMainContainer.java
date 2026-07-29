package com.github.tartaricacid.touhoulittlemaid.inventory.container;

import cn.sh1rocu.touhoulittlemaid.util.itemhandler.IItemHandler;
import cn.sh1rocu.touhoulittlemaid.util.itemhandler.SlotItemHandler;
import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.backpack.ITriggerSlotChange;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidBackpackChangeEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitCapabilities;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.world.inventory.InventoryMenu.*;


public abstract class MaidMainContainer extends AbstractMaidContainer {
    protected static final int PLAYER_INVENTORY_SIZE = 36;
    protected static final Identifier EMPTY_MAINHAND_SLOT = Identifier.withDefaultNamespace("container/slot/sword");
    protected static final Identifier EMPTY_BACK_SHOW_SLOT = Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "container/slot/back_show");
    protected static final Identifier[] TEXTURE_EMPTY_SLOTS = new Identifier[]{EMPTY_ARMOR_SLOT_BOOTS, EMPTY_ARMOR_SLOT_LEGGINGS, EMPTY_ARMOR_SLOT_CHESTPLATE, EMPTY_ARMOR_SLOT_HELMET};
    protected static final EquipmentSlot[] SLOT_IDS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public MaidMainContainer(MenuType<?> type, int id, Inventory inventory, int entityId) {
        super(type, id, inventory, entityId);
        if (maid != null) {
            this.addMaidArmorInv();
            this.addMaidHandInv();
            this.addMainDefaultInv();
            this.addBackpackInv(inventory);
        }
    }

    protected void addMaidHandInv() {
        IItemHandler handler = InitCapabilities.HAND_ITEM.find(maid, Direction.DOWN);
        if (handler == null) {
            return;
        }
        addSlot(new SlotItemHandler(handler, 0, 87, 77) {
            @Override
            public Identifier getNoItemIcon() {
                return EMPTY_MAINHAND_SLOT;
            }
        });
        addSlot(new SlotItemHandler(handler, 1, 121, 77) {
            @Override
            public Identifier getNoItemIcon() {
                return EMPTY_ARMOR_SLOT_SHIELD;
            }
        });
    }

    protected void addMaidArmorInv() {
        IItemHandler handler = InitCapabilities.ARMOR_ITEM.find(maid, Direction.DOWN);
        if (handler != null) {
            for (int i = 0; i < 2; ++i) {
                for (int j = 0; j < 2; j++) {
                    final EquipmentSlot equipmentSlot = SLOT_IDS[2 * i + j];
                    addSlot(new SlotItemHandler(handler, 3 - 2 * i - j, 94 + 20 * j, 37 + 20 * i) {
                        @Override
                        public int getMaxStackSize() {
                            return 1;
                        }

                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return maid != null && maid.getEquipmentSlotForItem(stack) == equipmentSlot && stack.getItem().canFitInsideContainerItems();
                        }

                        @Override
                        public boolean mayPickup(Player playerIn) {
                            ItemStack itemstack = this.getItem();
                            boolean curseEnchant = !itemstack.isEmpty() && !playerIn.isCreative()
                                    && EnchantmentHelper.has(itemstack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE);
                            return !curseEnchant && super.mayPickup(playerIn);
                        }

                        @Override
                        public Identifier getNoItemIcon() {
                            return TEXTURE_EMPTY_SLOTS[equipmentSlot.getIndex()];
                        }
                    });
                }
            }
        }
    }

    protected void addMainDefaultInv() {
        // 默认背包
        for (int i = 0; i < 6; i++) {
            addSlot(new BackpackSlot(maid, i, 143 + 18 * i, 37));
            // 最后一格给予特殊图标
            if (i == 5) {
                addSlot(new BackpackSlot(maid, i, 143 + 18 * i, 37) {
                    @Override
                    public Identifier getNoItemIcon() {
                        return EMPTY_BACK_SHOW_SLOT;
                    }
                });
            }
        }
    }

    protected abstract void addBackpackInv(Inventory inventory);

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack stack1 = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack2 = slot.getItem();
            stack1 = stack2.copy();

            if (index < PLAYER_INVENTORY_SIZE) {
                if (!this.moveItemStackTo(stack2, PLAYER_INVENTORY_SIZE, this.slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack2, 0, PLAYER_INVENTORY_SIZE, true)) {
                return ItemStack.EMPTY;
            }

            if (stack2.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack2.getCount() == stack1.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack2);
            // 触发 Shift 点击取出事件
            if (slot instanceof ITriggerSlotChange slotChange) {
                slotChange.onShiftTakeoff(player, stack1);
            }

            // 【有意的行为变更 / 1.21.11】原先此处调用 maid.setLastArmorItem / setLastHandItem，
            // 注释写明是「用来修正护甲值不变化的问题」—— 属 1.21.1 时代的 workaround。
            // 1.21.11 重写了装备系统（引入 EntityEquipment），lastEquipmentItems 已收为 LivingEntity
            // 的私有字段，**无任何公开途径可写**（javap 确认：setLastArmorItem/setLastHandItem 均已移除，
            // 仅剩 private Map<EquipmentSlot, ItemStack> lastEquipmentItems）。
            // 上游 origin/26.1（同代重写）亦已完全移除这两处调用 —— 判定为原 bug 已由装备系统重写解决。
            // 若 P7 实测发现护甲值仍不刷新，则需改用 accessor mixin 写入 lastEquipmentItems。
        }
        return stack1;
    }

    public static class BackpackSlot extends SlotItemHandler implements ITriggerSlotChange {
        private final EntityMaid maid;

        public BackpackSlot(EntityMaid maid, int index, int xPosition, int yPosition) {
            super(maid.getMaidInv(), index, xPosition, yPosition);
            this.maid = maid;
        }

        @Override
        public void onShiftTakeoff(@Nullable Player player, ItemStack stack) {
            if (!maid.level.isClientSide() && !stack.isEmpty()) {
                MaidBackpackChangeEvent.TAKE_OFF.invoker().takeOff(new MaidBackpackChangeEvent.TakeOff(maid, stack));
            }
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            super.onTake(player, stack);
            this.onShiftTakeoff(player, stack);
        }

        @Override
        public void setByPlayer(ItemStack stack) {
            super.setByPlayer(stack);
            if (!maid.level.isClientSide() && !stack.isEmpty()) {
                MaidBackpackChangeEvent.PUT_ON.invoker().putOn(new MaidBackpackChangeEvent.PutOn(maid, stack));
            }
        }
    }
}
