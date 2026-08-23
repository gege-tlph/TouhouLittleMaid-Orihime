package com.github.tartaricacid.touhoulittlemaid.gametest;

import cn.sh1rocu.touhoulittlemaid.util.transfer.ItemStacksResourceHandler;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.backpack.EmptyBackpackContainer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 快捷移动（Shift/Alt 搬运）的落点顺序。
 *
 * <p>契约有<b>两个动词</b>，两个都得验，只验一半会把错误修法判成通过：
 * ① 从玩家侧快捷移动，东西该先落<b>储物区</b>，不该塞进主手/副手；
 * ② 储物区装满时仍要<b>回落</b>到手持槽——不能变成「搬不动」；
 * ③ 手动拖放<b>不受影响</b>，手持槽照旧收得下任何东西（本刀只改落点顺序，不加准入限制）。</p>
 *
 * <p>用 {@link EmptyBackpackContainer}（不带背包的女仆）是有意的：它的储物区就是默认背包
 * 那几格，是所有背包类型共有的最小情形。{@code addBackpackInv} 是抽象方法，
 * 各背包容器共用 {@code MaidMainContainer} 的同一个 {@code quickMoveStack}，
 * 只负责往后追加自己的槽——所以这一处判据覆盖全部背包类型。</p>
 */
public class MaidQuickMoveGameTest {
    private static final BlockPos MAID_POS = new BlockPos(1, 2, 1);
    /** {@code addPlayerInv} 先铺 3×9 主背包（玩家背包索引 9 起），所以容器槽 0 == 玩家背包索引 9。 */
    private static final int PLAYER_INV_INDEX_OF_MENU_SLOT_0 = 9;
    /** 储物区容不下时用来占位的六件不可堆叠物，避免合并掩盖落点。 */
    private static final Item[] STORAGE_FILLER = {
            Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD,
            Items.GOLDEN_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD
    };

    private EntityMaid spawnMaid(GameTestHelper helper) {
        return helper.spawn(InitEntities.MAID, MAID_POS);
    }

    private EmptyBackpackContainer openMenu(GameTestHelper helper, EntityMaid maid, ServerPlayer player) {
        return new EmptyBackpackContainer(1, player.getInventory(), maid.getId());
    }

    /** 数储物区里某个物品有几个——储物区 = 女仆自己的 maidInv。 */
    private int countInStorage(EntityMaid maid, Item item) {
        int count = 0;
        NonNullList<ItemStack> stacks = maid.getMaidInv().copyToList();
        for (ItemStack stack : stacks) {
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * ① 主症状：玩家侧快捷移动的东西必须先落储物区，不许进主手/副手。
     *
     * <p>成因不是「手持槽优先级高」，而是手持槽几乎什么都收，而它按注册顺序排在储物区之前。</p>
     */
    @GameTest
    public void quickMoveFromPlayerLandsInStorageNotHands(GameTestHelper helper) {
        EntityMaid maid = spawnMaid(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        EmptyBackpackContainer menu = openMenu(helper, maid, player);

        player.getInventory().setItem(PLAYER_INV_INDEX_OF_MENU_SLOT_0, new ItemStack(Items.DIRT, 3));
        // 「先写入、后读出」的 fixture 必须先断言写进去了，否则下面的判定会因为空输入而假绿
        if (!menu.getSlot(0).getItem().is(Items.DIRT)) {
            helper.fail("fixture 没建起来：容器槽 0 里不是泥土，而是 " + menu.getSlot(0).getItem());
            return;
        }

        menu.quickMoveStack(player, 0);

        if (!maid.getMainHandItem().isEmpty() || !maid.getOffhandItem().isEmpty()) {
            helper.fail("快捷移动把东西塞进了手持槽：主手 " + maid.getMainHandItem()
                    + "，副手 " + maid.getOffhandItem());
            return;
        }
        if (countInStorage(maid, Items.DIRT) != 3) {
            helper.fail("储物区里的泥土不是 3 个，而是 " + countInStorage(maid, Items.DIRT));
            return;
        }
        helper.succeed();
    }

    /**
     * ② 回落：储物区满了仍要装得进手持槽——本刀只改顺序，不能把「装不下」变成「搬不动」。
     *
     * <p>装备槽在这条路上会把泥土拒掉（它自己覆写了 {@code mayPlace}，要求
     * {@code getEquipmentSlotForItem} 恰好等于该槽），所以落点必然是主手。</p>
     */
    @GameTest
    public void quickMoveFallsBackToHandsWhenStorageIsFull(GameTestHelper helper) {
        EntityMaid maid = spawnMaid(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        EmptyBackpackContainer menu = openMenu(helper, maid, player);

        // 默认背包只暴露 maidInv 的 0..5 六个索引，塞满它们就等于储物区无空位
        ItemStacksResourceHandler storage = maid.getMaidInv();
        for (int i = 0; i < STORAGE_FILLER.length; i++) {
            storage.set(i, ItemVariant.of(new ItemStack(STORAGE_FILLER[i])), 1);
        }
        for (int i = 0; i < STORAGE_FILLER.length; i++) {
            if (!storage.copyToList().get(i).is(STORAGE_FILLER[i])) {
                helper.fail("fixture 没建起来：储物区第 " + i + " 格没被填满，而是 "
                        + storage.copyToList().get(i));
                return;
            }
        }

        player.getInventory().setItem(PLAYER_INV_INDEX_OF_MENU_SLOT_0, new ItemStack(Items.DIRT, 3));
        menu.quickMoveStack(player, 0);

        if (!maid.getMainHandItem().is(Items.DIRT)) {
            helper.fail("储物区满时没有回落到手持槽，主手是 " + maid.getMainHandItem()
                    + "，玩家槽里还剩 " + player.getInventory().getItem(PLAYER_INV_INDEX_OF_MENU_SLOT_0));
            return;
        }
        helper.succeed();
    }

    /**
     * ③ 手动拖放不受影响：手持槽照旧收得下任何东西。
     *
     * <p>用户对本条的裁决是<b>不加准入限制</b>——手动放什么是玩家的自由，
     * 修法只落在快捷移动的落点顺序上。这条用例就是钉住「没有顺手加 mayPlace」。</p>
     */
    @GameTest
    public void handSlotStillAcceptsManualPlacement(GameTestHelper helper) {
        EntityMaid maid = spawnMaid(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        EmptyBackpackContainer menu = openMenu(helper, maid, player);

        // 不按下标认槽：放一件标记物进主手，再回容器里找哪个槽照出了它
        maid.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_HOE));
        if (!maid.getMainHandItem().is(Items.NETHERITE_HOE)) {
            helper.fail("fixture 没建起来：标记物没进主手，主手是 " + maid.getMainHandItem());
            return;
        }

        Slot mainHandSlot = null;
        int found = 0;
        for (Slot slot : menu.slots) {
            if (slot.getItem().is(Items.NETHERITE_HOE)) {
                mainHandSlot = slot;
                found++;
            }
        }
        // 活性下限：识别依据一变就静默零覆盖，而零覆盖的测试永远是绿的
        if (found != 1) {
            helper.fail("容器里照出主手的槽不是恰好一个，而是 " + found + " 个");
            return;
        }
        if (!mainHandSlot.mayPlace(new ItemStack(Items.DIRT))) {
            helper.fail("手持槽拒收泥土——本刀不该给它加任何准入限制");
            return;
        }
        helper.succeed();
    }
}
