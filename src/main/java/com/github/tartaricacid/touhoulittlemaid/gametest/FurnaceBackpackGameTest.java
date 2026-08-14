package com.github.tartaricacid.touhoulittlemaid.gametest;

import com.github.tartaricacid.touhoulittlemaid.api.backpack.IBackpackData;
import com.github.tartaricacid.touhoulittlemaid.entity.backpack.BackpackManager;
import com.github.tartaricacid.touhoulittlemaid.entity.backpack.FurnaceBackpack;
import com.github.tartaricacid.touhoulittlemaid.entity.backpack.data.FurnaceBackpackData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 熔炉背包的行为面：附件惰性绑定、烧炼逻辑、稀疏槽位存取。
 *
 * <p>烧炼用例**直接驱动 {@code serverTick}** 而不是等真实 tick——确定性、不吃超时；
 * 「manager.tick 有没有接上」由绑定用例（走 {@code maid.getBackpackData()} 的附件路径）
 * 与注册用例共同看住。</p>
 */
public class FurnaceBackpackGameTest {

    private EntityMaid spawnFurnaceMaid(GameTestHelper helper) {
        EntityMaid maid = helper.spawn(InitEntities.MAID, new BlockPos(1, 2, 1));
        maid.setMaidBackpackType(BackpackManager.findBackpack(FurnaceBackpack.ID).orElseThrow());
        return maid;
    }

    /** 附件链路：设完背包类型后能取到熔炉数据对象（惰性绑定 + BackpackStateData 附件）。 */
    @GameTest
    public void furnaceBackpackDataBindsThroughAttachment(GameTestHelper helper) {
        EntityMaid maid = spawnFurnaceMaid(helper);
        IBackpackData data = maid.getBackpackData();
        if (!(data instanceof FurnaceBackpackData)) {
            helper.fail("熔炉背包的数据对象没绑上：getBackpackData() = " + data);
            return;
        }
        helper.succeed();
    }

    /** 烧炼全链路：牛肉 + 煤，驱动 210 个 serverTick，产物槽必须出现熟牛肉。 */
    @GameTest
    public void furnaceBackpackSmeltsBeef(GameTestHelper helper) {
        EntityMaid maid = spawnFurnaceMaid(helper);
        if (!(maid.getBackpackData() instanceof FurnaceBackpackData data)) {
            helper.fail("熔炉背包的数据对象没绑上");
            return;
        }
        data.setItem(0, new ItemStack(Items.BEEF));
        data.setItem(1, new ItemStack(Items.COAL));
        // 熟成时间 200 tick，多给 10 tick 余量
        for (int i = 0; i < 210; i++) {
            data.serverTick(maid);
        }
        ItemStack output = data.getItem(2);
        if (!output.is(Items.COOKED_BEEF)) {
            helper.fail("烧了 210 tick 产物槽仍不是熟牛肉，而是 " + output);
            return;
        }
        helper.succeed();
    }

    /**
     * 稀疏槽位存取往返（上游缺陷 #1053 的回归钉）：只有燃料槽有物品时，
     * 存了再读，燃料必须还在燃料槽——不带索引的旧格式会把它压紧进输入槽，
     * 重进世界后燃料会被当原料烧掉。
     */
    @GameTest
    public void furnaceBackpackSparseSlotsSurviveSaveLoad(GameTestHelper helper) {
        EntityMaid maid = spawnFurnaceMaid(helper);
        if (!(maid.getBackpackData() instanceof FurnaceBackpackData data)) {
            helper.fail("熔炉背包的数据对象没绑上");
            return;
        }
        data.setItem(1, new ItemStack(Items.COAL, 7));

        CompoundTag tag = new CompoundTag();
        data.save(tag, maid);

        FurnaceBackpackData reloaded = new FurnaceBackpackData(maid);
        reloaded.load(tag, maid);

        if (!reloaded.getItem(0).isEmpty()) {
            helper.fail("稀疏状态被压紧：输入槽读出了 " + reloaded.getItem(0));
            return;
        }
        ItemStack fuel = reloaded.getItem(1);
        if (!fuel.is(Items.COAL) || fuel.getCount() != 7) {
            helper.fail("燃料槽没有按索引还原，读出的是 " + fuel);
            return;
        }
        helper.succeed();
    }
}
