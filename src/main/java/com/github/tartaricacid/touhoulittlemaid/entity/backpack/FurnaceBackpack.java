package com.github.tartaricacid.touhoulittlemaid.entity.backpack;

import com.github.tartaricacid.touhoulittlemaid.api.backpack.IBackpackData;
import com.github.tartaricacid.touhoulittlemaid.api.backpack.IMaidBackpack;
import com.github.tartaricacid.touhoulittlemaid.api.backpack.MaidBackpackRenderData;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.backpack.FurnaceBackpackRenderData;
import com.github.tartaricacid.touhoulittlemaid.entity.backpack.data.FurnaceBackpackData;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityTombstone;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.AbstractMaidContainer;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.backpack.FurnaceBackpackContainer;
import com.github.tartaricacid.touhoulittlemaid.item.BackpackLevel;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import cn.sh1rocu.touhoulittlemaid.util.transfer.VanillaContainerWrapper;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * 熔炉背包：女仆随身带一个熔炉。烧炼状态在 {@link FurnaceBackpackData}，
 * 由 {@code MaidBackpackManager} 经 {@code BackpackStateData} 附件持有并驱动。
 */
public class FurnaceBackpack extends IMaidBackpack {
    public static final Identifier ID = IdentifierUtil.modLoc("furnace_backpack");

    @Override
    public void onTakeOff(ItemStack stack, Player player, EntityMaid maid) {
        // 熔炉三格（原料/燃料/产物）随背包卸下一并丢出；写法对新基——
        // 基准的 Forge InvWrapper 在 26.1 已换成 util/transfer 的资源句柄，
        // VanillaContainerWrapper.of 把 Container 包成 ResourceHandler（即 SlottedStorage）
        IBackpackData backpackData = maid.getBackpackData();
        if (backpackData instanceof FurnaceBackpackData furnaceBackpackData) {
            ItemsUtil.dropEntityItems(maid, VanillaContainerWrapper.of(furnaceBackpackData), 0, null);
        }
        dropRelativeItems(stack, maid);
    }

    @Override
    public void onSpawnTombstone(EntityMaid maid, EntityTombstone tombstone) {
        // 死亡时熔炉三格进墓碑，不落地
        IBackpackData backpackData = maid.getBackpackData();
        if (backpackData instanceof FurnaceBackpackData furnaceBackpackData) {
            for (int i = 0; i < furnaceBackpackData.getContainerSize(); i++) {
                ItemStack stack = furnaceBackpackData.removeItemNoUpdate(i);
                if (!stack.isEmpty()) {
                    tombstone.insertItem(stack);
                }
            }
        }
    }

    @Override
    public boolean hasBackpackData() {
        return true;
    }

    @Nullable
    @Override
    public IBackpackData getBackpackData(EntityMaid maid) {
        return new FurnaceBackpackData(maid);
    }

    @Override
    public int getAvailableMaxContainerIndex() {
        return BackpackLevel.FURNACE_CAPACITY;
    }

    @Override
    public MaidBackpackRenderData getRenderData() {
        return new FurnaceBackpackRenderData();
    }

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public Item getItem() {
        return InitItems.FURNACE_BACKPACK;
    }

    @Override
    public MenuProvider getGuiProvider(int entityId) {
        return new ExtendedMenuProvider<Integer>() {
            @Override
            public Integer getScreenOpeningData(ServerPlayer player) {
                return entityId;
            }

            @Override
            public Component getDisplayName() {
                return Component.literal("Maid Furnace Container");
            }

            @Override
            public AbstractMaidContainer createMenu(int index, Inventory playerInventory, Player player) {
                return new FurnaceBackpackContainer(index, playerInventory, entityId);
            }

            @Override
            public boolean shouldCloseCurrentScreen() {
                return false;
            }
        };
    }
}
