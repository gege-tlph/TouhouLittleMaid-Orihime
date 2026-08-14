package com.github.tartaricacid.touhoulittlemaid.inventory.container.backpack;

import com.github.tartaricacid.touhoulittlemaid.entity.backpack.data.TankBackpackData;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.MaidMainContainer;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 液体背包的容器。储罐量的同步走两条路（行为基准同款）：
 * data slot 载低位 int，{@code SyncFluidAmountPackage} 载精确 long（开 GUI 与变化时发）。
 */
public class TankBackpackContainer extends MaidMainContainer {
    public static final MenuType<TankBackpackContainer> TYPE = new ExtendedMenuType<>(TankBackpackContainer::new, ByteBufCodecs.INT);
    private static final Identifier INPUT_SLOT = IdentifierUtil.modLoc("container/slot/tank_input");
    private static final Identifier OUTPUT_SLOT = IdentifierUtil.modLoc("container/slot/tank_output");
    private final ContainerData data;
    private long clientFluidCount;

    public TankBackpackContainer(int id, Inventory inventory, int entityId) {
        super(TYPE, id, inventory, entityId);
        TankBackpackData tankData;
        if (this.getMaid().getBackpackData() instanceof TankBackpackData backpackData) {
            tankData = backpackData;
        } else {
            // 兜底：类型与数据短暂不一致时给一份空数据，行为基准同款
            tankData = new TankBackpackData(this.getMaid());
        }
        this.data = tankData.getDataAccess();
        this.clientFluidCount = data.get(0);
        this.addSlot(new TankInputSlot(tankData, 0, 161, 101));
        this.addSlot(new TankOutputSlot(tankData, 1, 161, 140));
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

    public int getFluidCount() {
        return this.data.get(0);
    }

    @Environment(EnvType.CLIENT)
    public long getClientFluidCount() {
        return this.clientFluidCount;
    }

    @Environment(EnvType.CLIENT)
    public void setClientFluidCount(long amount) {
        this.clientFluidCount = amount;
    }

    public static class TankInputSlot extends Slot {
        public TankInputSlot(Container container, int slot, int pX, int pY) {
            super(container, slot, pX, pY);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return ContainerItemContext.withConstant(stack).find(FluidStorage.ITEM) != null;
        }

        @Override
        @Environment(EnvType.CLIENT)
        public Identifier getNoItemIcon() {
            return INPUT_SLOT;
        }
    }

    public static class TankOutputSlot extends Slot {
        public TankOutputSlot(Container container, int slot, int pX, int pY) {
            super(container, slot, pX, pY);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return ContainerItemContext.withConstant(stack).find(FluidStorage.ITEM) != null;
        }

        @Override
        @Environment(EnvType.CLIENT)
        public Identifier getNoItemIcon() {
            return OUTPUT_SLOT;
        }
    }
}
