package com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task;

import cn.sh1rocu.touhoulittlemaid.util.transfer.ItemUtil;
import com.github.tartaricacid.touhoulittlemaid.api.entity.targeting.MaidTargetingContext;
import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskAttack;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskBowAttack;
import com.github.tartaricacid.touhoulittlemaid.init.InitBrains;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

public class MaidEmergencyCombatGameTest {
    @GameTest(maxTicks = 100)
    public void emergencyOverridesScheduledActivitiesWithoutChangingPermanentTask(GameTestHelper helper) {
        EntityMaid maid = preparedMaid(helper, new BlockPos(1, 2, 1));
        Zombie target = threat(helper, new BlockPos(7, 2, 1));
        IMaidTask permanentTask = maid.getTask();

        for (Activity previous : List.of(Activity.WORK, Activity.IDLE, Activity.REST,
                InitBrains.RIDE_WORK, InitBrains.RIDE_IDLE, InitBrains.RIDE_REST)) {
            maid.getBrain().setActiveActivityIfPossible(previous);
            assertTrue(helper, maid.getEmergencyCombatManager().beginEmergency(
                    target, MaidTargetingContext.SELF_DEFENSE),
                    "failed to enter emergency combat from " + previous.getName());
            assertTrue(helper, maid.getBrain().isActive(InitBrains.EMERGENCY_COMBAT),
                    "emergency activity did not override " + previous.getName());
            assertSame(helper, permanentTask, maid.getTask(),
                    "emergency combat changed the permanent task");
            maid.getEmergencyCombatManager().cancelEmergency(false);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void entryAndExitOwnExecutionMemoriesAndRecomputeCurrentSchedule(GameTestHelper helper) {
        EntityMaid maid = preparedMaid(helper, new BlockPos(1, 2, 1));
        Zombie target = threat(helper, new BlockPos(7, 2, 1));
        BlockPos oldTarget = helper.absolutePos(new BlockPos(2, 2, 7));
        maid.getBrain().setMemory(InitBrains.TARGET_POS, new BlockPosTracker(oldTarget));
        maid.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                new WalkTarget(new BlockPosTracker(oldTarget), 0.5f, 0));

        // 基准靠 setDayTime 改日时来制造「退出后该切到另一个活动」的情形，而 26.1.2 删了
        // ServerLevel.setDayTime（日程改由 EnvironmentAttribute 驱动，javap 实查）。改为让
        // **进入应战前**的活动不等于当前日程：退出必须重算到日程活动，而不是恢复进入前的快照。
        Activity expected = maid.getScheduleDetail();
        Activity divergent = List.of(Activity.WORK, Activity.IDLE, Activity.REST).stream()
                .filter(activity -> !activity.equals(expected))
                .findFirst()
                .orElseThrow();
        maid.getBrain().setActiveActivityIfPossible(divergent);

        assertTrue(helper, maid.getEmergencyCombatManager().beginEmergency(
                target, MaidTargetingContext.SELF_DEFENSE), "valid emergency target was rejected");
        assertEmpty(helper, maid, InitBrains.TARGET_POS, "entry retained old TARGET_POS");
        assertEmpty(helper, maid, MemoryModuleType.WALK_TARGET, "entry retained old WALK_TARGET");
        assertSame(helper, target, maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null),
                "entry did not install its target");

        maid.getEmergencyCombatManager().cancelEmergency(false);

        assertTrue(helper, maid.getBrain().isActive(expected),
                "exit did not recompute the current schedule: expected " + expected.getName());
        assertEmpty(helper, maid, InitBrains.EMERGENCY_COMBAT_ACTIVE, "exit retained emergency marker");
        assertEmpty(helper, maid, MemoryModuleType.ATTACK_TARGET, "exit retained ATTACK_TARGET");
        assertEmpty(helper, maid, InitBrains.TARGET_POS, "exit restored old TARGET_POS");
        assertEmpty(helper, maid, MemoryModuleType.WALK_TARGET, "exit restored old WALK_TARGET");
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void coreCompetitorsRespectOneEmergencyMutex(GameTestHelper helper) {
        EntityMaid maid = preparedMaid(helper, new BlockPos(1, 2, 1));
        Zombie target = threat(helper, new BlockPos(7, 2, 1));
        maid.setHomeModeEnable(true);
        maid.setHomeTo(maid.blockPosition(), 2);

        // Configuring Home is itself an explicit player command, so wait for
        // its short re-entry suppression window before exercising the mutex.
        helper.runAtTickTime(21, () -> {
            assertTrue(helper, maid.getEmergencyCombatManager().beginEmergency(
                    target, MaidTargetingContext.SELF_DEFENSE), "valid emergency target was rejected");

            assertTrue(helper, MaidFollowOwnerTask.hasCompetingGoal(maid),
                    "owner follow did not see the emergency mutex");
            maid.getBrain().setMemory(InitBrains.VISIBLE_PICKUP_ENTITIES, List.of(target));
            assertFalse(helper, new ExposedPickupTask().canStart(helper, maid),
                    "pickup was allowed to start during emergency combat");

            maid.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                    new WalkTarget(new BlockPosTracker(target.blockPosition()), 0.7f, 0));
            new MaidAwaitTask().start(helper.getLevel(), maid, helper.getLevel().getGameTime());
            assertTrue(helper, maid.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET),
                    "Home await erased the emergency walk target");

            maid.getBrain().setMemory(MemoryModuleType.NEAREST_HOSTILE, target);
            new MaidPanicTask().start(helper.getLevel(), maid, helper.getLevel().getGameTime());
            assertTrue(helper, maid.getBrain().isActive(InitBrains.EMERGENCY_COMBAT),
                    "PANIC stole the active emergency activity");
            MaidUpdateActivityFromSchedule.updateActivityFromSchedule(maid);
            assertTrue(helper, maid.getBrain().isActive(InitBrains.EMERGENCY_COMBAT),
                    "schedule update stole the active emergency activity");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 100)
    public void survivalItemUsePausesEmergencyAttack(GameTestHelper helper) {
        EntityMaid maid = preparedMaid(helper, new BlockPos(1, 2, 1));
        Zombie target = threat(helper, new BlockPos(2, 2, 1));
        maid.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.APPLE));
        maid.startUsingItem(InteractionHand.OFF_HAND);
        assertTrue(helper, maid.getEmergencyCombatManager().beginEmergency(
                target, MaidTargetingContext.SELF_DEFENSE), "valid emergency target was rejected");

