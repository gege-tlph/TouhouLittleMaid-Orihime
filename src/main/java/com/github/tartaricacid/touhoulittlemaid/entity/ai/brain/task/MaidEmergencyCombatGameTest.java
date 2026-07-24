package com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task;

import com.github.tartaricacid.touhoulittlemaid.api.entity.targeting.MaidTargetingContext;
import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskAttack;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskBowAttack;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
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
                InitEntities.RIDE_WORK, InitEntities.RIDE_IDLE, InitEntities.RIDE_REST)) {
            maid.getBrain().setActiveActivityIfPossible(previous);
            assertTrue(helper, maid.getCombatManager().beginEmergency(
                    target, MaidTargetingContext.SELF_DEFENSE),
                    "failed to enter emergency combat from " + previous.getName());
            assertTrue(helper, maid.getBrain().isActive(InitEntities.EMERGENCY_COMBAT),
                    "emergency activity did not override " + previous.getName());
            assertSame(helper, permanentTask, maid.getTask(),
                    "emergency combat changed the permanent task");
            maid.getCombatManager().cancelEmergency(false);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void entryAndExitOwnExecutionMemoriesAndRecomputeCurrentSchedule(GameTestHelper helper) {
        EntityMaid maid = preparedMaid(helper, new BlockPos(1, 2, 1));
        Zombie target = threat(helper, new BlockPos(7, 2, 1));
        BlockPos oldTarget = helper.absolutePos(new BlockPos(2, 2, 7));
        maid.getBrain().setMemory(InitEntities.TARGET_POS, new BlockPosTracker(oldTarget));
        maid.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                new WalkTarget(new BlockPosTracker(oldTarget), 0.5f, 0));

        assertTrue(helper, maid.getCombatManager().beginEmergency(
                target, MaidTargetingContext.SELF_DEFENSE), "valid emergency target was rejected");
        assertEmpty(helper, maid, InitEntities.TARGET_POS, "entry retained old TARGET_POS");
        assertEmpty(helper, maid, MemoryModuleType.WALK_TARGET, "entry retained old WALK_TARGET");
        assertSame(helper, target, maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null),
                "entry did not install its target");

        helper.getLevel().setDayTime(13000);
        Activity expected = maid.getScheduleDetail();
        maid.getCombatManager().cancelEmergency(false);

        assertTrue(helper, maid.getBrain().isActive(expected),
                "exit did not recompute the current schedule: expected " + expected.getName());
        assertEmpty(helper, maid, InitEntities.EMERGENCY_COMBAT_ACTIVE, "exit retained emergency marker");
        assertEmpty(helper, maid, MemoryModuleType.ATTACK_TARGET, "exit retained ATTACK_TARGET");
        assertEmpty(helper, maid, InitEntities.TARGET_POS, "exit restored old TARGET_POS");
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
            assertTrue(helper, maid.getCombatManager().beginEmergency(
                    target, MaidTargetingContext.SELF_DEFENSE), "valid emergency target was rejected");

            assertTrue(helper, MaidFollowOwnerTask.hasCompetingGoal(maid),
                    "owner follow did not see the emergency mutex");
            maid.getBrain().setMemory(InitEntities.VISIBLE_PICKUP_ENTITIES, List.of(target));
            assertFalse(helper, new ExposedPickupTask().canStart(helper, maid),
                    "pickup was allowed to start during emergency combat");

            maid.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                    new WalkTarget(new BlockPosTracker(target.blockPosition()), 0.7f, 0));
            new MaidAwaitTask().start(helper.getLevel(), maid, helper.getLevel().getGameTime());
            assertTrue(helper, maid.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET),
                    "Home await erased the emergency walk target");

            maid.getBrain().setMemory(MemoryModuleType.NEAREST_HOSTILE, target);
            new MaidPanicTask().start(helper.getLevel(), maid, helper.getLevel().getGameTime());
            assertTrue(helper, maid.getBrain().isActive(InitEntities.EMERGENCY_COMBAT),
                    "PANIC stole the active emergency activity");
            MaidUpdateActivityFromSchedule.updateActivityFromSchedule(maid);
            assertTrue(helper, maid.getBrain().isActive(InitEntities.EMERGENCY_COMBAT),
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
        float health = target.getHealth();
        assertTrue(helper, maid.getCombatManager().beginEmergency(
                target, MaidTargetingContext.SELF_DEFENSE), "valid emergency target was rejected");

        helper.runAtTickTime(5, () -> {
            assertEquals(helper, health, target.getHealth(),
                    "emergency attack interrupted an active survival item use");
            assertTrue(helper, maid.getCombatManager().isEmergencyActive(),
                    "survival action incorrectly discarded the valid threat");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 100)
    public void explicitCommandClearsEmergencyAndSuppressesImmediateReentry(GameTestHelper helper) {
        EntityMaid maid = preparedMaid(helper, new BlockPos(1, 2, 1));
        Zombie target = threat(helper, new BlockPos(7, 2, 1));
        assertTrue(helper, maid.getCombatManager().beginEmergency(
                target, MaidTargetingContext.SELF_DEFENSE), "valid emergency target was rejected");

        // Same-value Home/follow commands are still explicit player intent.
        maid.setHomeModeEnable(maid.isHomeModeEnable());
        assertFalse(helper, maid.getCombatManager().isEmergencyActive(),
                "explicit player command did not clear emergency combat");
        assertFalse(helper, maid.getCombatManager().beginEmergency(
                target, MaidTargetingContext.SELF_DEFENSE),
                "old threat immediately re-entered during player-command suppression");

        helper.runAtTickTime(21, () -> {
            assertTrue(helper, maid.getCombatManager().beginEmergency(
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
            assertTrue(helper, maid.getCombatManager().beginEmergency(
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
        float health = target.getHealth();
        double initialDistance = maid.distanceToSqr(target);
        rememberVisibleTarget(helper, maid, target);

        assertTrue(helper, maid.getCombatManager().beginEmergency(
                target, MaidTargetingContext.SELF_DEFENSE), "empty hand rejected a valid emergency");

        helper.runAtTickTime(20, () -> {
            assertTrue(helper, maid.distanceToSqr(target) < initialDistance,
                    "empty-hand emergency did not walk toward its target");
            target.setPos(maid.getX() + 1, maid.getY(), maid.getZ());
            rememberVisibleTarget(helper, maid, target);
        });
        helper.runAtTickTime(30, () -> {
            assertTrue(helper, target.getHealth() < health,
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
        maid.getMaidInv().setStackInSlot(0, storedSword);
        Zombie target = threat(helper, new BlockPos(2, 2, 1));
        float health = target.getHealth();
        rememberVisibleTarget(helper, maid, target);

        assertFalse(helper, new TaskAttack().isWeapon(maid, heldStick),
                "planned melee task unexpectedly accepted an ordinary item");
        assertTrue(helper, maid.getCombatManager().beginEmergency(
                target, MaidTargetingContext.SELF_DEFENSE), "ordinary item rejected a valid emergency");

        helper.runAtTickTime(10, () -> {
            assertTrue(helper, target.getHealth() < health,
                    "ordinary held item did not perform emergency melee");
            assertSame(helper, heldStick, maid.getMainHandItem(),
                    "emergency melee replaced the held ordinary item");
            assertSame(helper, storedSword, maid.getMaidInv().getStackInSlot(0),
                    "emergency melee moved the stored weapon");
            maid.addTag("tlm_t4_any_item_mcp_pass");
        });
        helper.runAtTickTime(50, helper::succeed);
    }

    @GameTest(maxTicks = 100)
    public void usableBowCanMeleeOnlyThroughEmergencyLayer(GameTestHelper helper) {
        EntityMaid maid = preparedMaid(helper, new BlockPos(1, 2, 1));
        ItemStack bow = new ItemStack(Items.BOW);
        maid.setItemInHand(InteractionHand.MAIN_HAND, bow);
        maid.getMaidInv().setStackInSlot(0, new ItemStack(Items.ARROW, 4));
        Zombie target = threat(helper, new BlockPos(2, 2, 1));
        float health = target.getHealth();
        rememberVisibleTarget(helper, maid, target);

        assertFalse(helper, new TaskAttack().isWeapon(maid, bow),
                "planned melee task unexpectedly accepted a bow");
        assertTrue(helper, new TaskBowAttack().isWeapon(maid, bow),
                "ranged task lost its own bow gate");
        assertFalse(helper, new TaskBowAttack().isWeapon(maid, new ItemStack(Items.STICK)),
                "ranged task unexpectedly accepted an ordinary item");
        assertTrue(helper, maid.getCombatManager().beginEmergency(
                target, MaidTargetingContext.SELF_DEFENSE), "held bow rejected a valid emergency");

        helper.runAtTickTime(10, () -> {
            assertTrue(helper, target.getHealth() < health,
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

        assertTrue(helper, plainMaid.getCombatManager().beginEmergency(
                plainTarget, MaidTargetingContext.SELF_DEFENSE), "plain tool rejected a valid emergency");
        assertTrue(helper, enchantedMaid.getCombatManager().beginEmergency(
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
            super(0.6f);
        }

        private boolean canStart(GameTestHelper helper, EntityMaid maid) {
            return checkExtraStartConditions(helper.getLevel(), maid);
        }
    }
}
