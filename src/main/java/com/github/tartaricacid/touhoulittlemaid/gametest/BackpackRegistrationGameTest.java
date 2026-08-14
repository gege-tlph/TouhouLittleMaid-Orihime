package com.github.tartaricacid.touhoulittlemaid.gametest;

import com.github.tartaricacid.touhoulittlemaid.entity.backpack.BackpackManager;
import com.github.tartaricacid.touhoulittlemaid.entity.backpack.EnderChestBackpack;
import com.github.tartaricacid.touhoulittlemaid.init.InitContainer;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * 女仆背包的注册面。
 *
 * <p>背包这条链有**四个互相独立的登记点**，缺任何一个都不会报错，只会以不同方式失效：
 * 物品没注册 → 拿不到；{@code BackpackManager} 没登记 → 装上去等于没装；
 * 容器类型没注册 → 打开就断线；屏幕没登记 → 客户端崩。
 * 这条把前三个（服务端可查的）钉住，屏幕那个由 {@code InitContainerGui} 的编译期引用保证。</p>
 *
 * <p>宿主在迁移 26.1 时把工作台/末影箱/熔炉/液体四种背包整套删了，本组用例随补回的进度扩充。</p>
 */
public class BackpackRegistrationGameTest {
    @GameTest
    public void enderChestBackpackIsFullyRegistered(GameTestHelper helper) {
        if (!BuiltInRegistries.ITEM.containsKey(
                net.minecraft.resources.ResourceKey.create(BuiltInRegistries.ITEM.key(),
                        net.minecraft.resources.Identifier.fromNamespaceAndPath(
                                "touhou_little_maid", "ender_chest_backpack")))) {
            helper.fail("ender_chest_backpack 物品没注册");
            return;
        }
        if (BackpackManager.findBackpack(EnderChestBackpack.ID).isEmpty()) {
            helper.fail("末影箱背包没登记进 BackpackManager：装上去等于没装");
            return;
        }
        if (BackpackManager.findBackpack(InitItems.ENDER_CHEST_BACKPACK.getDefaultInstance()).isEmpty()) {
            helper.fail("按物品反查不到末影箱背包：BACKPACK_ITEM_MAP 没建立");
            return;
        }
        if (BuiltInRegistries.MENU.getKey(InitContainer.MAID_ENDER_CHEST_CONTAINER) == null) {
            helper.fail("末影箱背包的容器类型没注册：玩家一打开就会断线");
            return;
        }
        helper.succeed();
    }

    /** 末影箱背包不开放女仆自己的格子——它装的是玩家的末影箱，容量语义与其它背包不同。 */
    @GameTest
    public void enderChestBackpackExposesNoMaidSlots(GameTestHelper helper) {
        var backpack = BackpackManager.findBackpack(EnderChestBackpack.ID).orElse(null);
        if (backpack == null) {
            helper.fail("末影箱背包没登记");
            return;
        }
        int index = backpack.getAvailableMaxContainerIndex();
        if (index != com.github.tartaricacid.touhoulittlemaid.item.BackpackLevel.EMPTY_CAPACITY) {
            helper.fail("末影箱背包不该开放女仆格子，期望 EMPTY_CAPACITY，实为 " + index);
            return;
        }
        helper.succeed();
    }
}
