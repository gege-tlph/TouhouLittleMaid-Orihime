package com.github.tartaricacid.touhoulittlemaid.inventory.container.backpack;

import com.github.tartaricacid.touhoulittlemaid.entity.backpack.data.FurnaceBackpackData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.MaidItemManager;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.MaidMainContainer;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 熔炉背包的容器。进度条同步走 {@code addDataSlots(ContainerData)}——
 * 原版菜单 data slot 通道（26.1.2 javap 证仍在），与行为基准同一条路。
 */
public class FurnaceBackpackContainer extends MaidMainContainer {
    public static final MenuType<FurnaceBackpackContainer> TYPE = new ExtendedMenuType<>(FurnaceBackpackContainer::new, ByteBufCodecs.INT);
    private final ContainerData data;

    public FurnaceBackpackContainer(int id, Inventory inventory, int entityId) {
        super(TYPE, id, inventory, entityId);
        FurnaceBackpackData furnaceData;
        if (this.getMaid().getBackpackData() instanceof FurnaceBackpackData backpackData) {
            furnaceData = backpackData;
        } else {
            // 兜底：类型与数据短暂不一致时给一份空数据，行为基准同款
            furnaceData = new FurnaceBackpackData(this.getMaid());
        }
        this.data = furnaceData.getDataAccess();
        this.addSlot(new Slot(furnaceData, 0, 161, 101) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                // 26.1：canInsertItem 由 EntityMaid 挪进了 MaidItemManager（manager 拆分）
                return MaidItemManager.canInsertItem(stack);
            }
        });
        this.addSlot(new FurnaceBackpackFuelSlot(this, furnaceData, 1, 161, 142));
        this.addSlot(new FurnaceResultSlot(inventory.player, furnaceData, 2, 221, 121));
        this.addDataSlots(this.data);
    }

    @Override
    protected void addBackpackInv(Inventory inventory) {
        for (int i = 0; i < 6; i++) {
            addSlot(BackpackSlot.create(maid, 6 + i, 143 + 18 * i, 57));
        }
        for (int i = 0; i < 6; i++) {
            addSlot(BackpackSlot.create(maid, 12 + i, 143 + 18 * i, 75));
        }
    }

    private boolean isFuel(ItemStack stack) {
        return this.getMaid().level().fuelValues().isFuel(stack);
    }

    public int getBurnProgress() {
        int cookingProgress = this.data.get(2);
        int cookingTotalTime = this.data.get(3);
        return cookingTotalTime != 0 && cookingProgress != 0 ? cookingProgress * 24 / cookingTotalTime : 0;
    }

    public int getLitProgress() {
        int litDuration = this.data.get(1);
        if (litDuration == 0) {
            litDuration = 200;
        }
        return this.data.get(0) * 13 / litDuration;
    }

    public boolean isLit() {
        return this.data.get(0) > 0;
    }

    public static class FurnaceBackpackFuelSlot extends Slot {
        private final FurnaceBackpackContainer furnaceBackpackContainer;

        public FurnaceBackpackFuelSlot(FurnaceBackpackContainer furnaceBackpackContainer, Container container, int slot, int pX, int pY) {
            super(container, slot, pX, pY);
            this.furnaceBackpackContainer = furnaceBackpackContainer;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.furnaceBackpackContainer.isFuel(stack) || isBucket(stack);
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return isBucket(stack) ? 1 : super.getMaxStackSize(stack);
        }

        public static boolean isBucket(ItemStack stack) {
            return stack.is(Items.BUCKET);
        }
    }
}
