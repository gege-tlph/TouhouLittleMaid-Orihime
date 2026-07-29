package com.github.tartaricacid.touhoulittlemaid.entity.passive;

import com.github.tartaricacid.touhoulittlemaid.entity.backpack.data.FurnaceBackpackData;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityExtinguishingAgent;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * 上游未关闭缺陷的回归门禁（审计见 2026-07-29 的上游只读审计报告）。
 *
 * <p>本类只覆盖**能在无头 GameTest 里确定性复现**的那几条；视觉类（Z-fighting、Gecko 缩放）
 * 与需要真人交互的条目不在此列，它们必须由实机验收关闭。
 *
 * <p>放在 {@code entity.passive} 包内是有意为之：{@link EntityMaid#completeUsingItem()}
 * 是 protected，同包才能直接驱动进食收尾这一步，从而不依赖 AI 自然行为、避免用例变成概率性通过。
 */
public class MaidUpstreamBugFixGameTest {
    private static final BlockPos MAID_POS = new BlockPos(1, 2, 1);

    // ---------------------------------------------------------------- #1053

    /**
     * 炉子背包的稀疏槽位必须原样存读。
     *
     * <p>原实现经 {@code storeAsItemList}/{@code fromItemList} 往返，只写非空项且不带索引，
     * 读取时用 {@code addItem} 压紧到最前面的可用槽：输入空、燃料与产物非空时，
     * 重进世界后燃料落进输入槽会被当成原料烧掉。
     */
    @GameTest(maxTicks = 100)
    public void sparseFurnaceBackpackKeepsItsSlots(GameTestHelper helper) {
        EntityMaid maid = prepareMaid(helper);

        FurnaceBackpackData saved = new FurnaceBackpackData(maid);
        // 稀疏状态：输入槽 0 为空，燃料槽 1 与产物槽 2 非空
        saved.setItem(1, new ItemStack(Items.COAL, 5));
        saved.setItem(2, new ItemStack(Items.IRON_INGOT, 3));

        CompoundTag tag = new CompoundTag();
        saved.save(tag, maid);

        FurnaceBackpackData loaded = new FurnaceBackpackData(maid);
        loaded.load(tag, maid);

        assertTrue(helper, loaded.getItem(0).isEmpty(),
                "the empty input slot was filled after a save/load round trip; slots got compacted");
        assertTrue(helper, loaded.getItem(1).is(Items.COAL) && loaded.getItem(1).getCount() == 5,
                "the fuel slot did not survive a save/load round trip: " + loaded.getItem(1));
        assertTrue(helper, loaded.getItem(2).is(Items.IRON_INGOT) && loaded.getItem(2).getCount() == 3,
                "the output slot did not survive a save/load round trip: " + loaded.getItem(2));

        helper.succeed();
    }

    /**
     * 旧存档（只有 {@code Items}、没有槽位索引）必须仍能读入。
     *
     * <p>修法是纯增量的，所以这条钉住向后兼容：删掉索引键后行为回落到旧语义，不得抛异常或丢物品。
     */
    @GameTest(maxTicks = 100)
    public void legacyFurnaceBackpackWithoutSlotIndexStillLoads(GameTestHelper helper) {
        EntityMaid maid = prepareMaid(helper);

        FurnaceBackpackData saved = new FurnaceBackpackData(maid);
        saved.setItem(1, new ItemStack(Items.COAL, 2));

        CompoundTag tag = new CompoundTag();
        saved.save(tag, maid);
        tag.remove("ItemSlots");

        FurnaceBackpackData loaded = new FurnaceBackpackData(maid);
        loaded.load(tag, maid);

        // 旧格式下煤炭会被压紧到槽 0，这是历史行为；这里只要求「不炸、物品还在」。
        int coal = 0;
        for (int slot = 0; slot < loaded.getContainerSize(); slot++) {
            if (loaded.getItem(slot).is(Items.COAL)) {
                coal += loaded.getItem(slot).getCount();
            }
        }
        assertTrue(helper, coal == 2,
                "a legacy furnace backpack lost its fuel on load; expected 2 coal, found " + coal);

        helper.succeed();
    }

    // ---------------------------------------------------------------- #1177

    /**
     * 主手进食不得动副手。
     *
     * <p>四个换餐任务都用 {@code HandUtils.NATIVE_HANDS} 取「第一只空手」，主手为空时选中的是主手；
     * 而进食收尾原先硬编码操作副手，于是副手那件与进食无关的物品会被塞进背包（满则掉地上），
     * 随后副手被清空——玩家侧就是物品凭空消失。
     */
    @GameTest(maxTicks = 100)
    public void eatingFromTheMainHandLeavesTheOffHandAlone(GameTestHelper helper) {
        EntityMaid maid = prepareMaid(helper);

        // 复刻任务的选手过程：主手空 → eanHand = MAIN_HAND，被换下的原手持物为空
        maid.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.DIAMOND, 4));
        maid.memoryHandItemStack(ItemStack.EMPTY);
        maid.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COOKED_BEEF));

        maid.startUsingItem(InteractionHand.MAIN_HAND);
        maid.completeUsingItem();

        ItemStack offhand = maid.getItemInHand(InteractionHand.OFF_HAND);
        assertTrue(helper, offhand.is(Items.DIAMOND) && offhand.getCount() == 4,
                "the maid swept an untouched off-hand item away while eating from her main hand: " + offhand);

        helper.succeed();
    }

    /**
     * 双手都占用时的换餐语义不得回归。
     *
     * <p>这是修改前**本来就正确**的那条路径（两只手都非空 → eanHand 保持副手），
     * 用它钉住「改用 getUsedItemHand() 之后原路径行为不变」。
     */
    @GameTest(maxTicks = 100)
    public void swappedOffHandItemStillComesBack(GameTestHelper helper) {
        EntityMaid maid = prepareMaid(helper);

        maid.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
        // 双手皆满：任务取副手，副手原有物进隐藏槽
        maid.memoryHandItemStack(new ItemStack(Items.DIAMOND, 4));
        maid.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.COOKED_BEEF));

        maid.startUsingItem(InteractionHand.OFF_HAND);
        maid.completeUsingItem();

        ItemStack offhand = maid.getItemInHand(InteractionHand.OFF_HAND);
        assertTrue(helper, offhand.is(Items.DIAMOND) && offhand.getCount() == 4,
                "the memorised off-hand item did not come back after eating: " + offhand);
        assertTrue(helper, maid.getItemInHand(InteractionHand.MAIN_HAND).is(Items.IRON_PICKAXE),
                "the main hand tool was disturbed by an off-hand meal");

        helper.succeed();
    }

    // ----------------------------------------------------------------- #938

    /**
     * 女仆不得在半空截走玩家的忠诚三叉戟。
     *
     * <p>{@code isNoPhysics()} 在 1.21.11 等价于「忠诚三叉戟正在飞回主人」，
     * 原版对该状态的拾取一律限定所有者，女仆原先照单全收。
     */
    @GameTest(maxTicks = 100)
    public void maidDoesNotStealAReturningLoyaltyTrident(GameTestHelper helper) {
        EntityMaid maid = prepareMaid(helper);
        ThrownTrident trident = returningTridentOwnedBy(helper, helper.makeMockServerPlayerInLevel(), maid);

        assertTrue(helper, !maid.pickupArrow(trident, true),
                "the maid claimed a loyalty trident that was returning to its player owner");

        helper.succeed();
    }

    /** 反向对照：女仆自己投出的三叉戟仍要能回收，否则修复就变成了新缺陷。 */
    @GameTest(maxTicks = 100)
    public void maidStillRecoversHerOwnReturningTrident(GameTestHelper helper) {
        EntityMaid maid = prepareMaid(helper);
        ThrownTrident trident = returningTridentOwnedBy(helper, maid, maid);

        assertTrue(helper, maid.pickupArrow(trident, true),
                "the maid refused to recover the trident she threw herself");

        helper.succeed();
    }

    // ---------------------------------------------------------------- #1158

    /** 灭火剂必须同时清除普通火与灵魂火。 */
    @GameTest(maxTicks = 100)
    public void extinguishingAgentClearsSoulFire(GameTestHelper helper) {
        BlockPos soulFire = new BlockPos(1, 2, 1);
        helper.setBlock(soulFire.below(), Blocks.SOUL_SOIL);
        helper.setBlock(soulFire, Blocks.SOUL_FIRE);
        helper.assertBlockPresent(Blocks.SOUL_FIRE, soulFire);

        ServerLevel level = helper.getLevel();
        BlockPos absolute = helper.absolutePos(soulFire);
        level.addFreshEntity(new EntityExtinguishingAgent(level,
                new Vec3(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5)));

        // 灭火发生在 tickCount == 5，留出余量再断言
        helper.runAfterDelay(12, () -> {
            helper.assertBlockNotPresent(Blocks.SOUL_FIRE, soulFire);
            helper.succeed();
        });
    }

    // --------------------------------------------------------------- 夹具

    private static ThrownTrident returningTridentOwnedBy(GameTestHelper helper,
                                                        net.minecraft.world.entity.LivingEntity owner,
                                                        EntityMaid nearMaid) {
        ServerLevel level = helper.getLevel();
        ThrownTrident trident = new ThrownTrident(level, owner, new ItemStack(Items.TRIDENT));
        trident.setPos(nearMaid.getX(), nearMaid.getY(), nearMaid.getZ());
        // 返程状态：ThrownTrident.tick 对 loyalty>0 的三叉戟 setNoPhysics(true)
        trident.setNoPhysics(true);
        trident.pickup = AbstractArrow.Pickup.ALLOWED;
        level.addFreshEntity(trident);
        return trident;
    }

    private static EntityMaid prepareMaid(GameTestHelper helper) {
        EntityMaid maid = helper.spawn(InitEntities.MAID, MAID_POS);
        maid.tame(helper.makeMockServerPlayerInLevel());
        return maid;
    }

    private static void assertTrue(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }
}
