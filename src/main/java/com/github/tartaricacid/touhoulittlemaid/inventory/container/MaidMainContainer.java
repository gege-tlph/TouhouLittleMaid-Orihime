package com.github.tartaricacid.touhoulittlemaid.inventory.container;

import cn.sh1rocu.touhoulittlemaid.util.transfer.IndexModifier;
import cn.sh1rocu.touhoulittlemaid.util.transfer.ItemStacksResourceHandler;
import cn.sh1rocu.touhoulittlemaid.util.transfer.ResourceHandler;
import cn.sh1rocu.touhoulittlemaid.util.transfer.ResourceHandlerSlot;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import com.github.tartaricacid.touhoulittlemaid.api.backpack.ITriggerSlotChange;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidBackpackChangeEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitCapabilities;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
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

import javax.annotation.Nullable;

import static net.minecraft.world.inventory.InventoryMenu.*;


public abstract class MaidMainContainer extends AbstractMaidContainer {
    protected static final int PLAYER_INVENTORY_SIZE = 36;
    protected static final Identifier EMPTY_MAINHAND_SLOT = Identifier.parse("container/slot/sword");
    protected static final Identifier EMPTY_BACK_SHOW_SLOT = IdentifierUtil.modLoc("container/slot/back_show");
    protected static final Identifier[] TEXTURE_EMPTY_SLOTS = new Identifier[]{EMPTY_ARMOR_SLOT_BOOTS, EMPTY_ARMOR_SLOT_LEGGINGS, EMPTY_ARMOR_SLOT_CHESTPLATE, EMPTY_ARMOR_SLOT_HELMET};
    protected static final EquipmentSlot[] SLOT_IDS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /**
     * 储物区（默认背包 + 背包本体）在 {@code slots} 里的起点。
     *
     * <p>装备槽与手持槽排在它之前，而<b>手持槽几乎什么都收</b>：它用的是
     * {@link ResourceHandlerSlot} 的默认 {@code mayPlace}，转交
     * {@code LivingEntityEquipmentWrapper.isValid}，女仆分支只问
     * {@code MaidItemManager.canInsertItem}——即背包黑名单加
     * {@code canFitInsideContainerItems()}，此外一律放行。
     * 于是按注册顺序搬运时它们总是先被填满。见 {@link #quickMoveStack}。</p>
     *
     * <p>装备槽反而不受影响：它在上面那层之外<b>自己覆写了</b> {@code mayPlace}，
     * 要求 {@code getEquipmentSlotForItem} 恰好等于该槽。</p>
     */
    private int storageStart = PLAYER_INVENTORY_SIZE;

    public MaidMainContainer(MenuType<?> type, int id, Inventory inventory, int entityId) {
        super(type, id, inventory, entityId);
        if (maid != null) {
            this.addMaidArmorInv();
            this.addMaidHandInv();
            // 记下「储物区」的起点：它之前是装备槽与手持槽，之后全是可以随便放东西的地方。
            // 快捷移动要优先落在储物区，见 quickMoveStack。
            this.storageStart = this.slots.size();
            this.addMainDefaultInv();
            this.addBackpackInv(inventory);
        }
    }

    protected void addMaidHandInv() {
        ResourceHandler<ItemVariant> capability = InitCapabilities.HAND_ITEM.find(maid, Direction.DOWN);
        if (capability != null) {
            var indexModifier = ItemsUtil.createIndexModifier(capability);

            addSlot(new ResourceHandlerSlot(capability, indexModifier, 0, 87, 77) {
                @Override
                public Identifier getNoItemIcon() {
                    return EMPTY_MAINHAND_SLOT;
                }
            });
            addSlot(new ResourceHandlerSlot(capability, indexModifier, 1, 121, 77) {
                @Override
                public Identifier getNoItemIcon() {
                    return EMPTY_ARMOR_SLOT_SHIELD;
                }
            });
        }

    }

    protected void addMaidArmorInv() {
        ResourceHandler<ItemVariant> capability = InitCapabilities.ARMOR_ITEM.find(maid, Direction.DOWN);
        if (capability != null) {
            var indexModifier = ItemsUtil.createIndexModifier(capability);

            for (int i = 0; i < 2; ++i) {
                for (int j = 0; j < 2; j++) {
                    final EquipmentSlot equipmentSlot = SLOT_IDS[2 * i + j];
                    addSlot(new ResourceHandlerSlot(capability, indexModifier, 3 - 2 * i - j, 94 + 20 * j, 37 + 20 * i) {
                        @Override
                        public int getMaxStackSize() {
                            return 1;
                        }

                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return maid.getEquipmentSlotForItem(stack) == equipmentSlot && stack.getItem().canFitInsideContainerItems();
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
            addSlot(BackpackSlot.create(maid, i, 143 + 18 * i, 37));
            // 最后一格给予特殊图标
            if (i == 5) {
                ItemStacksResourceHandler maidInv = maid.getMaidInv();
                addSlot(new BackpackSlot(maid, maidInv::set, i, 143 + 18 * i, 37) {
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
                // 先试储物区（默认背包 + 背包本体），装不下才轮到装备与手持。
                //
                // ⚠️ 成因不是「手持槽优先级高」，而是**手持槽的准入判据几乎不挡东西**：
                // 它们用 ResourceHandlerSlot 的默认 mayPlace，最终只问 canInsertItem
                // （黑名单 + canFitInsideContainerItems），而搬运是按槽位注册顺序试的，
                // 它们又恰好排在储物区之前。玩家用 IPN 之类的整理工具按住 Shift/Alt
                // 批量搬运时，东西就全进了主副手。
                //
                // 这里只调整**落点顺序**，不加准入限制——手动拖放仍然可以往手持槽里放任何东西，
                // 那是玩家的明确意图；快捷移动则是「随便找个地方放」，该落在储物区。
                boolean moved = this.moveItemStackTo(stack2, this.storageStart, this.slots.size(), false)
                        || this.moveItemStackTo(stack2, PLAYER_INVENTORY_SIZE, this.storageStart, false);
                if (!moved) {
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
        }
        return stack1;
    }

    public static class BackpackSlot extends ResourceHandlerSlot implements ITriggerSlotChange {
        private final EntityMaid maid;

        private BackpackSlot(EntityMaid maid, IndexModifier<ItemVariant> slotModifier, int index, int xPosition, int yPosition) {
            super(maid.getMaidInv(), slotModifier, index, xPosition, yPosition);
            this.maid = maid;
        }

        public static BackpackSlot create(EntityMaid maid, int index, int xPosition, int yPosition) {
            ItemStacksResourceHandler maidInv = maid.getMaidInv();
            return new BackpackSlot(maid, maidInv::set, index, xPosition, yPosition);
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
