package com.github.tartaricacid.touhoulittlemaid.inventory.container.backpack;

import com.github.tartaricacid.touhoulittlemaid.inventory.container.MaidMainContainer;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.inventory.Slot;

/**
 * 末影箱背包的容器：槽位直接挂玩家自己的末影箱，而不是女仆背包。
 *
 * <p>所以这里用的是原版 {@link Slot} 而非本仓库的 {@code BackpackSlot}——
 * 容器内容不属于女仆，女仆死亡/脱下时也不该掉落。</p>
 */
public class EnderChestBackpackContainer extends MaidMainContainer {
    public static final MenuType<EnderChestBackpackContainer> TYPE = new ExtendedMenuType<>(
            EnderChestBackpackContainer::new, ByteBufCodecs.INT);

    public EnderChestBackpackContainer(int id, Inventory inventory, int entityId) {
        super(TYPE, id, inventory, entityId);
    }

    @Override
    protected void addBackpackInv(Inventory inventory) {
        PlayerEnderChestContainer enderChestContainer = inventory.player.getEnderChestInventory();
        for (int i = 0; i < 6; i++) {
            addSlot(new Slot(enderChestContainer, i, 143 + 18 * i, 61));
        }
        for (int j = 0; j < 3; j++) {
            for (int i = 0; i < 5; i++) {
                addSlot(new Slot(enderChestContainer, 6 + 5 * j + i, 161 + 18 * i, 79 + j * 18));
            }
        }
        for (int i = 0; i < 6; i++) {
            addSlot(new Slot(enderChestContainer, 21 + i, 143 + 18 * i, 133));
        }
    }
}
