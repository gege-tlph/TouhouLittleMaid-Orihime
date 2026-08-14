package com.github.tartaricacid.touhoulittlemaid.gametest;

import com.github.tartaricacid.touhoulittlemaid.entity.backpack.BackpackManager;
import com.github.tartaricacid.touhoulittlemaid.entity.backpack.TankBackpack;
import com.github.tartaricacid.touhoulittlemaid.entity.backpack.data.TankBackpackData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitDataComponent;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;

/**
 * 液体背包的行为面：附件绑定、储罐 NBT 往返（8.0.x 的 writeValue/readValue 通道）、
 * 战利品函数往物品里写储罐。流体贴图渲染是客户端路径，GameTest 够不着，入世验证（O1）。
 */
public class TankBackpackGameTest {

    private EntityMaid spawnTankMaid(GameTestHelper helper) {
        EntityMaid maid = helper.spawn(InitEntities.MAID, new BlockPos(1, 2, 1));
        maid.setMaidBackpackType(BackpackManager.findBackpack(TankBackpack.ID).orElseThrow());
        return maid;
    }

    /** 附件链路：设完背包类型后能取到储罐数据对象。 */
    @GameTest
    public void tankBackpackDataBindsThroughAttachment(GameTestHelper helper) {
        EntityMaid maid = spawnTankMaid(helper);
        if (!(maid.getBackpackData() instanceof TankBackpackData)) {
            helper.fail("液体背包的数据对象没绑上：getBackpackData() = " + maid.getBackpackData());
            return;
        }
        helper.succeed();
    }

    /** 储罐 NBT 往返：灌 3 桶岩浆 → save → 新数据 load → 流体与量原样回来。 */
    @GameTest
    public void tankSurvivesSaveLoad(GameTestHelper helper) {
        EntityMaid maid = spawnTankMaid(helper);
        if (!(maid.getBackpackData() instanceof TankBackpackData data)) {
            helper.fail("液体背包的数据对象没绑上");
            return;
        }
        try (Transaction tx = Transaction.openOuter()) {
            data.getTank().insert(FluidVariant.of(Fluids.LAVA), 3 * FluidConstants.BUCKET, tx);
            tx.commit();
        }

        CompoundTag tag = new CompoundTag();
        data.save(tag, maid);

        TankBackpackData reloaded = new TankBackpackData(maid);
        reloaded.load(tag, maid);

        if (!reloaded.getTank().getResource().isOf(Fluids.LAVA)) {
            helper.fail("储罐流体没回来，读出的是 " + reloaded.getTank().getResource());
            return;
        }
        if (reloaded.getTank().getAmount() != 3 * FluidConstants.BUCKET) {
            helper.fail("储罐量没回来，期望 " + 3 * FluidConstants.BUCKET + "，实为 " + reloaded.getTank().getAmount());
            return;
        }
        helper.succeed();
    }

    /** 战利品函数：SetTankCountFunction 跑完，物品的数据组件里必须带上流体与量。 */
    @GameTest
    public void setTankCountFunctionWritesFluidOntoTheItem(GameTestHelper helper) {
        var lootTable = helper.getLevel().getServer().reloadableRegistries().getLootTable(
                net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.LOOT_TABLE,
                        net.minecraft.resources.Identifier.fromNamespaceAndPath("touhou_little_maid", "chest/tank_backpack")));
        if (lootTable == net.minecraft.world.level.storage.loot.LootTable.EMPTY) {
            helper.fail("chest/tank_backpack 战利品表没加载");
            return;
        }
        var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(helper.getLevel())
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.EMPTY);
        // 空表项占比 12/(1+1+1+12)，多抽几轮总会出储罐；出了就验数据组件
        for (int i = 0; i < 64; i++) {
            for (ItemStack stack : lootTable.getRandomItems(params)) {
                if (stack.isEmpty()) {
                    continue;
                }
                CompoundTag tag = stack.get(InitDataComponent.TANK_BACKPACK_TAG);
                if (tag == null || tag.getLongOr("amount", 0L) <= 0) {
                    helper.fail("战利品出的储罐背包没带流体数据：" + tag);
                    return;
                }
                helper.succeed();
                return;
            }
        }
        helper.fail("64 轮抽取没出过一次储罐背包，权重表可能不对");
    }
}
