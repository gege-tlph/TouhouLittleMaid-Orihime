package com.github.tartaricacid.touhoulittlemaid.entity.backpack;

import com.github.tartaricacid.touhoulittlemaid.api.backpack.IMaidBackpack;
import com.github.tartaricacid.touhoulittlemaid.api.backpack.MaidBackpackRenderData;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.backpack.EnderChestBackpackRenderData;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityTombstone;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.AbstractMaidContainer;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.backpack.EnderChestBackpackContainer;
import com.github.tartaricacid.touhoulittlemaid.item.BackpackLevel;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 末影箱背包：女仆随身携带玩家的末影箱。
 *
 * <p>穿脱都不掉落物品——箱内容物属于玩家的末影箱而不是女仆背包，
 * 所以 {@code onPutOn} / {@code onTakeOff} / {@code onSpawnTombstone} 三个钩子都是空的，
 * 且可用槽位上限取 {@link BackpackLevel#EMPTY_CAPACITY}（女仆自己的格子一个都不开）。
 * 这三处的「空」是行为，不是没写完。</p>
 *
 * <p>写法对新基：26.1 的菜单用 {@code ExtendedMenuProvider}（基准是
 * {@code ExtendedScreenHandlerFactory}），渲染三件套挪进了 {@link MaidBackpackRenderData}
 * （基准还是 {@code getBackpackModel/Texture/offset} 三个方法）。</p>
 */
public class EnderChestBackpack extends IMaidBackpack {
    public static final Identifier ID = IdentifierUtil.modLoc("ender_chest_backpack");

    @Override
    public void onPutOn(ItemStack stack, Player player, EntityMaid maid) {
    }

    @Override
    public void onTakeOff(ItemStack stack, Player player, EntityMaid maid) {
    }

    @Override
    public void onSpawnTombstone(EntityMaid maid, EntityTombstone tombstone) {
    }

    @Override
    public int getAvailableMaxContainerIndex() {
        return BackpackLevel.EMPTY_CAPACITY;
    }

    @Override
    public MaidBackpackRenderData getRenderData() {
        return new EnderChestBackpackRenderData();
    }

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public Item getItem() {
        return InitItems.ENDER_CHEST_BACKPACK;
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
                return Component.literal("Maid Ender Chest Container");
            }

            @Override
            public AbstractMaidContainer createMenu(int index, Inventory playerInventory, Player player) {
                return new EnderChestBackpackContainer(index, playerInventory, entityId);
            }

            @Override
            public boolean shouldCloseCurrentScreen() {
                return false;
            }
        };
    }
}