        helper.runAtTickTime(5, () -> {
            // 前置：先证明 fixture 还立着。这条用例有两种失败方式——「物品使用状态自己丢了」
            // 与「闸门没拦住近战」，血量断言分不开它们。分开断言后，红在哪一条就是哪种失败。
            assertTrue(helper, maid.isUsingItem(),
                    "fixture failed: the survival item use was dropped before the assertion"
                            + " [TLM-QA-2 usedHand=" + maid.getUsedItemHand()
                            + " useItem=" + maid.getUseItem()
                            + " remainingTicks=" + maid.getUseItemRemainingTicks() + "]");
            assertFalse(helper, wasHurtBy(target, maid),
                    "emergency attack interrupted an active survival item use");
            assertTrue(helper, maid.getEmergencyCombatManager().isEmergencyActive(),
                    "survival action incorrectly discarded the valid threat");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 100)
    public void explicitCommandClearsEmergencyAndSuppressesImmediateReentry(GameTestHelper helper) {
        EntityMaid maid = preparedMaid(helper, new BlockPos(1, 2, 1));
        Zombie target = threat(helper, new BlockPos(7, 2, 1));
        assertTrue(helper, maid.getEmergencyCombatManager().beginEmergency(
                target, MaidTargetingContext.SELF_DEFENSE), "valid emergency target was rejected");

        // Same-value Home/follow commands are still explicit player intent.
        maid.setHomeModeEnable(maid.isHomeModeEnable());
        assertFalse(helper, maid.getEmergencyCombatManager().isEmergencyActive(),
                "explicit player command did not clear emergency combat");
        assertFalse(helper, maid.getEmergencyCombatManager().beginEmergency(
                target, MaidTargetingContext.SELF_DEFENSE),
                "old threat immediately re-entered during player-command suppression");

        helper.runAtTickTime(21, () -> {
            assertTrue(helper, maid.getEmergencyCombatManager().beginEmergency(
                    target, MaidTargetingContext.SELF_DEFENSE),
                    "fresh valid threat could not enter after suppression expired");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 100)
    public void autonomousDangerStandDoesNotSuppressImmediateThreat(GameTestHelper helper) {
        EntityMaid maid = preparedMaid(helper, new BlockPos(1, 2, 1));
        Zombie target = threat(helper, new BlockPos(7, 2, 1));
        maid.setInSittingPose(true);

        // Initial sitting is explicit player intent. Exercise the autonomous
        // transition only after that command's suppression window expires.
        helper.runAtTickTime(21, () -> {
            maid.getBrain().setMemory(MemoryModuleType.NEAREST_HOSTILE, target);
            MaidRunAwayTask.entity(MemoryModuleType.NEAREST_HOSTILE, 0.7F, true)
                    .start(helper.getLevel(), maid, helper.getLevel().getGameTime());

            assertFalse(helper, maid.isMaidInSittingPose(),
                    "danger escape did not autonomously release the sit state");
            assertTrue(helper, maid.getEmergencyCombatManager().beginEmergency(
                            target, MaidTargetingContext.SELF_DEFENSE),
                    "autonomous danger escape was misclassified as a player command");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 120)
    public void emptyHandEmergencyWalksAndAttacks(GameTestHelper helper) {
        EntityMaid maid = preparedMaid(helper, new BlockPos(1, 2, 1));
        maid.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        Zombie target = threat(helper, new BlockPos(6, 2, 1));
        double initialDistance = maid.distanceToSqr(target);
        rememberVisibleTarget(helper, maid, target);

        assertTrue(helper, maid.getEmergencyCombatManager().beginEmergency(
                target, MaidTargetingContext.SELF_DEFENSE), "empty hand rejected a valid emergency");

        helper.runAtTickTime(20, () -> {
            assertTrue(helper, maid.distanceToSqr(target) < initialDistance,
                    "empty-hand emergency did not walk toward its target");
            target.setPos(maid.getX() + 1, maid.getY(), maid.getZ());
            rememberVisibleTarget(helper, maid, target);
        });
        helper.runAtTickTime(30, () -> {
            assertTrue(helper, wasHurtBy(target, maid),
                    "empty-hand emergency did not attack an adjacent target");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 100)
    public void ordinaryItemEmergencyDoesNotSwapInventory(GameTestHelper helper) {
        EntityMaid maid = preparedMaid(helper, new BlockPos(1, 2, 1));
        ItemStack heldStick = new ItemStack(Items.STICK);
        ItemStack storedSword = new ItemStack(Items.DIAMOND_SWORD);
        maid.setItemInHand(InteractionHand.MAIN_HAND, heldStick);
        maid.getMaidInv().set(0, ItemVariant.of(storedSword), 1);
        Zombie target = threat(helper, new BlockPos(2, 2, 1));
        rememberVisibleTarget(helper, maid, target);

        assertFalse(helper, new TaskAttack().isWeapon(maid, heldStick),
                "planned melee task unexpectedly accepted an ordinary item");
        assertTrue(helper, maid.getEmergencyCombatManager().beginEmergency(
                target, MaidTargetingContext.SELF_DEFENSE), "ordinary item rejected a valid emergency");

        helper.runAtTickTime(10, () -> {
            assertTrue(helper, wasHurtBy(target, maid),
                    "ordinary held item did not perform emergency melee");
            assertSame(helper, heldStick, maid.getMainHandItem(),
                    "emergency melee replaced the held ordinary item");
            // 宿主库存是 Fabric transfer 形态，读到的是拷贝——按内容比对而非引用比对
            assertTrue(helper, ItemStack.matches(storedSword, ItemUtil.getStack(maid.getMaidInv(), 0)),
                    "emergency melee moved the stored weapon");
        });
        helper.runAtTickTime(50, helper::succeed);
    }

    @GameTest(maxTicks = 100)
    public void usableBowCanMeleeOnlyThroughEmergencyLayer(GameTestHelper helper) {
        EntityMaid maid = preparedMaid(helper, new BlockPos(1, 2, 1));
        ItemStack bow = new ItemStack(Items.BOW);
        maid.setItemInHand(InteractionHand.MAIN_HAND, bow);
        maid.getMaidInv().set(0, ItemVariant.of(new ItemStack(Items.ARROW)), 4);
        Zombie target = threat(helper, new BlockPos(2, 2, 1));
        rememberVisibleTarget(helper, maid, target);

        assertFalse(helper, new TaskAttack().isWeapon(maid, bow),
                "planned melee task unexpectedly accepted a bow");
        assertTrue(helper, new TaskBowAttack().isWeapon(maid, bow),
                "ranged task lost its own bow gate");
        assertFalse(helper, new TaskBowAttack().isWeapon(maid, new ItemStack(Items.STICK)),
                "ranged task unexpectedly accepted an ordinary item");
        assertTrue(helper, maid.getEmergencyCombatManager().beginEmergency(
                target, MaidTargetingContext.SELF_DEFENSE), "held bow rejected a valid emergency");

        helper.runAtTickTime(10, () -> {
            assertTrue(helper, wasHurtBy(target, maid),
                    "usable projectile weapon blocked emergency melee");
            assertSame(helper, bow, maid.getMainHandItem(),
                    "emergency melee replaced the held bow");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 100)
    public void toolMeleeUsesVanillaEnchantmentsAndDurability(GameTestHelper helper) {
        EntityMaid plainMaid = preparedMaid(helper, new BlockPos(1, 2, 1));
        EntityMaid enchantedMaid = preparedMaid(helper, new BlockPos(1, 2, 6));
        ItemStack plainAxe = new ItemStack(Items.IRON_AXE);
        ItemStack enchantedAxe = new ItemStack(Items.IRON_AXE);
        enchantedAxe.enchant(helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SHARPNESS), 5);
        plainMaid.setItemInHand(InteractionHand.MAIN_HAND, plainAxe);
        enchantedMaid.setItemInHand(InteractionHand.MAIN_HAND, enchantedAxe);
        Zombie plainTarget = threat(helper, new BlockPos(2, 2, 1));
        Zombie enchantedTarget = threat(helper, new BlockPos(2, 2, 6));

        assertTrue(helper, plainMaid.getEmergencyCombatManager().beginEmergency(
                plainTarget, MaidTargetingContext.SELF_DEFENSE), "plain tool rejected a valid emergency");
        assertTrue(helper, enchantedMaid.getEmergencyCombatManager().beginEmergency(
                enchantedTarget, MaidTargetingContext.SELF_DEFENSE), "enchanted tool rejected a valid emergency");
        assertTrue(helper, plainMaid.doHurtTarget(helper.getLevel(), plainTarget),
                "vanilla melee chain rejected the plain tool");
        assertTrue(helper, enchantedMaid.doHurtTarget(helper.getLevel(), enchantedTarget),
                "vanilla melee chain rejected the enchanted tool");
        assertTrue(helper, enchantedTarget.getHealth() < plainTarget.getHealth(),
                "held-item enchantment did not affect vanilla melee damage");
        assertTrue(helper, plainAxe.getDamageValue() > 0 && enchantedAxe.getDamageValue() > 0,
                "vanilla post-hit durability was not applied to held tools");
        helper.succeed();
    }

    private static EntityMaid preparedMaid(GameTestHelper helper, BlockPos pos) {
        EntityMaid maid = helper.spawn(InitEntities.MAID, pos);
        maid.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        return maid;
    }

    private static Zombie threat(GameTestHelper helper, BlockPos pos) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, pos);
        zombie.setNoAi(true);
        return zombie;
    }

    /**
     * 判据落在「谁打的」而不是「血少了」。
     *
     * <p>实测定案：GameTest 结构在地表，僵尸会被阳光点燃并每秒掉血
     * （探针读到 {@code lastDamageSource=DamageSource (onFire)}）。拿血量变化当判据，
     * 既会让「不该打」的用例偶发假红，也会让「该打」的三条用例被火烧喂成假绿。
     * {@code getLastHurtByMob} 只记生物攻击者，火不写它。</p>
     */
    private static boolean wasHurtBy(Zombie target, EntityMaid maid) {
        return target.getLastHurtByMob() == maid;
    }

    private static void rememberVisibleTarget(GameTestHelper helper, EntityMaid maid, Zombie target) {
        maid.getBrain().setMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
                new NearestVisibleLivingEntities(helper.getLevel(), maid, List.of(target)));
    }

    private static void assertEmpty(GameTestHelper helper, EntityMaid maid,
                                    MemoryModuleType<?> memory, String message) {
        if (maid.getBrain().hasMemoryValue(memory)) {
            throw helper.assertionException(message);
        }
    }

    private static void assertTrue(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(message);
        }
    }

    private static void assertFalse(GameTestHelper helper, boolean condition, String message) {
        assertTrue(helper, !condition, message);
    }

    private static void assertSame(GameTestHelper helper, Object expected, Object actual, String message) {
        if (expected != actual) {
            throw helper.assertionException("%s: expected=%s got=%s", message, expected, actual);
        }
    }

    private static void assertEquals(GameTestHelper helper, float expected, float actual, String message) {
        if (Float.compare(expected, actual) != 0) {
            throw helper.assertionException("%s: expected=%s got=%s", message, expected, actual);
        }
    }

    private static final class ExposedPickupTask extends MaidPickupEntitiesTask {
        private ExposedPickupTask() {
            super(maid -> true, 0.6f);
        }

        private boolean canStart(GameTestHelper helper, EntityMaid maid) {
            return checkExtraStartConditions(helper.getLevel(), maid);
        }
    }
}
