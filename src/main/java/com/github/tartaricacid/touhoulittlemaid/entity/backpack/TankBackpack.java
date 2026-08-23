package com.github.tartaricacid.touhoulittlemaid.entity.backpack;

import cn.sh1rocu.touhoulittlemaid.util.transfer.VanillaContainerWrapper;
import com.github.tartaricacid.touhoulittlemaid.api.backpack.IBackpackData;
import com.github.tartaricacid.touhoulittlemaid.api.backpack.IMaidBackpack;
import com.github.tartaricacid.touhoulittlemaid.api.backpack.MaidBackpackRenderData;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.backpack.TankBackpackRenderData;
import com.github.tartaricacid.touhoulittlemaid.entity.backpack.data.TankBackpackData;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityTombstone;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.AbstractMaidContainer;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.backpack.TankBackpackContainer;
import com.github.tartaricacid.touhoulittlemaid.item.BackpackLevel;
import com.github.tartaricacid.touhoulittlemaid.item.ItemTankBackpack;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
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
 * 液体背包：女仆随身带一个 10 桶储罐。储罐内容随物品走——
 * 卸下时经 {@link #getTakeOffItemStack} 写进物品，穿上时经 {@code onPutOn} 读回来
 * （行为基准同款；注册 id 是 {@code tank}，照抄不改）。
 */
public class TankBackpack extends IMaidBackpack {
    public static final Identifier ID = IdentifierUtil.modLoc("tank");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public Item getItem() {
        return InitItems.TANK_BACKPACK;
    }

    @Override
    public void onPutOn(ItemStack stack, Player player, EntityMaid maid) {
        IBackpackData backpackData = maid.getBackpackData();
        if (backpackData instanceof TankBackpackData tankBackpackData) {
            ItemTankBackpack.setTankBackpack(maid, tankBackpackData, stack);
        }
    }

    @Override
    public void onTakeOff(ItemStack stack, Player player, EntityMaid maid) {
        IBackpackData backpackData = maid.getBackpackData();
        if (backpackData instanceof TankBackpackData tankBackpackData) {
            ItemsUtil.dropEntityItems(maid, VanillaContainerWrapper.of(tankBackpackData), 0, null);
        }
        dropRelativeItems(stack, maid);
    }

    @Override
    public ItemStack getTakeOffItemStack(ItemStack stack, @Nullable Player player, EntityMaid maid) {
        IBackpackData backpackData = maid.getBackpackData();
        if (backpackData instanceof TankBackpackData tankBackpackData) {
            return ItemTankBackpack.getTankBackpack(maid.registryAccess(), tankBackpackData);
        }
        return super.getTakeOffItemStack(stack, player, maid);
    }

    @Override
    public void onSpawnTombstone(EntityMaid maid, EntityTombstone tombstone) {
        IBackpackData backpackData = maid.getBackpackData();
        if (backpackData instanceof TankBackpackData tankBackpackData) {
            for (int i = 0; i < tankBackpackData.getContainerSize(); i++) {
                ItemStack stack = tankBackpackData.removeItemNoUpdate(i);
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
        return new TankBackpackData(maid);
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
                return Component.literal("Maid Tank Container");
            }

            @Override
            public AbstractMaidContainer createMenu(int index, Inventory playerInventory, Player player) {
                return new TankBackpackContainer(index, playerInventory, entityId);
            }

            @Override
            public boolean shouldCloseCurrentScreen() {
                return false;
            }
        };
    }

    @Override
    public int getAvailableMaxContainerIndex() {
        return BackpackLevel.TANK_CAPACITY;
    }

    @Override
    public MaidBackpackRenderData getRenderData() {
        return new TankBackpackRenderData();
    }
}
